package io.github.rednick.newsswitcher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.window.OnBackInvokedDispatcher
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * The whole app: a full-screen WebView showing the hourly news switcher web
 * page. Loading the live page (rather than a copy bundled in the app) means
 * channel and feature changes made to the website show up here straight away,
 * and gives YouTube a real web address, which its player needs.
 *
 * On top of the page, this adds what a phone or TV app needs: no screen
 * timeout, no status bar, sound on from the start, and TV remote buttons.
 */
class MainActivity : Activity() {

    private companion object {
        const val SWITCHER_PAGE_URL = "https://rednick.github.io/news-switcher/"

        // Browsers make the page start muted. An app is allowed to play sound
        // straight away, so turn it on for the viewer.
        const val START_WITH_SOUND = true

        // Back closes any open menu first. Pressing it twice in this time exits.
        const val BACK_AGAIN_TO_EXIT_MILLISECONDS = 2_000L

        // How long to wait before trying again when the page can't be reached.
        const val RETRY_DELAY_MILLISECONDS = 15_000L

        const val TV_REMOTE_SCRIPT_FILE = "tv-remote.js"
    }

    private lateinit var webView: WebView
    private lateinit var tvRemoteScript: String
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var lastBackPressTime = 0L
    private val retryLoadingPage = Runnable { webView.loadUrl(SWITCHER_PAGE_URL) }

    @SuppressLint("SetJavaScriptEnabled") // The page is our own site and needs JavaScript.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // News runs for hours with nobody touching the screen.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tvRemoteScript = assets.open(TV_REMOTE_SCRIPT_FILE).bufferedReader().use { it.readText() }

        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true // The page saves its settings in localStorage.
            settings.mediaPlaybackRequiresUserGesture = false // Lets streams play, with sound, without a tap.
            webViewClient = SwitcherPageClient()
            webChromeClient = WebChromeClient()
        }
        setContentView(webView)
        hideSystemBars()
        listenForBackOnNewerAndroid()

        webView.loadUrl(SWITCHER_PAGE_URL)
    }

    // --- Full screen ---------------------------------------------------------

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            // On phones, a swipe from the edge shows the bars for a moment.
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Android can bring the bars back, e.g. after a dialog or the keyboard.
        if (hasFocus) {
            hideSystemBars()
        }
    }

    // --- TV remote -----------------------------------------------------------

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val pageAction = getPageActionForKey(event.keyCode)
            ?: return super.dispatchKeyEvent(event)

        // Act once per press, and don't let the WebView see the key as well.
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            runOnPage(pageAction)
        }
        return true
    }

    /** The JavaScript to run for a remote button, or null to leave the key alone. */
    private fun getPageActionForKey(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_CHANNEL_DOWN,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "newsSwitcherRemote.pressKey('ArrowLeft')"

        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_CHANNEL_UP,
        KeyEvent.KEYCODE_MEDIA_NEXT -> "newsSwitcherRemote.pressKey('ArrowRight')"

        KeyEvent.KEYCODE_DPAD_UP -> "newsSwitcherRemote.moveFocus(-1)"
        KeyEvent.KEYCODE_DPAD_DOWN -> "newsSwitcherRemote.moveFocus(1)"

        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> "newsSwitcherRemote.pressSelect()"

        KeyEvent.KEYCODE_MENU -> "newsSwitcherRemote.pressKey('m')"
        KeyEvent.KEYCODE_INFO -> "newsSwitcherRemote.pressKey('h')"

        else -> null
    }

    /** Runs a remote action, but only once the remote script is on the page. */
    private fun runOnPage(pageAction: String) {
        webView.evaluateJavascript("if (window.newsSwitcherRemote) { $pageAction; }", null)
    }

    // Android 13 and later send Back here instead of to onBackPressed, and from
    // Android 16 onBackPressed isn't called at all for this app.
    private fun listenForBackOnNewerAndroid() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) { handleBackPress() }
        }
    }

    @Deprecated("Only called on Android 12 and older. Newer versions use listenForBackOnNewerAndroid.")
    override fun onBackPressed() {
        handleBackPress()
    }

    private fun handleBackPress() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < BACK_AGAIN_TO_EXIT_MILLISECONDS) {
            finish()
            return
        }
        lastBackPressTime = now
        runOnPage("newsSwitcherRemote.pressKey('Escape')") // Closes the menu if it's open.
        Toast.makeText(this, R.string.press_back_again_to_exit, Toast.LENGTH_SHORT).show()
    }

    // --- Page loading --------------------------------------------------------

    private inner class SwitcherPageClient : WebViewClient() {

        override fun onPageFinished(view: WebView, url: String) {
            if (!url.startsWith(SWITCHER_PAGE_URL)) {
                return // The "can't connect" message, not the switcher.
            }
            view.evaluateJavascript(tvRemoteScript, null)
            if (START_WITH_SOUND) {
                runOnPage("newsSwitcherRemote.turnSoundOn()")
            }
        }

        // Keeps the app on the switcher. A link to anywhere else (like
        // tapping the YouTube logo) opens in the phone's browser instead.
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame || request.url.toString().startsWith(SWITCHER_PAGE_URL)) {
                return false
            }
            try {
                startActivity(Intent(Intent.ACTION_VIEW, request.url))
            } catch (noBrowserInstalled: ActivityNotFoundException) {
                // Common on TVs. Just stay on the switcher.
            }
            return true
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            // Only the page itself failing matters. A failed image or stream
            // is handled by the page, which skips to the next channel.
            if (!request.isForMainFrame) {
                return
            }
            showCannotConnectMessage(view)
            mainThreadHandler.removeCallbacks(retryLoadingPage)
            mainThreadHandler.postDelayed(retryLoadingPage, RETRY_DELAY_MILLISECONDS)
        }
    }

    private fun showCannotConnectMessage(view: WebView) {
        val messagePage = """
            <html><body style="background:#000;color:#fff;font-family:sans-serif;
                display:flex;align-items:center;justify-content:center;height:100vh;margin:0;
                text-align:center">
              <div>
                <h2>Can't reach the news switcher</h2>
                <p>Check the internet connection. Trying again in ${RETRY_DELAY_MILLISECONDS / 1000} seconds…</p>
              </div>
            </body></html>
        """.trimIndent()
        view.loadDataWithBaseURL(null, messagePage, "text/html", "utf-8", null)
    }

    // --- Lifecycle -----------------------------------------------------------

    // Unloads the page when the app goes into the background (e.g. Home on
    // the TV remote), so the sound definitely stops, then loads it again on
    // return. The schedule comes from the clock, so it picks up the right
    // channel for the current hour.
    override fun onStop() {
        super.onStop()
        mainThreadHandler.removeCallbacks(retryLoadingPage)
        webView.loadUrl("about:blank")
    }

    override fun onRestart() {
        super.onRestart()
        webView.loadUrl(SWITCHER_PAGE_URL)
    }

    override fun onDestroy() {
        mainThreadHandler.removeCallbacks(retryLoadingPage)
        webView.destroy()
        super.onDestroy()
    }
}
