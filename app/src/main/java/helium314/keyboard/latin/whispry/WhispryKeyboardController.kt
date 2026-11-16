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

data class ToolsState(
    val ids: List<String>, val names: List<String>, val emojis: List<String>, val groups: List<String>,
    val selected: String, val mode: String, val translate: Boolean, val translateLanguage: String,
) {
    companion object {
        fun from(b: Bundle) = ToolsState(b.getStringArrayList("ids").orEmpty(), b.getStringArrayList("names").orEmpty(),
            b.getStringArrayList("emojis").orEmpty(), b.getStringArrayList("groups").orEmpty(), b.getString("selected", "NONE"),
            b.getString("mode", "SESSION"), b.getBoolean("translate"), b.getString("translateLanguage", "English"))
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
    private var panel: ComposeView? = null
    private var keys: View? = null
    private val _panelOpen = MutableStateFlow(false)
    val panelOpen = _panelOpen.asStateFlow()
    private val _tools = MutableStateFlow<ToolsState?>(null)
    val tools = _tools.asStateFlow()
    private val _toolsMessage = MutableStateFlow("Connecting to Whispry…")
    val toolsMessage = _toolsMessage.asStateFlow()
    private val tapHandler = Handler(Looper.getMainLooper())
    private var pendingTap: Runnable? = null
    private var toolbar: View? = null
    private var hidden = false
    private var generation = 0L
    private var requestGeneration = -1L
    private var requestSession: String? = null
    private var lastResult: String? = null
    private val cancelledSessions = object : LinkedHashSet<String>() {
        fun remember(id: String) { add(id); while (size > 16) remove(first()) }
    }
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
                    if (_panelOpen.value) loadTools()
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
        // The tools panel sits over the lower keyboard area (below the strip) and swallows touches.
        panel?.let { (it.parent as? ViewGroup)?.removeView(it); it.disposeComposition() }
        keys = root.findViewById(R.id.keyboard_view_wrapper)
        val input = root as? FrameLayout
        panel = if (input == null) null else ComposeView(ime).apply {
            visibility = View.GONE
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { ToolsPanel(this@WhispryKeyboardController) }
            input.addView(this, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        renderPanel()
    }

    /** The panel replaces the key area: same bounds, keys hidden (not removed, so the keyboard keeps its measured height). */
    private fun renderPanel() {
        val view = panel ?: return
        val open = _panelOpen.value
        val wrapper = keys
        if (open && wrapper != null) {
            var top = 0
            var parent: View? = wrapper
            while (parent != null && parent !== view.parent) { top += parent.top; parent = parent.parent as? View }
            (view.layoutParams as? FrameLayout.LayoutParams)?.let {
                it.gravity = android.view.Gravity.TOP
                it.topMargin = top
                it.height = wrapper.height.coerceAtLeast(1)
                view.layoutParams = it
            }
        }
        view.visibility = if (open) View.VISIBLE else View.GONE
        wrapper?.let { k ->
            k.visibility = if (open) View.INVISIBLE else View.VISIBLE
            k.importantForAccessibility = if (open) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        }
    }

    fun togglePanel() { if (_panelOpen.value) closePanel() else openPanel() }
    fun openPanel() {
        if (Build.VERSION.SDK_INT < 26) { error("Voice tools need Android 8 or later.", "SDK"); return }
        if (!installed()) { error("Install Whispry to use voice tools.", "MISSING"); return }
        _panelOpen.value = true; renderPanel(); loadTools()
    }
    fun closePanel() { _panelOpen.value = false; renderPanel() }

    private fun loadTools() {
        val service = api
        if (service == null) { _toolsMessage.value = "Connecting to Whispry…"; connect(); return }
        scope.launch {
            try {
                val bundle = withContext(Dispatchers.IO) { service.tools() }
                _tools.value = ToolsState.from(bundle)
            } catch (_: Exception) { _toolsMessage.value = "Update Whispry to use tones."; _tools.value = null }
        }
    }

    fun selectTone(id: String) {
        val service = api ?: return
        _tools.value = _tools.value?.copy(selected = id, translate = false)
        scope.launch { runCatching { withContext(Dispatchers.IO) { service.setTranslation(false, ""); service.selectTone(id) } } }
    }

    fun setTranslation(enabled: Boolean, language: String) {
        val service = api ?: return
        _tools.value = _tools.value?.copy(translate = enabled, translateLanguage = language.ifBlank { _tools.value?.translateLanguage ?: "English" })
        scope.launch { runCatching { withContext(Dispatchers.IO) { service.setTranslation(enabled, language) } } }
    }

    fun openFeature(host: String) {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("whispry://$host")).setPackage(APP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ime.startActivity(intent) }.onFailure { error("Update Whispry to open this tool.", "CONNECTION") }
    }

    fun inputStarted(info: EditorInfo?, restarting: Boolean) {
        if (!restarting) generation++
    }

    fun show() {
        shown = true
        if (requestSession != null || _state.value.visible) connect()
        renderVisibility(); deliver(false)
    }
    fun hide() { shown = false; closePanel(); release() }
    fun destroy() { release(); overlay?.disposeComposition(); panel?.disposeComposition(); scope.cancel() }

    private fun installed() = ime.packageManager.getLaunchIntentForPackage(APP) != null

    private fun connect() {
        if (binding || !installed()) return
        try {
            binding = ime.bindService(Intent().setComponent(ComponentName(APP, "$IMPLEMENTATION.KeyboardBridgeService")),
                connection, Context.BIND_AUTO_CREATE)
            if (!binding) error("Update Whispry to connect your keyboard.", "CONNECTION")
        } catch (_: SecurityException) { error("Install matching builds of Whispry and Kitsune.", "CONNECTION") }
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

    /** One tap starts dictation once the double-tap window has passed; a second tap opens/closes the tools panel. */
    fun micTap() {
        pendingTap?.let {
            tapHandler.removeCallbacks(it); pendingTap = null
            togglePanel(); return
        }
        val run = Runnable { pendingTap = null; singleMicTap() }
        pendingTap = run
        tapHandler.postDelayed(run, android.view.ViewConfiguration.getDoubleTapTimeout().toLong())
    }

    private fun singleMicTap() {
        if (Build.VERSION.SDK_INT < 26) { error("Voice typing needs Android 8 or later.", "SDK"); return }
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
        if (_state.value.phase == "ERROR" && _state.value.retry) { retry(); return }
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
        val frozenGeneration = generation // editor identity is frozen with the target, before any async work
        scope.launch {
            try {
                val prepared = withContext(Dispatchers.IO) {
                    val start = service.prepareStart(target)
                    start to service.connect(callback)
                }
                val snapshot = VoiceState.from(prepared.second)
                if (frozenGeneration != generation) {
                    // The user moved to another field while Whispry was preparing: drop the session.
                    cancelledSessions.remember(snapshot.id)
                    if (snapshot.id.isNotBlank()) command(snapshot.id, "CANCEL")
                    _state.value = VoiceState(); renderVisibility()
                    return@launch
                }
                requestSession = snapshot.id
                requestGeneration = frozenGeneration
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
        val voice = overlay
        if (voice != null) {
            if (visible && voice.visibility != View.VISIBLE) {
                // The voice row rolls up into place from below, like the next face of a drum.
                voice.visibility = View.VISIBLE
                prepareRoll(voice)
                voice.rotationX = -90f; voice.translationY = voice.height * 0.5f; voice.alpha = 0f
                rollTo(voice, 0f, 0f, 1f)
            } else if (!visible && voice.visibility == View.VISIBLE) {
                rollTo(voice, -90f, voice.height * 0.5f, 0f) { voice.visibility = View.GONE }
            }
        }
        if (hidden == visible) return
        hidden = visible
        toolbar?.let { view ->
            view.isEnabled = !visible
            view.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            // The toolbar rolls away downwards (its top edge tips back and it drops out), keeping its height so the host doesn't jump.
            prepareRoll(view)
            if (visible) rollTo(view, 90f, view.height * 0.5f, 0f) else rollTo(view, 0f, 0f, 1f)
        }
    }

    private fun prepareRoll(view: View) {
        view.pivotX = view.width / 2f
        view.pivotY = view.height / 2f
        view.cameraDistance = 12000f * view.resources.displayMetrics.density
    }

    /** Spring-driven roll (rotationX + drop + fade). Animations are skipped entirely when the user disabled them. */
    private fun rollTo(view: View, rotation: Float, dropY: Float, alpha: Float, end: (() -> Unit)? = null) {
        val scale = android.provider.Settings.Global.getFloat(ime.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) { view.rotationX = rotation; view.translationY = dropY; view.alpha = alpha; end?.invoke(); return }
        fun spring(property: androidx.dynamicanimation.animation.DynamicAnimation.ViewProperty, target: Float) =
            SpringAnimation(view, property, target).apply { spring = SpringForce(target).setDampingRatio(.8f).setStiffness(260f) }
        spring(SpringAnimation.ROTATION_X, rotation).start()
        spring(SpringAnimation.TRANSLATION_Y, dropY).start()
        spring(SpringAnimation.ALPHA, alpha).apply { addEndListener { _, _, _, _ -> end?.invoke() } }.start()
    }

    fun pause() = command("PAUSE")
    fun cancel() {
        startWhenConnected = false
        cancelledSessions.remember(_state.value.id)
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

    private fun command(action: String, id: String = _state.value.id) {
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
        } catch (_: Exception) { _state.value = result.copy(message = "Transcript saved. Tap send to try again.", error = "INSERT"); return }
        // onTextInput has no success flag, so confirm the host editor actually received the text before
        // recording it as delivered. A null read means the editor can't be inspected; that is not a failure.
        if (!editorContainsTail(result.text)) { _state.value = result.copy(message = "The app didn't accept the text. Tap send to try again.", error = "INSERT"); return }
        lastResult = result.resultId
        delivered.edit().putString("lastResult", result.resultId).apply()
        val copied = copyResult(result)
        if (copied) acknowledge(result)
        else _state.value = result.copy(message = "Inserted. Tap Copy to copy your transcript.", error = "COPY")
    }

    private fun editorContainsTail(text: String): Boolean {
        val tail = text.trimEnd().takeLast(40)
        if (tail.isEmpty()) return true
        val before = ime.currentInputConnection?.getTextBeforeCursor(tail.length + 16, 0) ?: return ime.currentInputConnection != null
        return before.toString().contains(tail)
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
