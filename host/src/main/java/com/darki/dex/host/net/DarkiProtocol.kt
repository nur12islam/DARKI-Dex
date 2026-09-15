package com.darki.dex.host.net

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.Socket

/** Versioned framing layer for DARKI-Dex control, input and video packets. */
object DarkiProtocol {
    const val MAGIC = 0x4441524B
    const val VERSION = 1

    const val TYPE_HELLO = 1
    const val TYPE_HELLO_ACK = 2
    const val TYPE_PING = 3
    const val TYPE_PONG = 4
    const val TYPE_VIDEO_CONFIG = 10
    const val TYPE_VIDEO_FRAME = 11
    const val TYPE_MOUSE = 20
    const val TYPE_KEY = 21
    const val TYPE_TEXT = 22
    const val TYPE_NAVIGATION = 23
    const val TYPE_ERROR = 255

    const val MAX_CONTROL_PAYLOAD = 64 * 1024
    const val MAX_VIDEO_PAYLOAD = 4 * 1024 * 1024

    data class Packet(val type: Int, val payload: ByteArray)

    fun write(out: DataOutputStream, type: Int, payload: ByteArray = ByteArray(0)) {
        require(type in 0..255)
        val limit = if (type == TYPE_VIDEO_FRAME) MAX_VIDEO_PAYLOAD else MAX_CONTROL_PAYLOAD
        require(payload.size <= limit) { "payload too large: ${payload.size}" }
        out.writeInt(MAGIC)
        out.writeShort(VERSION)
        out.writeByte(type)
        out.writeInt(payload.size)
        out.write(payload)
        out.flush()
    }

    fun read(input: DataInputStream): Packet {
        try {
            require(input.readInt() == MAGIC) { "Invalid DARKI packet magic" }
            require(input.readUnsignedShort() == VERSION) { "Unsupported DARKI protocol version" }
            val type = input.readUnsignedByte()
            val length = input.readInt()
            val limit = if (type == TYPE_VIDEO_FRAME) MAX_VIDEO_PAYLOAD else MAX_CONTROL_PAYLOAD
            require(length in 0..limit) { "Invalid DARKI payload length: $length" }
            return Packet(type, ByteArray(length).also(input::readFully))
        } catch (e: EOFException) {
            throw e
        }
    }
}

class DarkiSession(private val socket: Socket) : AutoCloseable {
    private val input = DataInputStream(socket.getInputStream().buffered())
    private val output = DataOutputStream(socket.getOutputStream().buffered())
    private val writeLock = Any()

    fun sendHello(deviceName: String, role: String) {
        val payload = "device=$deviceName\nrole=$role\nprotocol=${DarkiProtocol.VERSION}".toByteArray()
        send(DarkiProtocol.TYPE_HELLO, payload)
    }

    fun send(type: Int, payload: ByteArray = ByteArray(0)) {
        synchronized(writeLock) { DarkiProtocol.write(output, type, payload) }
    }

    fun sendPing() = send(DarkiProtocol.TYPE_PING)

    fun receive(): DarkiProtocol.Packet = DarkiProtocol.read(input)

    override fun close() = socket.close()
}
