package me.coweery.vertx.service.proxy.exceptionhandler

import java.lang.reflect.Method

interface EbExceptionHandlersFactory {

    fun getReplyExceptionMapper(method: Method): EbExceptionHandler
}