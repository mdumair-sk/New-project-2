package com.fluxsync.core.platform

expect fun deriveKey(pin: String, salt: ByteArray): ByteArray

expect fun secureRandomBytes(length: Int): ByteArray
