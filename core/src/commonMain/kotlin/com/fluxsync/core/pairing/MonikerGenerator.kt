package com.fluxsync.core.pairing

import com.fluxsync.core.platform.secureRandomBytes

object MonikerGenerator {
    private val adjectives = listOf("Clever", "Swift", "Bright", "Steady", "Brave", "Calm", "Electric", "Lucky")
    private val nouns = listOf("Tomato", "Comet", "Maple", "Signal", "Pixel", "Lantern", "Harbor", "Circuit")

    fun generate(prefix: String = "Umair"): String {
        val bytes = secureRandomBytes(2)
        val adjective = adjectives[(bytes[0].toInt() and 0xFF) % adjectives.size]
        val noun = nouns[(bytes[1].toInt() and 0xFF) % nouns.size]
        return "$prefix's $adjective $noun"
    }
}
