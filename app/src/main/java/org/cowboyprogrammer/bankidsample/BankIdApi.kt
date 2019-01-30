package org.cowboyprogrammer.bankidsample

import com.squareup.moshi.Moshi
import dog.wunder.bankidsample.BuildConfig
import okhttp3.*
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


interface BankIdApi {
    /**
     * Initiates an authentication order. Use the collect method to query the status of the order.
     * If the request is successful, HTTP 200, the orderRef and autoStartToken is returned.
     */
    @Headers("Content-Type: application/json")
    @POST("auth")
    fun auth(@Body body: AuthBody): Call<AuthSignResponse>

    /**
     * Initiates a signing order. Use the collect method to query the status of the order.
     * If the request is successful, HTTP 200, the orderRef and autoStartToken is returned.
     */
    @Headers("Content-Type: application/json")
    @POST("sign")
    fun sign(@Body body: SignBody): Call<AuthSignResponse>

    /**
     * Collects the result of a sign or auth order using the orderRef as reference. Collect should be called
     * every two seconds as long as status indicates pending.
     * The user identity is returned when complete.
     */
    @Headers("Content-Type: application/json")
    @POST("collect")
    fun collect(@Body body: CollectBody): Call<CollectResponse>

    /**
     * Cancels an ongoing sign or auth order. This is typically used if the user cancels
     * the order in your service or app.
     */
    @Headers("Content-Type: application/json")
    @POST("cancel")
    fun cancel(@Body body: CancelBody): Call<CancelResponse>
}

data class AuthSignResponse(
    /**
     * Used as reference to this order when the okHttpClient is started automatically.
     */
    val autoStartToken: String,
    /**
     * Used to collect the status of the order.
     */
    val orderRef: String
)

data class AuthBody(
    /**
     * The user IP address as seen by the app. IPv4 and IPV6 allowed.
     */
    val endUserIp: String,
    /**
     * The personal number of the user written with 12 digits (century must be included).
     */
    val personalNumber: String? = null,
    /**
     * Requirements on how the auth or sign order must be performed.
     */
    val requirement: Requirement? = null
)

data class SignBody(
    /**
     * The text to be displayed and signed. The text can be formatted using CR, LF, and CRLF for new-lines.
     * The text must be encoded as UTF-8 and then base64 encoded. 1-40 000 characters after base64 encoding.
     */
    val userVisibleData: String,
    /**
     * The user IP address as seen by the app. IPv4 and IPV6 allowed.
     */
    val endUserIp: String,
    /**
     * Data not displayed to the user. The value must be base64 encoded. 1-200 000 characters after base64 encoding.
     */
    val userNonVisibleData: String? = null,
    /**
     * The personal number of the user written with 12 digits (century must be included).
     */
    val personalNumber: String? = null,
    /**
     * Requirements on how the auth or sign order must be performed.
     */
    val requirement: Requirement? = null
)

data class Requirement(
    /**
     * If true, user is allowed to use fingerprint for auth and sign.
     */
    val allowFingerprint: Boolean? = null,
    /**
     * If true, the okHttpClient must have been started using the autoStartToken. Use if it's important that the BankID app
     * is on the same device. It does NOT work to set this to false, just don't include it in that case.
     */
    val autoStartTokenRequired: Boolean? = null,
    /**
     * The common name of the issuer. Wildcards are not allowed.
     */
    val issuerCn: List<String>? = null,
    /**
     * The oid in certificate policies in the user certificate. One wildcard is allowed from position 5 and forward,
     * i.e. "1.2.752.78.*"
     */
    val certificatePolicies: List<String>? = null,
    /**
     * class1: (default) The transaction must be performed using a card reader where the PIN code is entered on the
     * computer's keyboard, or a card reader of higher class.
     * class2: The transaction must be performed using a card reader where the PIN code is entered on the reader, or a
     * reader of higher class.
     *
     * This condition should be combined with a certificatePolicies for a smart card to avoid undefined behavior.
     */
    val cardReader: String? = null
)

data class CollectBody(
    /**
     * The orderRef returned from auth or sign.
     */
    val orderRef: String
)

typealias CancelBody = CollectBody

data class CollectResponse(
    /**
     * The orderRef returned from auth or sign.
     */
    val orderRef: String,
    /**
     * pending: The order is being processed. hintCode describes the status of the order.
     * failed: Something went wrong with the order. hintCode describes the error.
     * complete: The order is complete. completionData holds user information.
     */
    val status: String,
    /**
     * Description of pending and failed orders.
     */
    val hintCode: String? = null,
    /**
     * Data for a completed order.
     */
    val completionData: CompletionData? = null
)

data class CompletionData(
    /**
     * Information related to the user.
     */
    val user: User,
    /**
     * Information related to the device.
     */
    val device: Device,
    /**
     * Information related tot he user's certificate (BankID)
     */
    val cert: Cert,
    /**
     * Base64 encoded XML signature.
     */
    val signature: String,
    /**
     * Base64 encoded OCSP response. The OCSP response is signed by a certificate that has the same issuer as
     * the certificate being verified.
     */
    val ocspResponse: String
)

data class User(
    /**
     * The personal number. 12 digits including century.
     */
    val personalNumber: String,
    /**
     * The given name and surname of the user.
     */
    val name: String,
    /**
     * The given name of the user.
     */
    val givenName: String,
    /**
     * The surname of the user.
     */
    val surname: String
)

data class Device(
    /**
     * The IP address of the user agent as the BankID server discovers it.
     */
    val ipAddress: String
)

data class Cert(
    /**
     * Start of validity of the user's BankID. Unix ms.
     */
    val notBefore: String,
    /**
     * End of validity of the user's BankID. Unix ms.
     */
    val notAfter: String
)

/**
 * Cancel returns an empty body on success.
 */
class CancelResponse {
    override fun equals(other: Any?): Boolean {
        if (other is CancelResponse) {
            return true
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

/**
 * Body returned by BankId API on failure
 */
data class ErrorResponse(
    val errorCode: String? = null,
    val details: String? = null
)

/**
 * Parses an error body if it is JSON formatted, otherwise returns null.
 */
fun ResponseBody.toErrorResponse(): ErrorResponse? =
    when (contentType()?.subtype()) {
        "json" -> Moshi.Builder().build().adapter<ErrorResponse>(ErrorResponse::class.java).fromJson(source())
        else -> null
    }

private const val BANKID_PROD_HOSTNAME = "appapi2.bankid.com"
private const val BANKID_TEST_HOSTNAME = "appapi2.test.bankid.com"
private const val BANKID_PROD_URL = "https://$BANKID_PROD_HOSTNAME/rp/v5/"
private const val BANKID_TEST_URL = "https://$BANKID_TEST_HOSTNAME/rp/v5/"

val BANKID_API_URL: String by lazy {
    when (BuildConfig.FLAVOR) {
        "prod" -> BANKID_PROD_URL
        else -> BANKID_TEST_URL
    }
}

fun getBankIdApi(
    url: String = BANKID_API_URL,
    okHttpClient: OkHttpClient
): BankIdApi {
    return Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(BankIdApi::class.java)
}

const val TEST_KEY_ALIAS = "{557681F5-FDF4-4AA2-AC94-E4591DCB02D5}"
const val TEST_CERT_ALIAS = "FP Testcert 2\u0000"

val demoServerCertificate: X509Certificate by lazy {
    BankIdApi::class.java.getResourceAsStream("/appapi2testbankidcom.crt").use {
        val fact = CertificateFactory.getInstance("X.509")
        fact.generateCertificate(it) as X509Certificate
    }
}

val prodServerCertificate: X509Certificate by lazy {
    BankIdApi::class.java.getResourceAsStream("/appapi2bankidcom.crt").use {
        val fact = CertificateFactory.getInstance("X.509")
        fact.generateCertificate(it) as X509Certificate
    }
}

val testClientHeldCertificate: HeldCertificate by lazy {
    val keyStore = KeyStore.getInstance("pkcs12")
    BankIdApi::class.java.getResourceAsStream("/FPTestcert2_20150818_102329.pfx").use {
        keyStore.load(it, "qwerty123".toCharArray())
    }

    val testClientPrivateKey = keyStore.getKey(TEST_KEY_ALIAS, "qwerty123".toCharArray())!! as PrivateKey
    val testClientCertificate = keyStore.getCertificate(TEST_CERT_ALIAS)!! as X509Certificate

    HeldCertificate.Builder()
        .keyPair(testClientCertificate.publicKey, testClientPrivateKey)
        .commonName(testClientCertificate.subjectX500Principal.name)
        .validityInterval(testClientCertificate.notBefore.time, testClientCertificate.notAfter.time)
        .serialNumber(testClientCertificate.serialNumber)
        .build()

    HeldCertificate(
        KeyPair(testClientCertificate.publicKey, testClientPrivateKey),
        testClientCertificate
    )
}

val clientCertificate: HeldCertificate by lazy {
    when (BuildConfig.FLAVOR) {
        "prod" -> TODO("No production okHttpClient cert yet")
        else -> testClientHeldCertificate
    }
}

fun okHttpClient(
    testServerCertificate: X509Certificate? = null
): OkHttpClient {
    val clientCertificates = HandshakeCertificates.Builder()
        .addPlatformTrustedCertificates()
        .heldCertificate(clientCertificate)
        .apply {
            @Suppress("ConstantConditionIf")
            when (BuildConfig.FLAVOR) {
                "demo" -> addTrustedCertificate(demoServerCertificate)
                "prod" -> addTrustedCertificate(prodServerCertificate)
            }
            if (testServerCertificate != null) {
                addTrustedCertificate(testServerCertificate)
            }
        }
        .build()

    val certificatePinner = CertificatePinner.Builder()
        .add(BANKID_TEST_HOSTNAME, "sha256/v9D5/j22rQiT+Uk5ZvCpPrqf+pskn/bR0DxL5AdH9qw=")
        .add(BANKID_PROD_HOSTNAME, "sha256/IdrpH8lWLA16etQftRpTcKfhpFyrPNiicGzDM1cUMdA=")
        .build()

    val client = OkHttpClient.Builder()
        .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager())
        .connectionSpecs(listOf(ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .build()))
        .hostnameVerifier { hostname, session ->
            when (hostname) {
                BANKID_PROD_HOSTNAME -> BuildConfig.FLAVOR == "prod"
                BANKID_TEST_HOSTNAME -> BuildConfig.FLAVOR == "demo"
                else -> testServerCertificate != null
            }
        }
        .certificatePinner(certificatePinner)
        .build()

    return client
}
