package org.cowboyprogrammer.bankidsample

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IpAddressKtTest {
    @Test
    fun getExternalIp() {
        val ip = runBlocking {
            getExternalIpAddress()
        }
        assertNotNull(ip)
        assertTrue(message = "Should start with number") {
            "\\d+\\.\\d+\\.\\d+\\.\\d+".toRegex().matches(ip)
        }
    }
}
