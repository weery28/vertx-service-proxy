package me.coweery.vertx.service.proxy

import io.vertx.reactivex.core.Vertx
import me.coweery.vertx.service.proxy.factories.proxy.EbProxyFactoryImpl
import me.coweery.vertx.service.proxy.exceptionhandler.EbExceptionHandlersMapImpl

interface EbProxy {

    companion object {

        fun create(vertx: Vertx): EbProxy {
            val ebExceptionHandlersMap = EbExceptionHandlersMapImpl()
            return EbProxyImpl(vertx, EbProxyFactoryImpl(ebExceptionHandlersMap), EventBusSubscriberImpl(ebExceptionHandlersMap))
        }
    }

    fun <T> get(serviceInterface: Class<T>): T

    fun <T : Any> attach(serviceInterface: Class<T>, implementation: T)
}