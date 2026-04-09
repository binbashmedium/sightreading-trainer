// Copyright 2026 BinBashMedium
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.binbashmedium.sightreadingtrainer.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/**
 * Set to `true` by the JavaScript bridge whenever Verovio finishes painting an SVG into the DOM.
 * Instrumented screenshot tests poll this flag (in the same process) to wait for actual rendering
 * rather than relying on a fixed sleep — the 6.7 MB WASM can take 30–60 s to JIT on a cold
 * emulator, far longer than any reasonable hard-coded delay.
 */
object VerovioRenderSignal {
    @Volatile
    var rendered: Boolean = false
}

/** Bridge exposed to JavaScript as `window.Android`. */
private class RenderBridge {
    @JavascriptInterface
    fun onRendered() {
        VerovioRenderSignal.rendered = true
    }
}

/**
 * Renders a segment of the grand staff using Verovio via an embedded WebView.
 *
 * The composable:
 * 1. Loads `staff.html` from assets (which loads `verovio-toolkit-wasm.js`).
 * 2. Converts [gameState] to MEI XML via [MeiConverter] for the [startBeat]..[endBeat) range.
 * 3. Calls `renderMei(meiData, showCursor)` in JavaScript once Verovio is ready.
 * 4. The JavaScript draws a red cursor line at notes tagged with `ncurr*` xml:ids.
 *
 * @param gameState          Current exercise/practice state.
 * @param startBeat          First UI beat-unit to include (inclusive).
 * @param endBeat            Last UI beat-unit to exclude; pass [Float.MAX_VALUE] to show all.
 * @param measureNumberLabel 1-based measure number shown in portrait mode; null = hidden.
 * @param showChordNames     When true, chord/note names are shown above each chord group.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VerovioStaffView(
    modifier: Modifier = Modifier,
    gameState: GameState,
    startBeat: Float = 0f,
    endBeat: Float = Float.MAX_VALUE,
    @Suppress("UNUSED_PARAMETER") measureNumberLabel: Int? = null,
    showChordNames: Boolean = false,
    onFirstRender: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var pageLoaded by remember { mutableStateOf(false) }
    var firstRenderFired by remember { mutableStateOf(false) }

    val webView = remember(context) {
        WebView(context).apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            // Allow loading local scripts (verovio-toolkit-wasm.js) from file:// page
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            // Expose the render-complete bridge so staff.html can signal Android.onRendered().
            addJavascriptInterface(RenderBridge(), "Android")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = true   // prevent navigation away

                override fun onPageFinished(view: WebView?, url: String?) {
                    pageLoaded = true
                }
            }
            clearCache(true)
            loadUrl("file:///android_asset/staff.html")
        }
    }

    // Clean up WebView when this composable leaves the composition.
    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    val mei = remember(gameState, startBeat, endBeat, showChordNames) {
        MeiConverter.convert(gameState, startBeat, endBeat, showChordNames)
    }

    val isPortraitRow = endBeat < Float.MAX_VALUE / 2f
    val showCursor = if (isPortraitRow) {
        gameState.currentBeat >= startBeat && gameState.currentBeat < endBeat
    } else true

    LaunchedEffect(pageLoaded, mei, showCursor) {
        if (!pageLoaded) return@LaunchedEffect
        val jsonMei = JSONObject.quote(mei)  // properly escapes the MEI string for JS
        val js = "renderMei($jsonMei, ${showCursor});"
        webView.evaluateJavascript(js, null)
        if (!firstRenderFired) {
            firstRenderFired = true
            onFirstRender?.invoke()
        }
    }

    AndroidView(
        factory = { webView },
        update = { /* state changes handled via LaunchedEffect */ },
        modifier = modifier
    )
}
