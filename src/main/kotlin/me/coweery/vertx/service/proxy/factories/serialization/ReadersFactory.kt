package me.coweery.vertx.service.proxy.factories.serialization

import io.vertx.core.json.JsonArray
import java.lang.reflect.Type

interface ReadersFactory {

    fun add(clazz: Class<*>, reader: (JsonArray, Int) -> Any)

    fun get(type: Type): (JsonArray, Int) -> Any

    fun getCustoms(args: Array<out Any>): List<((JsonArray, Int) -> Any)?>
}