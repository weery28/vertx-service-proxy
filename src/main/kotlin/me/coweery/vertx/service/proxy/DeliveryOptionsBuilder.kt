package me.coweery.vertx.service.proxy

import io.vertx.core.eventbus.DeliveryOptions
import me.coweery.vertx.service.proxy.annotations.options.CodecName
import me.coweery.vertx.service.proxy.annotations.options.Headers
import me.coweery.vertx.service.proxy.annotations.options.LocalOnly
import me.coweery.vertx.service.proxy.annotations.options.SendTimeout
import java.lang.reflect.Method

class DeliveryOptionsBuilder {

    fun build(method: Method): DeliveryOptions {

        val deliveryOptions = DeliveryOptions()

        method.getAnnotation(SendTimeout::class.java)?.let {
            deliveryOptions.sendTimeout = it.timeout
        }

        method.getAnnotation(LocalOnly::class.java)?.let {
            deliveryOptions.isLocalOnly = true
        }

        method.getAnnotation(CodecName::class.java)?.let {
            deliveryOptions.codecName = it.name
        }

        method.getAnnotationsByType(Headers::class.java).forEach {
            deliveryOptions.addHeader(it.key, it.value)
        }

        return deliveryOptions
    }
}