package me.coweery.vertx.service.proxy

import io.vertx.reactivex.core.Vertx
import me.coweery.vertx.service.proxy.factories.proxy.EbProxyFactoryImpl

interface EbProxy {

    companion object {

        fun create(vertx: Vertx): EbProxy {
            return EbProxyImpl(vertx, EbProxyFactoryImpl(), EventBusSubscriberImpl())
        }
    }

    fun <T> get(serviceInterface: Class<T>): T

    fun <T : Any> attach(serviceInterface: Class<T>, implementation: T)
}