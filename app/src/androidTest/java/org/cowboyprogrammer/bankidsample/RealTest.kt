package org.cowboyprogrammer.bankidsample

import androidx.test.runner.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RealTest {
    @Test
    fun boom() {
        val api =
            getBankIdApi(okHttpClient = okHttpClient())

        val call = api.auth(AuthBody(endUserIp = runBlocking {
            getExternalIpAddress().also {
                System.err.println("ip: $it")
            }
        }))

        assertTrue {
            call.request().isHttps
        }

        assertEquals("application/json", call.request().body()!!.contentType().toString())

        val response = call.execute()

        println("Response: $response")

        assertTrue(message = "${response.errorBody()?.toErrorResponse() ?: response.message()}") {
            response.isSuccessful
        }
    }
}
