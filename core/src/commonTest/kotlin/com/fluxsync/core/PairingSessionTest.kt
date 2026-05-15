package com.fluxsync.core

import com.fluxsync.core.pairing.PairingSession
import com.fluxsync.core.pairing.PairingState
import com.fluxsync.core.protocol.ProtocolConstants
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class PairingSessionTest {
    @Test
    fun generatePinReturnsSixDigits() = runTest {
        val session = PairingSession(this)
        assertTrue(session.generatePin().matches(Regex("\\d{6}")))
    }

    @Test
    fun confirmPinReturnsTrueOnMatch() = runTest {
        val session = PairingSession(this)
        val pin = session.start()
        assertTrue(session.confirmPin(pin))
    }

    @Test
    fun confirmPinReturnsFalseOnMismatch() = runTest {
        val session = PairingSession(this)
        session.start()
        assertFalse(session.confirmPin("000000"))
    }

    @Test
    fun expiresAfterTimeout() = runTest {
        val session = PairingSession(this)
        session.start()
        advanceTimeBy(ProtocolConstants.PIN_EXPIRY_MS)
        runCurrent()
        assertTrue(session.state.value is PairingState.Expired)
    }
}
