package me.coweery.vertx.service.proxy.exceptionhandler

import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import java.lang.reflect.Method
import kotlin.reflect.KClass

class EbExceptionHandlersMapImpl : EbExceptionHandlersMap {

    private val handlers: MutableMap<KClass<*>, EbExceptionHandler> = mutableMapOf()

    private val defaultReplyExceptionMapper = object : EbExceptionHandler {

        override fun mapTo(throwable: Throwable): EbException {
            return EbException(1, throwable.message)
        }

        override fun mapFrom(throwable: EbException): Throwable {
            return throwable
        }
    }

    override fun getReplyExceptionMapper(method: Method): EbExceptionHandler {

        return method.getAnnotation(me.coweery.vertx.service.proxy.annotations.options.EbExceptionHandler::class.java)?.let {
            val key = it.mapper
            handlers[key] ?: handlers.let {
                val instance = key.java.newInstance() as? EbExceptionHandler ?: throw IllegalArgumentException()
                it[key] = instance
                instance
            }
        } ?: defaultReplyExceptionMapper
    }
}