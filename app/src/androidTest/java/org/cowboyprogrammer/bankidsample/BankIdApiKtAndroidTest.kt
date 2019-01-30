package org.cowboyprogrammer.bankidsample

import androidx.test.runner.AndroidJUnit4
import com.squareup.moshi.Moshi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import javax.net.ssl.SSLSocketFactory
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Android does weird things when it comes to networking which is why this is not a regular unit test
 */
@RunWith(AndroidJUnit4::class)
class BankIdApiKtAndroidTest {
    val moshi = Moshi.Builder().build()
    lateinit var mockServer: MockWebServer
    lateinit var api: BankIdApi

    @Before
    fun setupMockServer() {
        mockServer = MockWebServer()
        mockServer.useHttps(serverSslSocketFactory(), false)
        mockServer.start()

        api = getBankIdApi(
            url = mockServer.url("/").toString(),
            okHttpClient = okHttpClient(testServerCertificate = serverLocalhostCertificate.certificate())
        )
    }

    @After
    fun tearDownMockServer() {
        mockServer.close()
    }

    @Test
    fun authHasJSONType() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "autoStartToken": "abc",
                "orderRef": "def"
                }
            """.trimIndent()
            )
        })

        val ipAddress = "127.0.0.1"
        api.auth(
            AuthBody(
                endUserIp = ipAddress
            )
        ).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            "application/json",
            request.getHeader("Content-Type"),
            message = "Request header should specify application/json"
        )
    }

    @Test
    fun signHasJSONType() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "autoStartToken": "abc",
                "orderRef": "def"
                }
            """.trimIndent()
            )
        })

        val ipAddress = "127.0.0.1"
        api.sign(
            SignBody(
                endUserIp = ipAddress,
                userVisibleData = "foo"
            )
        ).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            "application/json",
            request.getHeader("Content-Type"),
            message = "Request header should specify application/json"
        )
    }

    @Test
    fun collectHasJSONType() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "autoStartToken": "abc",
                "orderRef": "def"
                }
            """.trimIndent()
            )
        })

        api.collect(
            CollectBody(
                orderRef = "abc"
            )
        ).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            "application/json",
            request.getHeader("Content-Type"),
            message = "Request header should specify application/json"
        )
    }

    @Test
    fun cancelHasJSONType() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "autoStartToken": "abc",
                "orderRef": "def"
                }
            """.trimIndent()
            )
        })

        api.cancel(
            CancelBody(
                orderRef = "abc"
            )
        ).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            "application/json",
            request.getHeader("Content-Type"),
            message = "Request header should specify application/json"
        )
    }

    @Test
    fun minimal_auth_request_should_parse_correctly() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "autoStartToken": "abc",
                "orderRef": "def"
                }
            """.trimIndent()
            )
        })

        val ipAddress = "127.0.0.1"
        val response = api.auth(
            AuthBody(
                endUserIp = ipAddress
            )
        ).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            "{\"endUserIp\":\"$ipAddress\"}",
            request.body.readByteString().utf8(),
            message = "Request body should not include null values"
        )

        assertEquals(
            AuthSignResponse(
                autoStartToken = "abc",
                orderRef = "def"
            ),
            response.body(),
            message = "Response should have been parsed correctly"
        )
    }

    @Test
    fun maximal_auth_request_should_parse_correctly() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "autoStartToken": "abc",
                "orderRef": "def"
                }
            """.trimIndent()
            )
        })

        val requirement = Requirement(
            allowFingerprint = true,
            autoStartTokenRequired = true,
            issuerCn = listOf("foo"),
            certificatePolicies = listOf("bar"),
            cardReader = "foobar"
        )
        val authBody = AuthBody(
            endUserIp = "127.0.0.1",
            personalNumber = "123456789012",
            requirement = requirement
        )
        val response = api.auth(authBody).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            authBody,
            request.body.toAuthBody(),
            message = "Request should not include values which were not specified"
        )

        assertEquals(
            AuthSignResponse(
                autoStartToken = "abc",
                orderRef = "def"
            ),
            response.body(),
            message = "Response should have been parsed correctly"
        )
    }

    @Test
    fun minimal_sign_should_be_sent_correctly() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "autoStartToken": "abc",
                "orderRef": "def"
                }
            """.trimIndent()
            )
        })

        val signBody = SignBody(
            endUserIp = "127.0.0.1",
            userVisibleData = "Hello there"
        )
        val response = api.sign(signBody).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            "{\"endUserIp\":\"${signBody.endUserIp}\",\"userVisibleData\":\"${signBody.userVisibleData}\"}",
            request.body.readByteString().utf8(),
            message = "Request body should not include null values"
        )

        assertEquals(
            AuthSignResponse(
                autoStartToken = "abc",
                orderRef = "def"
            ),
            response.body(),
            message = "Response should have been parsed correctly"
        )
    }

    @Test
    fun maximal_sign_request_should_parse_correctly() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "autoStartToken": "abc",
                "orderRef": "def"
                }
            """.trimIndent()
            )
        })

        val requirement = Requirement(
            allowFingerprint = true,
            autoStartTokenRequired = true,
            issuerCn = listOf("foo"),
            certificatePolicies = listOf("bar"),
            cardReader = "foobar"
        )
        val signBody = SignBody(
            endUserIp = "127.0.0.1",
            personalNumber = "123456789012",
            requirement = requirement,
            userVisibleData = "Hello there",
            userNonVisibleData = "hidden text"
        )
        val response = api.sign(signBody).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            signBody,
            request.body.toSignBody(),
            message = "Request should not include values which were not specified"
        )

        assertEquals(
            AuthSignResponse(
                autoStartToken = "abc",
                orderRef = "def"
            ),
            response.body(),
            message = "Response should have been parsed correctly"
        )
    }

    @Test
    fun minimal_collect_should_parse_correctly() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "orderRef": "abc",
                "status": "foo"
                }
            """.trimIndent()
            )
        })

        val collectBody = CollectBody(
            orderRef = "abc"
        )
        val response = api.collect(collectBody).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            "{\"orderRef\":\"${collectBody.orderRef}\"}",
            request.body.readByteString().utf8(),
            message = "Request body should not include null values"
        )

        assertEquals(
            CollectResponse(
                orderRef = "abc",
                status = "foo"
            ),
            response.body(),
            message = "Response should have been parsed correctly"
        )
    }

    @Test
    fun cancel_should_parse_correctly() {
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {}
            """.trimIndent()
            )
        })

        val cancelBody = CancelBody(
            orderRef = "abc"
        )
        val response = api.cancel(cancelBody).execute()

        val request = mockServer.takeRequest()

        assertEquals(
            "{\"orderRef\":\"${cancelBody.orderRef}\"}",
            request.body.readByteString().utf8(),
            message = "Request body should not include null values"
        )

        assertEquals(
            CancelResponse(),
            response.body(),
            message = "Response should have been parsed correctly"
        )
    }

    @Test
    fun bad_request_with_no_body_is_OK() {
        mockServer.enqueue(MockResponse().also {
            it.setResponseCode(400)
        })

        val response = api.cancel(CancelBody(orderRef = "abc")).execute()

        assertEquals(
            400,
            response.code(),
            message = "Response code should be 400"
        )

        assertEquals(
            "",
            response.errorBody()?.string(),
            message = "Error body should be empty"
        )

        assertNull(
            response.errorBody()!!.toErrorResponse(),
            message = "Empty error body should return null"
        )
    }

    @Test
    fun bad_request_with_JSON_body_is_parsed_OK() {
        mockServer.enqueue(MockResponse().also {
            it.setResponseCode(400)
            it.setHeader("Content-Type", "application/json")
            it.setBody(
                """
                {
                "errorCode": "invalidParameters",
                "details": "No such order"
                }
            """.trimIndent()
            )
        })

        val response = api.cancel(CancelBody(orderRef = "abc")).execute()

        assertEquals(
            400,
            response.code(),
            message = "Response code should be 400"
        )

        assertEquals(
            ErrorResponse(
                errorCode = "invalidParameters",
                details = "No such order"
            ),
            response.errorBody()?.toErrorResponse(),
            message = "Error body should have been parsed OK"
        )
    }

    @Test
    fun requests_are_made_with_proper_certificates_at_both_ends() {
        mockServer.requestClientAuth()
        mockServer.enqueue(MockResponse().also {
            it.setBody(
                """
                {
                "orderRef": "abc",
                "status": "foo"
                }
            """.trimIndent()
            )
        })

        val response = api.collect(CollectBody(orderRef = "abc")).execute()
        val request = mockServer.takeRequest()

        assertEquals(
            testClientHeldCertificate.certificate().subjectX500Principal,
            request.handshake?.peerPrincipal(),
            message = "Request should include okHttpClient certificate"
        )

        assertEquals(
            serverLocalhostCertificate.certificate().subjectX500Principal,
            response.raw().handshake()?.peerPrincipal(),
            message = "Response should include server certificate"
        )
    }

    private fun Buffer.toAuthBody() =
        moshi.adapter<AuthBody>(AuthBody::class.java).fromJson(this)

    private fun Buffer.toSignBody() =
        moshi.adapter<SignBody>(SignBody::class.java).fromJson(this)

    private val serverLocalhostCertificate: HeldCertificate by lazy {
        val localhost = InetAddress.getByName("localhost").canonicalHostName
        HeldCertificate.Builder()
            .addSubjectAlternativeName(localhost)
            .build()
    }

    private fun serverSslSocketFactory(): SSLSocketFactory? {
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(serverLocalhostCertificate)
            .addTrustedCertificate(testClientHeldCertificate.certificate())
            .build()

        return serverCertificates.sslSocketFactory()
    }
}
