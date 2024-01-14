package com.example.mtls_1_3_test

import android.content.res.AssetManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import com.example.mtls_1_3_test.CsrHelper.generateCSR
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date
import javax.security.auth.x500.X500Principal


class IdentityKeyStoreProvider(private val assets: AssetManager) {
    fun isHardwareBackedKeyStore(): Boolean {
        val keyStore = KeyStore.getInstance(KEY_PROVIDER).apply { load(null) }
        val privateKey = keyStore.getKey(INSTANCE_MUT_KEY, null)
        val keyInfo = KeyFactory.getInstance(privateKey.algorithm, KEY_PROVIDER)
            .getKeySpec(privateKey, KeyInfo::class.java)

        return keyInfo.isInsideSecureHardware
    }

    fun getKeyStore(key: String): KeyStore {
        if (!key.startsWith("client.")
            && key != "badssl") {
            return getHardwareKeyStore(key)
        }
        return emulatedKeyStore(key)
    }

    private fun getHardwareKeyStore(key: String): KeyStore {
        val keyStore = KeyStore.getInstance(KEY_PROVIDER).apply { load(null) }

        val certificate: Certificate = assets.open("$key.pem")
            .use {
                CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(it)
            }

        keyStore.setKeyEntry(
            INSTANCE_MUT_KEY,
            keyStore.getKey(INSTANCE_MUT_KEY, null),
            null,
            arrayOf(certificate))

        return keyStore
    }

    private fun emulatedKeyStore(key: String): KeyStore {
        val identityStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
        }

        val certificate: Certificate = assets.open("client.$key.pem").use {
            CertificateFactory.getInstance("X.509")
                .generateCertificate(it)
        }

        val privateKeyContent = assets.open("client.$key.key").use {
            String(
                it.readBytes(),
                Charset.defaultCharset())
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace(System.lineSeparator().toRegex(), "")
                .replace("-----END PRIVATE KEY-----", "")
        }

        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent))

        identityStore.setKeyEntry(
            "client",
            KeyFactory.getInstance("RSA").generatePrivate(keySpec),
            null,
            arrayOf(certificate)
        )

        return identityStore
    }

    fun clear() {
        val keyStore = KeyStore.getInstance(KEY_PROVIDER).apply { load(null) }
        for (alias in keyStore.aliases()) {
            keyStore.deleteEntry(alias)
        }
    }

    fun size(): Int {
        val keyStore = KeyStore.getInstance(KEY_PROVIDER).apply { load(null) }
        return keyStore.size()
    }

    fun createIdentity(): String {
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            KEY_PROVIDER
        )
        val parameterSpec = KeyGenParameterSpec.Builder(
            INSTANCE_MUT_KEY,
            KeyProperties.PURPOSE_SIGN
                    or KeyProperties.PURPOSE_VERIFY
                    or KeyProperties.PURPOSE_ENCRYPT
                    or KeyProperties.PURPOSE_DECRYPT
            ).run {

            setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)

            setRandomizedEncryptionRequired(false)
            setKeySize(2048)

            setKeyValidityEnd(Date.from(Instant.now().plusSeconds(INSTANCE_MUT_KEY_EXPIRY)))
//            setCertificateSubject(X500Principal("CN=" + INSTANCE_MUT_KEY))
            build()
        }

        kpg.initialize(parameterSpec)

        val keypair = kpg.generateKeyPair()

        val csr = generateCSR(keypair, "device_1949585578")
        val base64 = String(Base64.getEncoder().encode(csr.encoded))
        val req = "-----BEGIN CERTIFICATE REQUEST-----\n" +
                base64.chunked(64).joinToString("\n") +
                "\n-----END CERTIFICATE REQUEST-----\n"
        Log.d("mTLS", "Save the CSR below to ca_signing/${MainActivity.DEVICE_ID}.csr\n$req")
        return req
    }

    companion object {
        private const val KEY_PROVIDER = "AndroidKeyStore"
        private const val INSTANCE_MUT_KEY = "client"
        private const val INSTANCE_MUT_KEY_EXPIRY = 3600 * 24 * 365L // 365 days
    }
}
