package com.fluxsync.android.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.lang.Integer.TYPE
import kotlin.test.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForegroundTransferServiceTest {
    @Test
    fun serviceContractExists() {
        ForegroundTransferService::class.java.getDeclaredMethod("startTransfer", String::class.java, String::class.java, TYPE)
    }
}
