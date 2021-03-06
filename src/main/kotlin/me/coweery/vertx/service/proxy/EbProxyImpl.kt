package me.coweery.vertx.service.proxy

import io.vertx.reactivex.core.Vertx
import me.coweery.vertx.service.proxy.factories.proxy.EbProxyFactory
import me.coweery.vertx.service.proxy.factories.serialization.ConfigurationContext
import me.coweery.vertx.service.proxy.factories.serialization.ConfigurationContextImpl

class EbProxyImpl(
    private val vertx: Vertx,
    private val proxyFactory: EbProxyFactory,
    private val eventBusSubscriber: EventBusSubscriber
) : EbProxy {

    override fun <T> get(serviceInterface: Class<T>): T {
        return proxyFactory.create(vertx.eventBus(), serviceInterface)
    }

    override fun <T : Any> attach(serviceInterface: Class<T>, implementation: T) {
        eventBusSubscriber.subscribe(vertx.eventBus(), serviceInterface, implementation)
    }

    override fun configurate(configuration: ConfigurationContext.() -> Unit): EbProxy {
        ConfigurationContextImpl(proxyFactory.writersFactory).apply(configuration)
        return this
    }
}