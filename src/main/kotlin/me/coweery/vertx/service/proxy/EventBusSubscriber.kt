package me.coweery.vertx.service.proxy

import com.fasterxml.jackson.databind.type.TypeFactory
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.eventbus.EventBus
import io.vertx.reactivex.core.eventbus.Message
import me.coweery.vertx.service.proxy.exceptionhandler.EbExceptionHandler
import me.coweery.vertx.service.proxy.exceptionhandler.EbExceptionHandlersMap
import jdk.nashorn.internal.codegen.types.Type
import java.lang.reflect.Method
import java.time.Instant

interface EventBusSubscriber {

    fun <T : Any> subscribe(eventBus: EventBus, serviceInterface: Class<T>, serviceImpl: T)
}

class EventBusSubscriberImpl(
    private val ebExceptionHandlersMap: EbExceptionHandlersMap
) : EventBusSubscriber {

    override fun <T : Any> subscribe(eventBus: EventBus, serviceInterface: Class<T>, serviceImpl: T) {

        eventBus.consumer(serviceInterface.name, handleMessage(serviceInterface, serviceImpl))
    }

    private fun <T : Any> handleMessage(serviceInterface: Class<T>, serviceImpl: T): (Message<JsonObject>) -> Unit {

        return { message ->
            val args = message.body().getJsonArray(EB_METHOD_ARGUMENTS_KEY)
            val methodName = message.headers()[EB_METHOD_HEADER]!!

            val method = serviceInterface.methods.first {
                it.name == methodName &&
                        it.parameters.filter { it.type != DeliveryOptions::class.java }.size == args.size()
            }

            val exceptionHandler = ebExceptionHandlersMap.getReplyExceptionMapper(method)

            when (method.returnType) {
                Completable::class.java -> invokeWithCompletableResult(method, args, serviceImpl, message, exceptionHandler)
                Single::class.java -> invokeWithSingleResult(method, args, serviceImpl, message, exceptionHandler)
                Maybe::class.java -> invokeWithMaybeResult(method, args, serviceImpl, message, exceptionHandler)
            }
        }
    }

    private fun invokeWithSingleResult(
        method: Method,
        args: JsonArray,
        impl: Any,
        message: Message<JsonObject>,
        exceptionHandler: EbExceptionHandler
    ) {
        (method.invoke(impl, *parseArgs(method, args)) as Single<Any>).subscribe { res, throwable ->
            if (throwable != null) {
                handleException(message, exceptionHandler, throwable)
            } else {
                message.reply(JsonObject().put(EB_METHOD_RESULT_KEY, JsonObject.mapFrom(res)))
            }
        }
    }

    private fun invokeWithCompletableResult(
        method: Method,
        args: JsonArray,
        impl: Any,
        message: Message<JsonObject>,
        exceptionHandler: EbExceptionHandler
    ) {
        (method.invoke(impl, *parseArgs(method, args)) as Completable).subscribe(
            {
                message.reply(EB_COMPLETABLE_METHOD_SUCCESS)
            },
            {
                handleException(message, exceptionHandler, it)
            }
        )
    }

    private fun invokeWithMaybeResult(
        method: Method,
        args: JsonArray,
        impl: Any, message: Message<JsonObject>,
        exceptionHandler: EbExceptionHandler
    ) {
        (method.invoke(impl, *parseArgs(method, args)) as Maybe<Any>).subscribe(
            {
                message.reply(JsonObject().put(EB_METHOD_RESULT_KEY, JsonObject.mapFrom(it)))
            },
            {
                handleException(message, exceptionHandler, it)
            },{
                message.reply(JsonObject())
            }
        )
    }

    private fun parseArgs(method: Method, args: JsonArray): Array<Any> {

        var index = -1
        return method.parameters.map { parameter ->
            if (parameter.type == DeliveryOptions::class.java) {
                DeliveryOptions()
            } else {
                index += 1
                val type = parameter.parameterizedType
                when (type) {
                   String::class.java -> args.getString(index)
                    Long::class.java -> args.getLong(index)
                    Int::class.java -> args.getInteger(index)
                    Instant::class.java -> args.getInstant(index)
                    else -> Json.mapper.readValue(args.getValue(index).toString(), TypeFactory.rawClass(type))

                }
            }
        }.toTypedArray()
    }

    private fun handleException(message: Message<JsonObject>, handler: EbExceptionHandler, throwable: Throwable) {

        val ebException = handler.mapTo(throwable)
        message.fail(ebException.code, ebException.message)
    }
}