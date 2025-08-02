package com.eltonkola.dreamcraft

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File


class WebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).run {
            isAppearanceLightStatusBars = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Fix for three-button nav not properly going edge-to-edge.
            // See: https://issuetracker.google.com/issues/298296168
            window.isNavigationBarContrastEnforced = false
        }

        val path = intent?.getStringExtra("html_path")
        val file = path?.let { File(it) }

        setContent {
            if (file != null && file.exists()) {
                Box( // Added Box wrapper
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars) // Applied padding
                ) {

                    HtmlWebViewScreen(
                        gameFile = file
                    )

                }
            } else {
                Box( // Added Box wrapper
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars), // Applied padding
                    contentAlignment = Alignment.Center
                ) {
                    Text("HTML file not found")
                }
            }
        }
    }

}

@SuppressLint("SetJavaScriptEnabled") // Keep if you want to suppress this specific lint warning
@Composable
fun HtmlWebViewScreen(gameFile: File) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // Layer type: Enable hardware acceleration explicitly for the WebView's view
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d(
                                "HtmlWebViewScreen",
                                "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}"
                            )
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                    // You could override onProgressChanged here to show a loading bar
                    // override fun onProgressChanged(view: WebView?, newProgress: Int) { ... }
                }

                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true // Be cautious if loading untrusted external file:// content

                    domStorageEnabled = true
                    databaseEnabled = true

                    // Performance & Rendering Optimizations
                    cacheMode = WebSettings.LOAD_DEFAULT // Default, but good to be explicit for local files

                    // Viewport and Scaling (crucial for games to display correctly)
                    loadWithOverviewMode = true // Load the HTML content in overview mode (i.e., zoom out to fit content)
                    useWideViewPort = true    // Enable support for the viewport meta tag, or use a wide viewport if no tag

                    // Disable zoom controls for a game-like experience
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false

                    // Media playback
                    // Allows media (audio/video) to play without a direct user gesture first.
                    // Test thoroughly, as behavior can be inconsistent and is often restricted by modern policies.
                    // Might be useful for background music or intro videos in games.
                    mediaPlaybackRequiresUserGesture = false

                    // Optional: If you face issues with text scaling in games (rare for canvas based games)
                    // textZoom = 100 // Explicitly set text zoom to 100%

                    // Optional: Further optimize for games if applicable
                    // setRenderPriority(WebSettings.RenderPriority.HIGH) // Deprecated in API 18, no longer effective
                }

                loadUrl(Uri.fromFile(gameFile).toString())
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


