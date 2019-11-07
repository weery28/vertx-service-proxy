package me.coweery.vertx.service.proxy.exceptionhandler

import java.lang.reflect.Method

interface EbExceptionHandlersMap {

    fun getReplyExceptionMapper(method: Method): EbExceptionHandler
}