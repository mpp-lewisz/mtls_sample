package com.example.mtls_1_3_test

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mtls_1_3_test.MainActivity.Companion.DEVICE_ID
import com.example.mtls_1_3_test.MainActivity.Companion.TAG
import com.example.mtls_1_3_test.MainActivity.Companion.URL
import com.example.mtls_1_3_test.ui.theme.MTLS_1_3_TestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security


class MainActivity : ComponentActivity() {
    companion object {
        const val DEVICE_ID = "lewis"
        const val URL = "https://192.168.31.133:3000"
        const val TAG = "mTLS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())

        setContent {
            MTLS_1_3_TestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TestPanel(Modifier.padding(32.dp))
                }
            }
        }
    }
}

@Composable
fun TestPanel(modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val openDialog = remember { mutableStateOf("") }
    val assets = LocalContext.current.assets

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            onClick = {
                coroutineScope.launch(Dispatchers.Default) {
                    try {
                        val csr = IdentityKeyStoreProvider(assets).createIdentity()
                        Log.i(TAG, "Size: ${IdentityKeyStoreProvider(assets).size()}")
                        openDialog.value = csr
                    } catch(e: Exception) {
                        Log.e(TAG, e.localizedMessage, e)
                        openDialog.value = e.stackTraceToString()
                    }
                }
            }
        ) {
            Text("Generate Identity Key", modifier=modifier)
        }

        TextButton(
            onClick = {
                coroutineScope.launch(Dispatchers.Default) {
                    try {
                        val res = Connector(assets, DEVICE_ID, URL).connect()
                        openDialog.value = res
                    } catch(e: Exception) {
                        Log.e(TAG, e.localizedMessage, e)
                        openDialog.value = e.stackTraceToString()
                    }
                }
            }
        ) {
            Text("Connect", modifier=modifier)
        }

        TextButton(
            onClick = {
                coroutineScope.launch(Dispatchers.Default) {
                    try {
                        IdentityKeyStoreProvider(assets).clear()

                        Log.i(TAG, "Size: ${IdentityKeyStoreProvider(assets).size()}")
                    } catch(e: Exception) {
                        Log.e(TAG, e.localizedMessage, e)
                        openDialog.value = e.stackTraceToString()
                    }
                }
            }
        ) {
            Text("Clear Key Store", modifier=modifier)
        }
    }

    if(openDialog.value.isNotBlank()) {
        ExceptionAlertDialog(
            dialogText = openDialog.value
        ) {
            openDialog.value = ""
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExceptionAlertDialog(
    dialogText: String,
    onDismissRequest: (() -> Unit)
) {
    AlertDialog(
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MTLS_1_3_TestTheme {
        TestPanel()
    }
}

