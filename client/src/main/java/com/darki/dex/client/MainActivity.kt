package com.darki.dex.client

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "DARKI-Dex Desktop\n\nLenovo Tab 6 • LineageOS\nRole: Desktop Client\n\nWaiting for Host…"
            textSize = 18f
            setPadding(48, 48, 48, 32)
        }

        val connect = Button(this).apply {
            text = "Discover host"
            setOnClickListener {
                title.text = "DARKI-Dex Desktop\n\nSearching local network…\n\nDiscovery transport comes next."
            }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(connect)
        })
    }
}
