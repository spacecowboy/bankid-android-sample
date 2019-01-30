package org.cowboyprogrammer.bankidsample

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

private val client: OkHttpClient by lazy {
    OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build()
}

/**
 * Returns the external IP address of the device - prioritizing ipv4 but then falling back to ipv6
 */
suspend fun getExternalIpAddress(): String = withContext(Dispatchers.IO) {
    var error: Exception? = null
    try {
        client.newCall(Request.Builder().url("https://ipv4.icanhazip.com/").build()).execute().let { response ->
            if (response.isSuccessful) {
                return@withContext response.body()!!.string().trim()
            }
        }
    } catch (e: IOException) {
        error = e
    }

    try {
        // If that failed then attempt ivp6
        client.newCall(Request.Builder().url("https://ipv6.icanhazip.com/").build()).execute().let { response ->
            if (response.isSuccessful) {
                return@withContext response.body()!!.string().trim()
            }
        }
    } catch (e: IOException) {
        error = error ?: e
    }

    throw IOException("Failed to retrieve external IP address; maybe you are offline?", error)
}
