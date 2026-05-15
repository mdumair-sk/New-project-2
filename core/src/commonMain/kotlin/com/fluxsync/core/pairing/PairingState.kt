package com.fluxsync.core.pairing

sealed class PairingState {
    data object Idle : PairingState()
    data class AwaitingConfirmation(val pin: String, val expiresAt: Long) : PairingState()
    data class Confirmed(val encryptedKey: ByteArray, val salt: ByteArray) : PairingState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Confirmed) return false
            return encryptedKey.contentEquals(other.encryptedKey) && salt.contentEquals(other.salt)
        }

        override fun hashCode(): Int {
            var result = encryptedKey.contentHashCode()
            result = 31 * result + salt.contentHashCode()
            return result
        }
    }
    data object Expired : PairingState()
    data object Failed : PairingState()
}
