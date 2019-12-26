package me.coweery.vertx.service.proxy.factories.proxy

import com.fasterxml.jackson.databind.type.TypeFactory
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.eventbus.EventBus
import me.coweery.vertx.service.proxy.DeliveryOptionsBuilder
import me.coweery.vertx.service.proxy.EB_COMPLETABLE_METHOD_SUCCESS
import me.coweery.vertx.service.proxy.EB_METHOD_ARGUMENTS_KEY
import me.coweery.vertx.service.proxy.EB_METHOD_HEADER
import me.coweery.vertx.service.proxy.EB_METHOD_RESULT_KEY
import me.coweery.vertx.service.proxy.exceptionhandler.EbException
import me.coweery.vertx.service.proxy.exceptionhandler.EbExceptionHandler
import me.coweery.vertx.service.proxy.exceptionhandler.EbExceptionHandlersFactory
import me.coweery.vertx.service.proxy.factories.serialization.WritersFactory
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class EbProxyFactoryImpl(
    private val ebExceptionHandlersFactory: EbExceptionHandlersFactory,
    override val writersFactory: WritersFactory
) : EbProxyFactory {

    override fun <T> create(eventBus: EventBus, serviceInterface: Class<T>): T {

        return Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            arrayOf(serviceInterface),
            EbProxyInvocationHandler(eventBus, serviceInterface.name, ebExceptionHandlersFactory, writersFactory)
        ) as T
    }

    private class EbProxyInvocationHandler(
        private val eventBus: EventBus,
        private val address: String,
        private val ebExceptionHandlersFactory: EbExceptionHandlersFactory,
        private val writersFactory: WritersFactory
    ) : InvocationHandler {

        private val deliveryOptionsBuilder = DeliveryOptionsBuilder()

        override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {

            val deliveryOptions = createDeliveryOptions(method, args)
            val body = createBody(args, writersFactory)
            val exceptionHandler = ebExceptionHandlersFactory.getReplyExceptionMapper(method)

            return when (method.returnType) {
                Completable::class.java -> returnCompletable(eventBus, address, body, deliveryOptions)
                    .decodeThrowable(exceptionHandler)
                Single::class.java -> returnSingle(eventBus, address, body, deliveryOptions, method)
                    .decodeThrowable(exceptionHandler)
                Maybe::class.java -> returnMaybe(eventBus, address, body, deliveryOptions, method)
                    .decodeThrowable(exceptionHandler)
                else -> throw IllegalArgumentException()
            }
        }

        private fun createDeliveryOptions(method: Method, args: Array<out Any>): DeliveryOptions {

            return DeliveryOptions(
                (args.firstOrNull { it is DeliveryOptions } as DeliveryOptions?)
                    ?: deliveryOptionsBuilder.build(method)
            ).addHeader(EB_METHOD_HEADER, method.name)
        }

        private fun createBody(
            args: Array<out Any>,
            writersFactory: WritersFactory

        ): JsonObject {

            val argsWriters = writersFactory.getCustoms(args)

            return JsonObject().put(
                EB_METHOD_ARGUMENTS_KEY,
                args.filter { it !is DeliveryOptions }
                    .mapIndexed { i, arg ->
                        if (argsWriters[i] != null) {
                            argsWriters[i]!!(arg)
                        } else {
                            writersFactory.get(args::class.java)(arg)
                        }
                    }
            )
        }

        private fun returnSingle(
            eventBus: EventBus,
            address: String,
            body: JsonObject,
            deliveryOptions: DeliveryOptions,
            method: Method
        ): Single<Any> {

            val generic = method.genericReturnType as ParameterizedTypeImpl
            val resultClass = generic.actualTypeArguments.first()

            return eventBus
                .rxRequest<JsonObject>(address, body, deliveryOptions)
                .map {
                    if (resultClass is ParameterizedTypeImpl){
                        if (resultClass.rawType == List::class.java){
                            val childClass = resultClass.actualTypeArguments.first()
                            JsonArray(it.body().getValue(EB_METHOD_RESULT_KEY).toString()).map {
                                Json.mapper.readValue(
                                    (it as JsonObject).encode(),
                                    TypeFactory.rawClass(childClass)
                                )
                            }
                        } else {
                            Json.mapper.readValue(
                                it.body().getValue(EB_METHOD_RESULT_KEY).toString(),
                                TypeFactory.rawClass(resultClass)
                            )
                        }
                    } else {
                        Json.mapper.readValue(
                            it.body().getValue(EB_METHOD_RESULT_KEY).toString(),
                            TypeFactory.rawClass(resultClass)
                        )
                    }
                }
        }

        private fun returnMaybe(
            eventBus: EventBus,
            address: String,
            body: JsonObject,
            deliveryOptions: DeliveryOptions,
            method: Method
        ): Maybe<Any> {

            val generic = method.genericReturnType as ParameterizedTypeImpl
            val resultClass = generic.actualTypeArguments.first()

            return eventBus
                .rxRequest<JsonObject>(address, body, deliveryOptions)
                .flatMapMaybe {
                    if (it.body().isEmpty) {
                        Maybe.empty()
                    } else {
                        Maybe.just(Json.mapper.readValue(
                            it.body().getValue(EB_METHOD_RESULT_KEY).toString(),
                            TypeFactory.rawClass(resultClass)
                        ))
                    }
                }
        }

        private fun returnCompletable(
            eventBus: EventBus,
            address: String,
            body: JsonObject,
            deliveryOptions: DeliveryOptions
        ): Completable {

            return eventBus.rxRequest<String>(address, body, deliveryOptions)
                .flatMapCompletable {
                    if (it.body() == EB_COMPLETABLE_METHOD_SUCCESS) {
                        Completable.complete()
                    } else {
                        Completable.error(UnknownError())
                    }
                }
        }

        private fun <T> Single<T>.decodeThrowable(exceptionHandler: EbExceptionHandler): Single<T> {

            return this.onErrorResumeNext {
                if (it is ReplyException) {
                    with(EbException(it.failureCode(), it.message)) {
                        Single.error<T>(exceptionHandler.mapFrom(this))
                    }
                } else {
                    Single.error<T>(it)
                }
            }
        }

        private fun Completable.decodeThrowable(exceptionHandler: EbExceptionHandler): Completable {

            return this.onErrorResumeNext {
                if (it is ReplyException) {
                    with(EbException(it.failureCode(), it.message)) {
                        Completable.error(exceptionHandler.mapFrom(this))
                    }
                } else {
                    Completable.error(it)
                }
            }
        }

        private fun <T> Maybe<T>.decodeThrowable(exceptionHandler: EbExceptionHandler): Maybe<T> {

            return this.onErrorResumeNext { t: Throwable ->
                if (t is ReplyException) {
                    with(EbException(t.failureCode(), t.message)) {
                        Maybe.error<T>(exceptionHandler.mapFrom(this))
                    }
                } else {
                    Maybe.error<T>(t)
                }
            }
        }
    }
}