package me.coweery.vertx.service.proxy.annotations.options

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class EbExceptionHandler(
    val mapper: KClass<*>
)