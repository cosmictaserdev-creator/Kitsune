// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.whispry

import android.app.PendingIntent
import android.content.*
import android.os.*
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.cosmictaser.whispry.bridge.IKeyboardBridge
import com.cosmictaser.whispry.bridge.IKeyboardBridgeCallback
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceState(
    val phase: String = "IDLE", val id: String = "", val message: String = "",
    val level: Float = 0f, val elapsed: Long = 0, val text: String = "",
    val resultId: String = "", val retry: Boolean = false, val microphone: Boolean = false,
    val error: String = "", val target: String = "",
) {
    val listening get() = phase == "LISTENING" || phase == "PAUSED"
    val busy get() = listening || phase in listOf("CONNECTING", "STARTING", "TRANSCRIBING", "FORMATTING")
    val visible get() = phase !in listOf("IDLE", "PREPARED", "CANCELLED", "COMPLETED")
    companion object {
        fun from(value: Bundle) = VoiceState(phase = value.getString("phase", "IDLE"),
            id = value.getString("sessionId", ""), message = value.getString("message", ""),
            level = value.getFloat("level"), elapsed = value.getLong("elapsed"), text = value.getString("text", ""),
            resultId = value.getString("resultId", ""), retry = value.getBoolean("retryAvailable"),
            microphone = value.getBoolean("microphoneGranted"), error = value.getString("error", ""),
            target = value.getString("targetPackage", ""))
    }
}

/** IME adapter only: Whispry owns recording, keys, providers, errors and history. */
class WhispryKeyboardController(private val ime: LatinIME) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(VoiceState())
    val state = _state.asStateFlow()
    private var api: IKeyboardBridge? = null
    private var binding = false
    private var shown = false
    private var startWhenConnected = false
    private var overlay: ComposeView? = null
    private var toolbar: View? = null
    private var hidden = false
    private var generation = 0L
    private var requestGeneration = -1L
    private var requestSession: String? = null
    private var lastResult: String? = null
    private val cancelledSessions = mutableSetOf<String>()
    private val delivered = ime.getSharedPreferences("whispry_delivery", Context.MODE_PRIVATE)

    private val callback = object : IKeyboardBridgeCallback.Stub() {
        override fun onState(bundle: Bundle) { scope.launch { accept(bundle) } }
    }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            api = IKeyboardBridge.Stub.asInterface(binder)
            scope.launch {
                try {
                    val snapshot = withContext(Dispatchers.IO) { api!!.connect(callback) }
                    accept(snapshot)
                    if (startWhenConnected) {
                        startWhenConnected = false
                        if (!_state.value.busy && _state.value.phase != "READY") begin()
                    }
                } catch (_: Exception) { error("Open Whispry and update both apps to connect.", "CONNECTION") }
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            api = null
            if (_state.value.busy) error("Whispry disconnected. Reopen the keyboard to reconnect.", "CONNECTION")
        }
        override fun onBindingDied(name: ComponentName) { release(); if (shown) connect() }
        override fun onNullBinding(name: ComponentName) { release(); error("Update Whispry to enable keyboard dictation.", "CONNECTION") }
    }

    fun attach(root: View) {
        overlay?.let { (it.parent as? ViewGroup)?.removeView(it); it.disposeComposition() }
        toolbar = root.findViewById(R.id.suggestion_strip_view)
        val host = root.findViewById<View>(R.id.strip_container) as? FrameLayout ?: return
        overlay = ComposeView(ime).apply {
            visibility = View.GONE
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { VoiceRow(this@WhispryKeyboardController) }
            host.addView(this, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        hidden = false
        renderVisibility()
    }

    fun inputStarted(info: EditorInfo?, restarting: Boolean) {
        if (!restarting) generation++
    }

    fun show() {
        shown = true
        if (requestSession != null || _state.value.visible) connect()
        renderVisibility(); deliver(false)
    }
    fun hide() { shown = false; release() }
    fun destroy() { release(); overlay?.disposeComposition(); scope.cancel() }

    private fun installed() = ime.packageManager.getLaunchIntentForPackage(APP) != null

    private fun connect() {
        if (binding || !installed()) return
        try {
            binding = ime.bindService(Intent().setComponent(ComponentName(APP, "$IMPLEMENTATION.KeyboardBridgeService")),
                connection, Context.BIND_AUTO_CREATE)
            if (!binding) error("Update Whispry to connect your keyboard.", "CONNECTION")
        } catch (_: SecurityException) { error("Install matching builds of Whispry and Whispry Keys.", "CONNECTION") }
        if (binding) scope.launch {
            delay(6000)
            if (api == null && startWhenConnected) { startWhenConnected = false; error("Whispry didn't respond. Open the app and try again.", "CONNECTION") }
        }
    }

    private fun release() {
        runCatching { api?.disconnect(callback) }
        api = null
        if (binding) runCatching { ime.unbindService(connection) }
        binding = false
    }

    fun micTap() {
        val info = ime.currentInputEditorInfo
        val variation = (info?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
        val kind = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS
        val password = kind == InputType.TYPE_CLASS_TEXT && variation in listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) ||
            kind == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        if (password) { error("Voice input isn't available in password fields.", "FIELD"); return }
        if (!installed()) { error("Install Whispry to use voice typing.", "MISSING"); return }
        if (_state.value.busy) return
        if (_state.value.phase == "READY") { deliver(true); return }
        requestGeneration = generation
        if (api == null) {
            startWhenConnected = true
            _state.value = VoiceState(phase = "CONNECTING", message = "Connecting to Whispry…")
            renderVisibility(); connect()
        } else begin()
    }

    private fun begin() {
        val service = api ?: return
        val target = ime.currentInputEditorInfo?.packageName.orEmpty()
        if (target.isBlank()) { error("Tap a text field before recording.", "FIELD"); return }
        scope.launch {
            try {
                val prepared = withContext(Dispatchers.IO) {
                    val start = service.prepareStart(target)
                    start to service.connect(callback)
                }
                val snapshot = VoiceState.from(prepared.second)
                requestSession = snapshot.id
                requestGeneration = generation
                _state.value = snapshot.copy(phase = "STARTING", message = "Starting Whispry…")
                renderVisibility()
                if (!snapshot.microphone) openRecordingScreen()
                else {
                    try { prepared.first.send() }
                    catch (_: Exception) { error("Open Whispry to start recording.", "FOREGROUND") }
                }
            } catch (_: Exception) { error("Couldn't start. Open Whispry and try again.", "CONNECTION") }
        }
    }

    private fun accept(bundle: Bundle) {
        if (bundle.getInt("protocol") != 1) { error("Update both apps to use voice typing.", "CONNECTION"); return }
        val incoming = VoiceState.from(bundle)
        if (incoming.id in cancelledSessions) return
        if (incoming.phase == "PREPARED" && _state.value.phase == "STARTING" && incoming.id == requestSession) return
        _state.value = incoming
        renderVisibility()
        deliver(false)
    }

    private fun renderVisibility() {
        val visible = _state.value.visible
        overlay?.visibility = if (visible) View.VISIBLE else View.GONE
        if (hidden == visible) return
        hidden = visible
        toolbar?.let { view ->
            view.isEnabled = !visible
            view.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            val targetX = if (visible) view.width.toFloat() else 0f
            SpringAnimation(view, SpringAnimation.TRANSLATION_X, targetX).apply {
                spring = SpringForce(targetX).setDampingRatio(.85f).setStiffness(400f)
            }.start()
            view.animate().alpha(if (visible) 0f else 1f).translationY(if (visible) -8f else 0f).setDuration(180).start()
        }
    }

    fun pause() = command("PAUSE")
    fun cancel() {
        startWhenConnected = false
        cancelledSessions.add(_state.value.id)
        if (_state.value.id.isNotBlank()) command("CANCEL")
        _state.value = VoiceState(); renderVisibility()
    }
    fun finish() { if (_state.value.listening) command("FINISH") else if (_state.value.phase == "READY") deliver(true) }
    fun retry() {
        val service = api ?: return
        scope.launch {
            runCatching {
                val start = withContext(Dispatchers.IO) { service.prepareRetry(_state.value.id) }
                start.send()
            }.onFailure { error("Couldn't retry. Open Whispry to recover your recording.", "CONNECTION") }
        }
    }
    fun consumeEnter(): Boolean {
        if (!_state.value.busy && _state.value.phase != "READY") return false
        finish(); return true
    }

    private fun command(action: String) {
        val id = _state.value.id
        val service = api ?: return
        scope.launch {
            try { withContext(Dispatchers.IO) { service.command(id, action) } }
            catch (_: Exception) { error("Whispry disconnected. Open the app to recover your recording.", "CONNECTION") }
        }
    }

    private fun deliver(explicit: Boolean) {
        val result = _state.value
        if (result.phase != "READY" || result.resultId.isBlank() || result.text.isBlank() || !shown) return
        if (lastResult == result.resultId || delivered.getString("lastResult", null) == result.resultId) {
            if (copyResult(result)) acknowledge(result)
            else _state.value = result.copy(message = "Inserted. Tap Insert to retry copying.", error = "COPY")
            return
        }
        val sameEditor = requestSession == result.id && generation == requestGeneration &&
            ime.currentInputEditorInfo?.packageName == result.target
        if (!explicit && !sameEditor) return
        if (ime.currentInputConnection == null) return
        // Normal keyboard text transaction updates composition and suggestions as well as the field.
        try {
            ime.onTextInput(result.text)
            lastResult = result.resultId
            delivered.edit().putString("lastResult", result.resultId).apply()
        } catch (_: Exception) { error("Transcript saved. Tap Insert to try again.", "INSERT"); return }
        val copied = copyResult(result)
        if (copied) acknowledge(result)
        else _state.value = result.copy(message = "Inserted. Tap Copy to copy your transcript.", error = "COPY")
    }

    private fun copyResult(result: VoiceState): Boolean = runCatching {
        val clipboard = ime.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Whispry transcript", result.text))
    }.isSuccess

    private fun acknowledge(result: VoiceState) {
        val service = api ?: return
        scope.launch { runCatching { withContext(Dispatchers.IO) { service.acknowledge(result.id, result.resultId) } } }
    }

    fun openSettings() {
        if (!installed()) {
            val web = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$APP")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { ime.startActivity(web) }; return
        }
        if (_state.value.error == "FOREGROUND" || !_state.value.microphone) { openRecordingScreen(); return }
        val settings = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("whispry://settings")).setPackage(APP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ime.startActivity(settings) }
    }

    private fun openRecordingScreen() {
        runCatching { ime.startActivity(Intent().setComponent(ComponentName(APP, "$IMPLEMENTATION.KeyboardRecordingActivity"))
            .putExtra("targetPackage", ime.currentInputEditorInfo?.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { error("Update Whispry to start voice typing.", "CONNECTION") }
    }

    private fun error(message: String, code: String) {
        _state.value = _state.value.copy(phase = "ERROR", message = message, error = code)
        renderVisibility()
    }
    companion object {
        val APP: String get() = helium314.keyboard.latin.BuildConfig.WHISPRY_PACKAGE
        private const val IMPLEMENTATION = "com.example.whispry.bridge"
    }
}
