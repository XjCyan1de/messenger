package com.github.xjcyan1de.messenger.codec


interface Codec<M> {
    fun encode(message: M): ByteArray

    fun decode(byteArray: ByteArray): M
}