package me.coweery.vertx.service.proxy.exceptionhandler

interface EbExceptionHandler {

    fun mapTo(throwable: Throwable): EbException

    fun mapFrom(throwable: EbException): Throwable
}