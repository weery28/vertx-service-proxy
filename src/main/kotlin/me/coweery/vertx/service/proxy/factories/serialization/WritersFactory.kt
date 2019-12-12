package me.coweery.vertx.service.proxy.factories.serialization

interface WritersFactory {

    fun add(clazz: Class<*>, writer: (Any) -> Any)

    fun get(clazz: Class<*>): (Any) -> Any

    fun getCustoms(args: Array<out Any>): List<((Any) -> Any)?>
}