package me.coweery.vertx.service.proxy.factories.proxy

import io.vertx.reactivex.core.eventbus.EventBus

interface EbProxyFactory {

    fun <T> create(eventBus: EventBus, serviceInterface: Class<T>): T
}