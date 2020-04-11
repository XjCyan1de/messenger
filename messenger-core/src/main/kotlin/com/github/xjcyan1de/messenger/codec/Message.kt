package com.github.xjcyan1de.messenger.codec

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Message(
        val codec: KClass<out Codec<*>>
)
