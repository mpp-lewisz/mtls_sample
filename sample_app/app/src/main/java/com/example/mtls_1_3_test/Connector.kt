package com.example.mtls_1_3_test

import android.content.res.AssetManager
import android.util.Log
import okhttp3.CipherSuite.Companion.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
import okhttp3.CipherSuite.Companion.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class Connector(private val assets: AssetManager,
                private val deviceID: String,
                private val url: String) {
    private val keystoreProvider: IdentityKeyStoreProvider =IdentityKeyStoreProvider(assets)

    fun connect(): String {
        val client = buildMTLSClient(deviceID)

        val request = Request.Builder().apply {
            url(url)
            get()
        }.build()

        val response = client.newCall(request).execute()

        if ((200 until 300).contains(response.code)) {
            Log.i("mTLS", "connected OK")
            return response.body?.string() ?: "No response body."
        }
        throw Exception("Status code: ${response.code}")
    }

    private fun getTrustManager(default: Boolean=false): Pair<Array<TrustManager>, X509TrustManager> {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        if(default) {
            trustManagerFactory.init(null as KeyStore?)
        } else {
            assets.open("node.pem").use {
                val trustedCertificate: Certificate =
                    certificateFactory.generateCertificate(it)

                val trustStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                    load(null, null)
                }

                trustStore.setCertificateEntry("server-certificate", trustedCertificate)
                trustManagerFactory.init(trustStore)
            }
        }
        var defaultTm: X509TrustManager? = null
        for (tm in trustManagerFactory.trustManagers) {
            if (tm is X509TrustManager) {
                defaultTm = tm
                break
            }
        }

        return Pair(trustManagerFactory.trustManagers, defaultTm!!)
    }

    private fun buildMTLSClient(key: String): OkHttpClient {
        val trustManagers = getTrustManager(key == "badssl")
        val identityStore = keystoreProvider.getKeyStore(key)

        for (alias in identityStore.aliases()) {
            Log.d("mTLS", alias)
        }

        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(identityStore, null)

        val keyManagers = keyManagerFactory.keyManagers

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, trustManagers.first, null)

        val spec: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.RESTRICTED_TLS)
            .tlsVersions(TlsVersion.TLS_1_3)
            .cipherSuites(
                TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
            )
            .build()

        val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory,
                trustManagers.second
            )
            .connectionSpecs(listOf(spec))
            .build()
    }
}
