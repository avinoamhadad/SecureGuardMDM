package com.secureguard.mdm

import com.emanuelef.remote_capture.PCAPdroid
import com.secureguard.mdm.utils.AppLogger
import dagger.hilt.android.HiltAndroidApp
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

@HiltAndroidApp
class SecureGuardApplication : PCAPdroid() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.i("Application", "App started. Logger initialized.")
        setupGlobalSslTrust()
    }

    private fun setupGlobalSslTrust() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            })

            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
            AppLogger.i("Application", "Global SSL trust established.")
        } catch (e: Exception) {
            AppLogger.e("Application", "Failed to setup global SSL trust: ${e.message}")
        }
    }
}