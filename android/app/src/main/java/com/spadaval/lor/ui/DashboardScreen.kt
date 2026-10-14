package com.spadaval.lor.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.spadaval.lor.data.Prefs

/** The LoR dashboard itself (Today, Graph, Memory), inside the app. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val url = remember { Prefs(context).serverUrl }
    var view by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    BackHandler(enabled = canGoBack) { view?.goBack() }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(v: WebView, u: String?, isReload: Boolean) {
                        canGoBack = v.canGoBack()
                    }
                }
                loadUrl(url)
                view = this
            }
        },
    )
}
