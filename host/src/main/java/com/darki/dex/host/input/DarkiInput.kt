package com.darki.dex.host.input

import java.nio.ByteBuffer

object DarkiInput {
    const val TYPE_MOUSE = 20
    const val TYPE_KEY = 21
    const val TYPE_TEXT = 22

    const val MOUSE_MOVE = 0
    const val MOUSE_DOWN = 1
    const val MOUSE_UP = 2
    const val MOUSE_SCROLL = 3

    data class MouseEvent(
        val action: Int,
        val x: Float,
        val y: Float,
        val button: Int,
        val scrollX: Float,
        val scrollY: Float
    )

    data class KeyEvent(
        val action: Int,
        val keyCode: Int,
        val metaState: Int,
        val unicodeChar: Int
    )

    fun parseMouse(payload: ByteArray): MouseEvent {
        require(payload.size == 24) { "Invalid mouse payload" }
        val input = ByteBuffer.wrap(payload)
        return MouseEvent(input.get().toInt(), input.float, input.float, input.int, input.float, input.float)
    }

    fun parseKey(payload: ByteArray): KeyEvent {
        require(payload.size == 16) { "Invalid key payload" }
        val input = ByteBuffer.wrap(payload)
        return KeyEvent(input.get().toInt(), input.int, input.int, input.int)
    }
}
