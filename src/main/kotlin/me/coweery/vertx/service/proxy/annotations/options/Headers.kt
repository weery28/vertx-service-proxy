package me.coweery.vertx.service.proxy.annotations.options

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Headers(
    val key: String,
    val value: String
)