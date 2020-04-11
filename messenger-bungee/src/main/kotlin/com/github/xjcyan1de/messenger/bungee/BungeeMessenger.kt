package com.github.xjcyan1de.messenger.bungee

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.messaging.PluginMessageListener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Predicate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object BungeeMessenger : PluginMessageListener, Listener {
    private const val CHANNEL = "BungeeCord"
    private const val ALL_SERVERS = "ALL"
    private const val ONLINE_SERVERS = "ONLINE"

    private val setup = AtomicBoolean(false)
    private val listeners = LinkedList<MessageCallback>()
    private val lock = ReentrantLock()
    private val queuedMessages = ConcurrentHashMap.newKeySet<MessageAgent>()
    private lateinit var plugin: Plugin

    fun initialize(plugin: Plugin) {
        this.plugin = plugin

        if (setup.compareAndSet(false, true)) {
            return
        }

        plugin.server.messenger.registerOutgoingPluginChannel(plugin, CHANNEL)
        plugin.server.messenger.registerIncomingPluginChannel(plugin, CHANNEL, this)
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onDisable(event: PluginDisableEvent) {
        if (event.plugin == plugin) {
            plugin.server.messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL)
            plugin.server.messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, this)
            flushQueuedMessages()
        }
    }

    override fun onPluginMessageReceived(channel: String, player: Player, data: ByteArray) {
        if (channel != CHANNEL) {
            return
        }

        val byteIn = ByteArrayInputStream(data)
        val input = DataInputStream(byteIn)

        val subChannel = input.readUTF()
        byteIn.mark(0)

        lock.lock()
        try {
            val iterator = listeners.iterator()
            while (iterator.hasNext()) {
                val messageCallback = iterator.next()
                if (messageCallback.subChannel != subChannel) {
                    continue
                }
                byteIn.reset()
                val accepted = messageCallback.testResponse(player, input)
                if (!accepted) {
                    continue
                }
                byteIn.reset()
                val shouldRemove = messageCallback.acceptResponse(player, input)
                if (shouldRemove) {
                    iterator.remove()
                }
            }
        } finally {
            lock.unlock()
        }
    }

    private fun sendMessage(agent: MessageAgent) {
        var player: Player? = agent.handle
        if (player != null) {
            check(player.isOnline) { "Player not online" }
            sendToChannel(agent, player)
            return
        }
        player = plugin.server.onlinePlayers.firstOrNull()
        if (player != null) {
            sendToChannel(agent, player)
        } else {
            queuedMessages.add(agent)
        }
    }

    private fun flushQueuedMessages() {
        if (queuedMessages.isEmpty()) {
            return
        }
        val player = plugin.server.onlinePlayers.firstOrNull()
        if (player != null) {
            queuedMessages.removeIf {
                sendToChannel(it, player)
                true
            }
        }
    }

    private fun sendToChannel(agent: MessageAgent, player: Player) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val out = DataOutputStream(byteArrayOutputStream)
        out.writeUTF(agent.subChannel)
        agent.appendPayload(out)
        player.sendPluginMessage(plugin, CHANNEL, byteArrayOutputStream.toByteArray())
        if (agent is MessageCallback) {
            val callback = agent as MessageCallback
            registerCallback(callback)
        }
    }

    private fun registerCallback(callback: MessageCallback) {
        lock.lock()
        try {
            listeners.add(callback)
        } finally {
            lock.unlock()
        }
    }

    fun connect(player: Player, serverName: String) = sendMessage(object : MessageAgent {
        override val subChannel = "Connect"
        override val handle = player
        override fun appendPayload(out: DataOutputStream) {
            out.writeUTF(serverName)
        }
    })

    fun connectOther(playerName: String, serverName: String) = sendMessage(object : MessageAgent {
        override val subChannel: String = "ConnectOther"
        override fun appendPayload(out: DataOutputStream) {
            out.writeUTF(playerName)
            out.writeUTF(serverName)
        }
    })

    suspend fun ip(player: Player) = suspendCoroutine<Pair<String, Int>> {
        sendMessage(object : MessageAgent, MessageCallback {
            override val subChannel: String = "IP"
            override var handle: Player = player
            override fun testResponse(receiver: Player, input: DataInputStream): Boolean =
                    receiver.uniqueId == handle.uniqueId

            override fun acceptResponse(receiver: Player, input: DataInputStream): Boolean {
                val ip = input.readUTF()
                val port = input.readInt()
                it.resume(ip to port)
                return true
            }
        })
    }

    suspend fun playerCount(serverName: String) = suspendCoroutine<Int> {
        sendMessage(object : MessageAgent, MessageCallback {
            override val subChannel: String = "PlayerCount"
            override fun appendPayload(out: DataOutputStream) {
                out.writeUTF(serverName)
            }

            override fun testResponse(receiver: Player, input: DataInputStream): Boolean =
                    input.readUTF().equals(serverName, ignoreCase = true)

            override fun acceptResponse(receiver: Player, input: DataInputStream): Boolean {
                input.readUTF()
                val count = input.readInt()
                it.resume(count)
                return true
            }
        })
    }

    suspend fun playerList(serverName: String) = suspendCoroutine<Collection<String>> {
        sendMessage(object : MessageAgent, MessageCallback {
            override val subChannel: String = "PlayerList"
            override fun appendPayload(out: DataOutputStream) {
                out.writeUTF(serverName)
            }

            override fun testResponse(receiver: Player, input: DataInputStream): Boolean =
                    input.readUTF().equals(serverName, ignoreCase = true)

            override fun acceptResponse(receiver: Player, input: DataInputStream): Boolean {
                input.readUTF()
                val csv = input.readUTF()
                if (csv.isEmpty()) {
                    it.resume(emptyList())
                    return true
                }
                it.resume(csv.split(", ").toList())
                return true
            }
        })
    }

    suspend fun getServers() = suspendCoroutine<Collection<String>> {
        sendMessage(object : MessageAgent, MessageCallback {
            override val subChannel: String = "GetServers"
            override fun acceptResponse(receiver: Player, input: DataInputStream): Boolean {
                val csv = input.readUTF()
                if (csv.isEmpty()) {
                    it.resume(emptyList())
                    return true
                }
                it.resume(csv.split(", ").toList())
                return true
            }
        })
    }

    fun message(playerName: String, message: String) = sendMessage(object : MessageAgent {
        override val subChannel = "Message"
        override fun appendPayload(out: DataOutputStream) {
            out.writeUTF(playerName)
            out.writeUTF(message)
        }
    })

    suspend fun getServer() = suspendCoroutine<String> {
        sendMessage(object : MessageAgent, MessageCallback {
            override val subChannel = "GetServer"
            override fun acceptResponse(receiver: Player, input: DataInputStream): Boolean {
                it.resume(input.readUTF())
                return true
            }
        })
    }

    suspend fun uuid(player: Player) = suspendCoroutine<UUID> {
        sendMessage(object : MessageAgent, MessageCallback {
            override val subChannel = "UUID"
            override val handle: Player = player
            override fun testResponse(receiver: Player, input: DataInputStream): Boolean {
                return receiver.uniqueId == handle.uniqueId
            }

            override fun acceptResponse(receiver: Player, input: DataInputStream): Boolean {
                val uuid = input.readUTF()
                it.resume(UUID.fromString(uuid))
                return true
            }
        })
    }

    suspend fun uuidOther(playerName: String) = suspendCoroutine<UUID> {
        sendMessage(object : MessageAgent, MessageCallback {
            override val subChannel = "UUIDOther"
            override fun appendPayload(out: DataOutputStream) {
                out.writeUTF(playerName)
            }

            override fun testResponse(receiver: Player, input: DataInputStream): Boolean {
                return input.readUTF().equals(playerName, ignoreCase = true)
            }

            override fun acceptResponse(receiver: Player, input: DataInputStream): Boolean {
                input.readUTF()
                val uuid = input.readUTF()
                it.resume(UUID.fromString(uuid))
                return true
            }
        })
    }

    suspend fun serverIp(serverName: String) = suspendCoroutine<Pair<String, Int>> {
        sendMessage(object : MessageAgent, MessageCallback {
            override val subChannel = "ServerIP"
            override fun appendPayload(out: DataOutputStream) {
                out.writeUTF(serverName)
            }

            override fun testResponse(receiver: Player, input: DataInputStream): Boolean {
                return input.readUTF().equals(serverName, ignoreCase = true)
            }

            override fun acceptResponse(receiver: Player, input: DataInputStream): Boolean {
                input.readUTF()
                val ip = input.readUTF()
                val port = input.readInt()
                it.resume(ip to port)
                return true
            }
        })
    }

    fun kickPlayer(playerName: String, reason: String) = sendMessage(object : MessageAgent {
        override val subChannel = "KickPlayer"
        override fun appendPayload(out: DataOutputStream) {
            out.writeUTF(playerName)
            out.writeUTF(reason)
        }
    })

    fun forward(serverName: String, channelName: String, data: ByteArray) =
            sendMessage(object : MessageAgent {
                override val subChannel = "Forward"
                override fun appendPayload(out: DataOutputStream) {
                    out.writeUTF(serverName)
                    out.writeUTF(channelName)
                    out.writeShort(data.size)
                    out.write(data)
                }
            })

    fun forwardToPlayer(playerName: String, channelName: String, data: ByteArray) =
            sendMessage(object : MessageAgent {
                override val subChannel = "ForwardToPlayer"
                override fun appendPayload(out: DataOutputStream) {
                    out.writeUTF(playerName)
                    out.writeUTF(channelName)
                    out.writeShort(data.size)
                    out.write(data)
                }
            })

    fun registerForwardCallback(channelName: String, callback: Predicate<ByteArray>) =
            registerCallback(object : MessageCallback {
                override val subChannel: String = channelName
                override fun acceptResponse(receiver: Player, input: DataInputStream): Boolean {
                    val len = input.readShort()
                    val data = ByteArray(len.toInt())
                    input.readFully(data)
                    return callback.test(data)
                }
            })


    private interface MessageAgent {
        val subChannel: String
        val handle: Player?
            get() = null

        fun appendPayload(out: DataOutputStream) {}
    }

    private interface MessageCallback {
        val subChannel: String
        fun testResponse(receiver: Player, input: DataInputStream): Boolean = true
        fun acceptResponse(receiver: Player, input: DataInputStream): Boolean
    }
}