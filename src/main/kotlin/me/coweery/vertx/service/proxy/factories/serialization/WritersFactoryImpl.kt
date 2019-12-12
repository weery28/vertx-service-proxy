package me.coweery.vertx.service.proxy.factories.serialization

import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonObject
import me.coweery.vertx.service.proxy.JsonUtils

class WritersFactoryImpl : WritersFactory {

    private val defaultWriter: (Any) -> Any = {
        checkAndCopy(it)
    }

    private val writers = mutableMapOf<Class<*>, (Any) -> Any>()

    override fun add(clazz: Class<*>, writer: (Any) -> Any) {
        writers[clazz] = writer
    }

    override fun get(clazz: Class<*>): (Any) -> Any {
        return writers[clazz] ?: defaultWriter
    }

    override fun getCustoms(args: Array<out Any>): List<((Any) -> Any)?> {

        // TODO writers by annotation

        return args.map {
            if (it is DeliveryOptions) {
                null
            } else {
                writers[it::class.java]
            }
        }
    }

    private fun checkAndCopy(obj: Any): Any {

        return try {
            JsonUtils.checkAndCopy(obj, true)
        } catch (e: IllegalStateException) {
            JsonObject.mapFrom(obj).copy()
        }
    }
}