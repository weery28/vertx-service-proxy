package me.coweery.vertx.service.proxy.factories.proxy

import com.fasterxml.jackson.databind.type.TypeFactory
import io.reactivex.Completable
import io.reactivex.Single
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.eventbus.EventBus
import me.coweery.vertx.service.proxy.DeliveryOptionsBuilder
import me.coweery.vertx.service.proxy.EB_COMPLETABLE_METHOD_SUCCESS
import me.coweery.vertx.service.proxy.EB_METHOD_ARGUMENTS_KEY
import me.coweery.vertx.service.proxy.EB_METHOD_HEADER
import me.coweery.vertx.service.proxy.EB_METHOD_RESULT_KEY
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class EbProxyFactoryImpl : EbProxyFactory {

    override fun <T> create(eventBus: EventBus, serviceInterface: Class<T>): T {

        return Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            arrayOf(serviceInterface),
            EbProxyInvocationHandler(eventBus, serviceInterface.name)
        ) as T
    }

    private class EbProxyInvocationHandler(
        private val eventBus: EventBus,
        private val address: String
    ) : InvocationHandler {

        private val deliveryOptionsBuilder = DeliveryOptionsBuilder()

        override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {

            val deliveryOptions = createDeliveryOptions(method, args)
            val body = createBody(args)

            return when (method.returnType) {
                Completable::class.java -> returnCompletable(eventBus, address, body, deliveryOptions)
                Single::class.java -> returnSingle(eventBus, address, body, deliveryOptions, method)
                else -> throw IllegalArgumentException()
            }
        }

        private fun createDeliveryOptions(method: Method, args: Array<out Any>): DeliveryOptions {

            return DeliveryOptions(
                (args.firstOrNull { it is DeliveryOptions } as DeliveryOptions?)
                    ?: deliveryOptionsBuilder.build(method)
            ).addHeader(EB_METHOD_HEADER, method.name)
        }

        private fun createBody(args: Array<out Any>): JsonObject {

            return JsonObject().put(EB_METHOD_ARGUMENTS_KEY, args.filter { it !is DeliveryOptions })
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
                    Json.mapper.readValue(
                        it.body().getValue(EB_METHOD_RESULT_KEY).toString(),
                        TypeFactory.rawClass(resultClass)
                    )
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
    }
}