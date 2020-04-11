package com.github.xjcyan1de.messenger

import com.github.xjcyan1de.messenger.codec.Codec
import com.github.xjcyan1de.messenger.codec.GZipCodec
import com.github.xjcyan1de.messenger.codec.GsonCodec
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractChannel<T>
@JvmOverloads
constructor(
        val messenger: AbstractMessenger,
        override val name: String,
        val type: Class<T>,
        override var codec: Codec<T> = GZipCodec(getCodec(type))
) : Channel<T> {
    internal val agents = ConcurrentHashMap.newKeySet<AbstractChannelAgent<T>>()
    private var subscribed: Boolean = false

    fun onIncomingMessage(message: ByteArray) = try {
        val decoded = codec.decode(message)
        for (agent in agents) {
            try {
                agent.onIncomingMessage(decoded)
            } catch (e: Exception) {
                RuntimeException(
                        "Unable to pass decoded message in channel: $name to agent: $decoded",
                        e
                ).printStackTrace()
            }
        }
    } catch (e: Exception) {
        RuntimeException(
                "Unable to decode message in channel $name: ${Base64.getEncoder().encodeToString(message)}",
                e
        ).printStackTrace()
    }

    internal fun checkSubscription() {
        val shouldSubscribe = agents.stream().anyMatch { obj: AbstractChannelAgent<T> -> obj.hasListeners }
        if (shouldSubscribe == subscribed) {
            return
        }
        subscribed = shouldSubscribe
        GlobalScope.launch {
            try {
                if (shouldSubscribe) {
                    messenger.notifySubscribe(name)
                } else {
                    messenger.notifyUnsubscribe(name)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun newAgent(): ChannelAgent<T> {
        val agent = object : AbstractChannelAgent<T>(this) {}
        agents.add(agent)
        return agent
    }

    override fun sendMessage(message: T) {
        GlobalScope.launch {
            val encode = codec.encode(message)
            messenger.outgoingMessage(name, encode)
        }
    }

    companion object {
        private fun <T> getCodec(clazz: Class<T>): Codec<T> {
            return GsonCodec(Gson(), clazz)
        }
    }
}