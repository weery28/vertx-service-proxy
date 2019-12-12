package me.coweery.vertx.service.proxy.factories.serialization

class ConfigurationContextImpl(
    private val writersFactory: WritersFactory
) : ConfigurationContext {

    override fun <T : Any> registerWriterForClass(clazz: Class<T>, writer: (T) -> Any) {
        writersFactory.add(clazz, writer as ((Any) -> (Any)))
    }

    override fun <T : Any> registerReaderForClass(clazz: Class<T>, writer: (T) -> Any) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}