package org.cowboyprogrammer.bankidsample


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.*


class LoginFragment : CoroutineScopedFragment() {
    val api by lazy {
        getBankIdApi(okHttpClient = okHttpClient())
    }
    var loginJob: Job? = null
    var collectJob: Job? = null
    var orderRef: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false).apply {
            findViewById<View>(R.id.button_login)?.setOnClickListener {
                initiateLogin()
            }

            findViewById<View>(R.id.button_cancel)?.setOnClickListener {
                cancelLogin()
            }
        }
    }

    fun initiateLogin() {
        loginJob?.cancel()
        collectJob?.cancel()

        login_1.visibility = View.INVISIBLE
        login_2.visibility = View.VISIBLE
        login_3.visibility = View.INVISIBLE

        loginJob = launch(Dispatchers.Default) {
            var autoStartToken: String? = null
            try {
                val response = withContext(Dispatchers.IO) {
                    api.auth(
                        AuthBody(
                            endUserIp = getExternalIpAddress()
                        )
                    ).execute()
                }

                if (response.isSuccessful) {
                    autoStartToken = response.body()?.autoStartToken
                    orderRef = response.body()?.orderRef

                    waitForLoginToComplete()

                    startActivity(Intent().apply {
                        setPackage("com.bankid.bus")
                        action = Intent.ACTION_VIEW
                        data =
                            Uri.parse("bankid:///?autostarttoken=$autoStartToken&redirect=null")
                    })
                } else {
                    throw Exception(response.errorBody()?.toErrorResponse()?.details ?: response.message())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = e.message ?: "Error"

                withContext(Dispatchers.Main) {
                    login_1.visibility = View.INVISIBLE
                    login_2.visibility = View.INVISIBLE
                    login_3.visibility = View.VISIBLE

                    text_result.text = "Error: $errorMsg"
                }
            }
        }
    }

    fun waitForLoginToComplete() {
        collectJob = launch(Dispatchers.Default) {
            orderRef.let { orderRef ->
                var status: String = "pending"
                var result: CollectResponse? = null
                while (status == "pending" && orderRef != null) {
                    // Status should be checked every 2 seconds
                    delay(2000)
                    try {
                        val response = api.collect(CollectBody(orderRef = orderRef)).execute()
                        if (response.isSuccessful) {
                            result = response.body()
                            status = response.body()?.status?.trim() ?: "failed"
                        }
                    } catch (e: Throwable) {
                        status = "error: $e"
                    }
                }

                withContext(Dispatchers.Main) {
                    login_1.visibility = View.INVISIBLE
                    login_2.visibility = View.INVISIBLE
                    login_3.visibility = View.VISIBLE

                    if (status == "complete") {
                        // Success
                        text_result.text = "Hello ${result?.completionData?.user?.name}"
                    } else {
                        text_result.text = "Failure. Status: $status, hintCode: ${result?.hintCode}"
                    }
                }
            }
        }
    }

    fun cancelLogin() {
        loginJob?.cancel()
        collectJob?.cancel()

        orderRef?.let { orderRef ->
            launch(Dispatchers.Default) {
                api.cancel(CancelBody(orderRef = orderRef))
            }
        }

        login_1.visibility = View.VISIBLE
        login_2.visibility = View.INVISIBLE
        login_3.visibility = View.INVISIBLE
    }
}
