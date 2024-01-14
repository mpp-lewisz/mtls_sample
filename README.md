CA signing environment
=====================

1. Option 1 - Follow [Create Security Certificates using OpenSSL](https://www.cockroachlabs.com/docs/stable/create-security-certificates-openssl) to create a CA signing environment. So that you can issue certificates to your KEYs, you need to create one **node key** for the TLS server.

2. Option 2 - You can use the ca_signing folder as is.

**Please delete and re-create the ca_signing folder if you choose option 1.**

Generate the app client key.
=====================

1. Open the app folder in Android Studio.

2. Change the server IP address in MainActivity.kt - Set MainActivity.URL to 'https://**your local IP address**:3000'.

3. Change MainActivity.DEVICE_ID to a name you prefer, which is the device ID.

4. Run the app on an actual device or an emulator. If the app runs on an actual device, the device has to be on the same WiFi network with the TLS server running on your laptop.

5. Click the 'Generate Identity Key' button to generate a new key in Hardware KeyStore.

6. A certificate request will be printed out on the Logcat console.

7. Save the certificate request to the file **ca_signing/your_deivce_id**.csr

Issue the certificate
=======================
```console
foo@bar:sample_folder$ cd ca_signing
foo@bar:sample_folder$ ./create_certificate.sh your_device_ID
```
A certificate **your_deivce_ID.pem** is created in the folder ca_signing/certs. Copy the file to the Android Project assets folder - sample_app/app/src/main/assets.

```console
foo@bar:ca_signing$ cp certs/lewis.pem ../sample_app/app/src/main/assets/
```

Test the connection
======================
1. Run the NodeJS test server.
```console
foo@bar:sample_folder$ cd server
foo@bar:sample_folder$ node server.js
```

2. Re-launch the app, as a new certificate has been added. Then press the 'Connect' button.

3. If the mTLS link works, Hello World should pop up.
