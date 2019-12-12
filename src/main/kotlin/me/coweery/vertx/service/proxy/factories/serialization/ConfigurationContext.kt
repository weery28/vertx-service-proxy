package me.coweery.vertx.service.proxy.factories.serialization

interface ConfigurationContext {

    fun <T : Any> registerWriterForClass(clazz: Class<T>, writer: (T) -> Any)

    fun <T : Any> registerReaderForClass(clazz: Class<T>, writer: (T) -> Any)
}