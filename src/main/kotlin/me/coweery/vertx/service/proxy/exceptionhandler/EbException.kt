package me.coweery.vertx.service.proxy.exceptionhandler

class EbException(
    val code: Int,
    message: String?
) : Exception(message)