package com.github.xjcyan1de.messenger.codec

import com.google.gson.Gson
import java.io.*

class GsonCodec<M>(
        private val gson: Gson,
        private val type: Class<M>
) : Codec<M> {
    override fun encode(message: M): ByteArray = try {
        val byteOut = ByteArrayOutputStream()
        OutputStreamWriter(byteOut).use {
            gson.toJson(message, type, it)
        }
        byteOut.toByteArray()
    } catch (e: IOException) {
        throw EncodingException(e)
    }

    override fun decode(byteArray: ByteArray): M = try {
        InputStreamReader(ByteArrayInputStream(byteArray)).use {
            gson.fromJson(it, type)
        }
    } catch (e: Exception) {
        throw EncodingException(e)
    }
}
