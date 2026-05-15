package com.fluxsync.core.discovery

import com.fluxsync.core.model.Peer
import com.fluxsync.core.pairing.MonikerGenerator
import com.fluxsync.core.protocol.ProtocolConstants
import com.fluxsync.core.platform.secureRandomBytes

class ManualEntryDiscovery {
    fun resolve(input: String, localPeer: Peer): Result<Peer> {
        val trimmed = input.trim()
        val parts = trimmed.split(":")
        val host = parts.getOrNull(0).orEmpty()
        val port = parts.getOrNull(1)?.toIntOrNull() ?: ProtocolConstants.WS_PORT

        if (!isValidHost(host) || port !in 1..65535) {
            return Result.failure(IllegalArgumentException("Enter a valid IP address and port"))
        }

        val random = secureRandomBytes(8).joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        return Result.success(
            Peer(
                id = "manual-$random",
                moniker = MonikerGenerator.generate(prefix = localPeer.moniker.substringBefore("'s")),
                isTrusted = false,
                address = host,
                port = port,
            ),
        )
    }

    private fun isValidHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        val octets = host.split(".")
        return octets.size == 4 && octets.all { it.toIntOrNull() in 0..255 }
    }
}
