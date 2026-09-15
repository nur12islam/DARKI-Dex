package com.darki.link

import org.json.JSONObject
import java.util.UUID

/** Protocol primitives shared by every DARKI Link transport. */
object Protocol {
    const val VERSION = 1

    fun message(type: String, payload: JSONObject = JSONObject()): JSONObject =
        JSONObject()
            .put("version", VERSION)
            .put("type", type)
            .put("id", UUID.randomUUID().toString())
            .put("timestamp", System.currentTimeMillis())
            .put("payload", payload)

    fun isSupported(message: JSONObject): Boolean =
        message.optInt("version", -1) == VERSION && message.has("type")
}
