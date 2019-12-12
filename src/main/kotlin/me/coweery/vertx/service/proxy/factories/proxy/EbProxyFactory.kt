package me.coweery.vertx.service.proxy.factories.proxy

import io.vertx.reactivex.core.eventbus.EventBus
import me.coweery.vertx.service.proxy.factories.serialization.WritersFactory

interface EbProxyFactory {

    fun <T> create(eventBus: EventBus, serviceInterface: Class<T>): T

    val writersFactory: WritersFactory
}