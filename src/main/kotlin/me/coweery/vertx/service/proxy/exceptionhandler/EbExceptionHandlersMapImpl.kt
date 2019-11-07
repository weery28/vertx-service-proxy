package me.coweery.vertx.service.proxy.exceptionhandler

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
            val `class` = it.mapper
            handlers[`class`] ?: handlers.let {
                val instance = `class`.java.newInstance() as EbExceptionHandler
                it[`class`] = instance
                instance
            }
        } ?: defaultReplyExceptionMapper
    }
}