package com.darki.dex.client.net

import java.nio.ByteBuffer

object DarkiInput {
    const val ACTION_DOWN = 0
    const val ACTION_UP = 1
    const val ACTION_MOVE = 2
    const val ACTION_SCROLL = 3

    fun mouse(action: Int, x: Float, y: Float, button: Int = 0, scrollX: Float = 0f, scrollY: Float = 0f): ByteArray =
        ByteBuffer.allocate(24).apply {
            putInt(action)
            putInt(button)
            putFloat(x)
            putFloat(y)
            putFloat(scrollX)
            putFloat(scrollY)
        }.array()

    fun key(action: Int, keyCode: Int, metaState: Int): ByteArray =
        ByteBuffer.allocate(12).apply {
            putInt(action)
            putInt(keyCode)
            putInt(metaState)
        }.array()

    fun text(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)
}
