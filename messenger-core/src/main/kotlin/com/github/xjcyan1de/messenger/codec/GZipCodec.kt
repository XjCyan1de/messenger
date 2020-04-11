package com.github.xjcyan1de.messenger.codec

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GZipCodec<M>(
        val delegate: Codec<M>
) : Codec<M> {
    override fun encode(message: M): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).use {
            it.write(delegate.encode(message))
        }
        return byteArrayOutputStream.toByteArray()
    }

    override fun decode(byteArray: ByteArray): M {
        val uncompressed = GZIPInputStream(ByteArrayInputStream(byteArray)).use {
            it.readBytes()
        }
        try {
            return delegate.decode(uncompressed)
        } catch (e: Exception) {
            throw RuntimeException("Unable to decode message: ${Base64.getEncoder().encodeToString(uncompressed)}", e)
        }
    }
}