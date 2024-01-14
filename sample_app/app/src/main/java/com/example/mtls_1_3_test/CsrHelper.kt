package com.example.mtls_1_3_test

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.ExtensionsGenerator
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.PrivateKey
import java.security.Signature
import java.util.Locale


object CsrHelper {
    private val DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA"
    private val CN_PATTERN = "CN=%s, O=Aralink, OU=OrgUnit"

    //Create the certificate signing request (CSR) from private and public keys
    fun generateCSR(
        keyPair: KeyPair,
        cn: String?
    ): PKCS10CertificationRequest {
        val principal = String.format(CN_PATTERN, cn)
        val signer: ContentSigner =
            JCESigner(keyPair.private, DEFAULT_SIGNATURE_ALGORITHM)
        val csrBuilder: PKCS10CertificationRequestBuilder =
            JcaPKCS10CertificationRequestBuilder(
                X500Name(principal), keyPair.public
            )
        val extensionsGenerator =
            ExtensionsGenerator()
        extensionsGenerator.addExtension(
            Extension.basicConstraints, true, BasicConstraints(
                true
            )
        )
        csrBuilder.addAttribute(
            PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
            extensionsGenerator.generate()
        )
        return csrBuilder.build(signer)
    }

    private class JCESigner(privateKey: PrivateKey?, sigAlgo: String) :
        ContentSigner {
        private val mAlgo: String
        private var signature: Signature? = null
        private var outputStream: ByteArrayOutputStream? = null

        init {
            //Utils.throwIfNull(privateKey, sigAlgo);
            mAlgo = sigAlgo.lowercase(Locale.getDefault())
            try {
                outputStream = ByteArrayOutputStream()
                signature = Signature.getInstance(sigAlgo)
                signature?.initSign(privateKey)
            } catch (gse: GeneralSecurityException) {
                throw IllegalArgumentException(gse.message)
            }
        }

        override fun getAlgorithmIdentifier(): AlgorithmIdentifier {
            val id = ALGOS.get(mAlgo)
                ?: throw IllegalArgumentException(
                    "Does not support algo: " +
                            mAlgo
                )
            return id
        }

        override fun getOutputStream(): OutputStream {
            return (outputStream)!!
        }

        override fun getSignature(): ByteArray? {
            try {
                signature?.update(outputStream!!.toByteArray())
                return signature?.sign()
            } catch (gse: GeneralSecurityException) {
                gse.printStackTrace()
                return null
            }
        }

        companion object {
            private val ALGOS: MutableMap<String, AlgorithmIdentifier> = HashMap()

            init {
                ALGOS["SHA256withRSA".lowercase(Locale.getDefault())] =
                    AlgorithmIdentifier(
                        ASN1ObjectIdentifier("1.2.840.113549.1.1.11")
                    )
//                ALGOS["SHA1withRSA".lowercase(Locale.getDefault())] =
//                    AlgorithmIdentifier(
//                        ASN1ObjectIdentifier("1.2.840.113549.1.1.5")
//                    )
            }
        }
    }
}