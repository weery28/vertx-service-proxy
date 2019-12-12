package me.coweery.vertx.service.proxy.factories.serialization

import io.vertx.core.json.JsonArray
import java.lang.reflect.Type
import java.util.Date

class ReadersFactoryImpl : ReadersFactory {

    private val readers = mutableMapOf<Type, (JsonArray, Int) -> Any>()

    private val defaultReader: (Type) -> ((JsonArray, Int) -> Any) = { type ->
        { args, index ->
            when (type) {

                Double::class.java -> args.getDouble(index)
                String::class.java -> args.getString(index)
                Float::class.java -> args.getFloat(index)
                Int::class.java -> args.getInteger(index)
                Integer::class.java -> args.getInteger(index)
                Long::class.java -> args.getLong(index)
                Date::class.java -> Date.from(args.getInstant(index))
                else -> args.getJsonObject(index).mapTo(Class.forName(type.typeName))
            }
        }
    }

    override fun add(clazz: Class<*>, reader: (JsonArray, Int) -> Any) {
        readers[clazz] = reader
    }

    override fun get(type: Type): (JsonArray, Int) -> Any {

        return readers[type] ?: defaultReader(type)
    }

    override fun getCustoms(args: Array<out Any>): List<((JsonArray, Int) -> Any)?> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}