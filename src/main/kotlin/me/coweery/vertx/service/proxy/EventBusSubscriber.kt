package me.coweery.vertx.service.proxy

import com.fasterxml.jackson.databind.type.TypeFactory
import io.reactivex.Completable
import io.reactivex.Single
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.eventbus.EventBus
import io.vertx.reactivex.core.eventbus.Message
import java.lang.reflect.Method

interface EventBusSubscriber {

    fun <T : Any> subscribe(eventBus: EventBus, serviceInterface: Class<T>, serviceImpl: T)
}

class EventBusSubscriberImpl : EventBusSubscriber {

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

            when (method.returnType) {
                Completable::class.java -> invokeWithCompletableResult(method, args, serviceImpl, message)
                Single::class.java -> invokeWithSingleResult(method, args, serviceImpl, message)
            }
        }
    }

    private fun invokeWithSingleResult(method: Method, args: JsonArray, impl: Any, message: Message<JsonObject>) {
        (method.invoke(impl, *parseArgs(method, args)) as Single<Any>).subscribe { res, throwable ->
            if (throwable != null) {
                message.fail(1, throwable.message)
            } else {
                message.reply(JsonObject().put(EB_METHOD_RESULT_KEY, JsonObject.mapFrom(res)))
            }
        }
    }

    private fun invokeWithCompletableResult(method: Method, args: JsonArray, impl: Any, message: Message<JsonObject>) {
        (method.invoke(impl, *parseArgs(method, args)) as Completable).subscribe(
            {
                message.reply(EB_COMPLETABLE_METHOD_SUCCESS)
            },
            {
                message.fail(1, it.message)
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
                Json.mapper.readValue(args.getValue(index).toString(), TypeFactory.rawClass(type))
            }
        }.toTypedArray()
    }
}