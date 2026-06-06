package com.example

import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BillingAppShell(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onWebViewCreated = { loadedWebView ->
                            webView = loadedWebView
                        }
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        // Allow navigating backwards in WebView if possible to prevent accidental app exit
        webView?.let {
            if (it.canGoBack()) {
                it.goBack()
            } else {
                super.onBackPressed()
            }
        } ?: super.onBackPressed()
    }
}

@Composable
fun BillingAppShell(modifier: Modifier = Modifier, onWebViewCreated: (WebView) -> Unit) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Configure standard security context details
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                // Attach custom clients to support JavaScript dialog interfaces natively
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        // Prevent page breakouts
                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    // Map HTML alert() calls to native android dialogs
                    override fun onJsAlert(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        result: JsResult?
                    ): Boolean {
                        AlertDialog.Builder(context)
                            .setTitle("Malar Auto Garage")
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                                result?.confirm()
                            }
                            .setCancelable(false)
                            .show()
                        return true
                    }

                    // Map HTML confirm() calls to native dialogs
                    override fun onJsConfirm(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        result: JsResult?
                    ): Boolean {
                        AlertDialog.Builder(context)
                            .setTitle("Confirm Action")
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                                result?.confirm()
                            }
                            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                                result?.cancel()
                            }
                            .setCancelable(false)
                            .show()
                        return true
                    }

                    // Map HTML prompt() calls to native input dialogue popups
                    override fun onJsPrompt(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        defaultValue: String?,
                        result: JsPromptResult?
                    ): Boolean {
                        val input = EditText(context).apply {
                            setText(defaultValue)
                            setSelection(text.length)
                        }
                        AlertDialog.Builder(context)
                            .setTitle(message ?: "Enter Value")
                            .setView(input)
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                                result?.confirm(input.text.toString())
                            }
                            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                                result?.cancel()
                            }
                            .setCancelable(false)
                            .show()
                        return true
                    }

                    // Debug Console updates
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        android.util.Log.d("MalarGarageWebConsole", consoleMessage?.message() ?: "")
                        return true
                    }
                }

                // Load central offline-secured app asset index
                loadUrl("file:///android_asset/billing.html")
                
                onWebViewCreated(this)
            }
        },
        update = {
            // No-op for runtime update since WebView holds self-contained memory bindings
        }
    )
}
