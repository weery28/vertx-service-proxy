package me.coweery.vertx.service.proxy.annotations.options

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SendTimeout(
    val timeout: Long
)