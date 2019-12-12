package me.coweery.vertx.service.proxy

import io.vertx.reactivex.core.Vertx
import me.coweery.vertx.service.proxy.exceptionhandler.EbExceptionHandlersFactoryImpl
import me.coweery.vertx.service.proxy.factories.proxy.EbProxyFactoryImpl
import me.coweery.vertx.service.proxy.factories.serialization.ConfigurationContext
import me.coweery.vertx.service.proxy.factories.serialization.ReadersFactoryImpl
import me.coweery.vertx.service.proxy.factories.serialization.WritersFactoryImpl

interface EbProxy {

    companion object {

        fun create(vertx: Vertx): EbProxy {
            val ebExceptionHandlersMap = EbExceptionHandlersFactoryImpl()
            val writersFactory = WritersFactoryImpl()
            val readersFactory = ReadersFactoryImpl()
            return EbProxyImpl(
                vertx,
                EbProxyFactoryImpl(ebExceptionHandlersMap, writersFactory),
                EventBusSubscriberImpl(ebExceptionHandlersMap, readersFactory)
            )
        }
    }

    fun <T> get(serviceInterface: Class<T>): T

    fun <T : Any> attach(serviceInterface: Class<T>, implementation: T)

    fun configurate(configuration: ConfigurationContext.() -> Unit): EbProxy
}