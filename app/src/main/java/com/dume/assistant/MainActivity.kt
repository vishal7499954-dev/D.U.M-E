package com.dume.assistant

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * D.U.M-E offline-first assistant.
 * Voice recognition prefers on-device/offline recognition when the phone's
 * installed speech service supports it. Availability depends on device language packs.
 */
class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val background = Color.rgb(7, 17, 31)
    private val surface = Color.rgb(15, 31, 49)
    private val cyan = Color.rgb(103, 232, 249)
    private val white = Color.rgb(232, 242, 250)
    private val requestAudio = 41
    private val requestVoice = 42

    private lateinit var conversation: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var memory: android.content.SharedPreferences
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var waitingForSendConfirmation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memory = getSharedPreferences("dume_local_memory", Context.MODE_PRIVATE)
        tts = TextToSpeech(this, this)
        buildInterface()
        addMessage("D.U.M-E", "Systems online. Offline foundation active.\nTap VOICE to speak. Phone control needs your permission in Android Settings.")
        restoreHistory()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(14))
            setBackgroundColor(background)
        }
        val heading = TextView(this).apply {
            text = "D.U.M-E"
            textSize = 30f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(cyan)
            gravity = Gravity.CENTER
        }
        val status = TextView(this).apply {
            text = "LOCAL SYSTEM  •  OFFLINE-FIRST MODE"
            textSize = 10f
            letterSpacing = 0.16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(14))
        }

        scroll = ScrollView(this)
        conversation = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(14), dp(12), dp(14))
        }
        scroll.addView(conversation)
        scroll.setBackgroundColor(surface)

        input = EditText(this).apply {
            hint = "Speak or type a command..."
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 16f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(14), 0, dp(14), 0)
            setBackgroundColor(surface)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) { submitMessage(); true } else false
            }
        }

        val send = Button(this).apply {
            text = "SEND"
            setTextColor(background)
            setBackgroundColor(cyan)
            setOnClickListener { submitMessage() }
        }
        val mic = Button(this).apply {
            text = "VOICE"
            setOnClickListener { startVoiceInput() }
        }
        val control = Button(this).apply {
            text = "ENABLE CONTROL"
            setOnClickListener { openAccessibilitySettings() }
        }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(input, LinearLayout.LayoutParams(0, dp(54), 1f))
            addView(send, LinearLayout.LayoutParams(dp(76), dp(54)))
        }
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(mic, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(control, LinearLayout.LayoutParams(0, dp(48), 1f))
        }

        root.addView(heading)
        root.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(controls, LinearLayout.LayoutParams(-1, dp(58)))
        root.addView(actionRow, LinearLayout.LayoutParams(-1, dp(50)))
        setContentView(root)
    }

    private fun startVoiceInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestAudio)
            return
        }
        launchRecognizer()
    }

    private fun launchRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to D.U.M-E")
        }
        try {
            startActivityForResult(intent, requestVoice)
        } catch (_: Exception) {
            reply("No speech recognition app is available. Install an offline speech pack or use text commands.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestAudio && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            launchRecognizer()
        } else if (requestCode == requestAudio) {
            reply("Microphone permission was not granted.")
        }
    }

    @Deprecated("Activity result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestVoice && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                input.setText(spoken)
                submitMessage()
            }
        }
    }

    private fun openAccessibilitySettings() {
        addMessage("D.U.M-E", "Enable D.U.M-E Control Service on the next screen. Android will show a system warning because this permission can interact with apps.")
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun submitMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        input.setText("")
        addMessage("YOU", message)
        saveHistory("YOU: $message")
        val response = handleCommand(message)
        reply(response)
    }

    private fun handleCommand(message: String): String {
        val command = message.trim()
        val lower = command.lowercase(Locale.ROOT)
        val service = DumeAccessibilityService.activeService

        if (waitingForSendConfirmation) {
            if (lower == "repeat my message for me" || lower == "repeat my message" || lower == "read my message") {
                val draft = service?.readFocusedFieldText()
                return if (draft.isNullOrBlank()) "I can't access the message text in this app."
                       else "Your message says: $draft"
            }
            if (lower == "confirm send" || lower == "yes send") {
                waitingForSendConfirmation = false
                val success = service?.clickVisibleLabel("send", allowSensitive = true) == true
                return if (success) "Confirmed. I tapped the visible Send control."
                       else "I couldn't find or tap a visible Send control. Nothing was sent by D.U.M-E."
            }
            if (lower in listOf("cancel", "cancel send", "no")) {
                waitingForSendConfirmation = false
                return "Cancelled. I did not send the message."
            }
            return "A message is awaiting confirmation. Say 'confirm send' to send it, or 'cancel' to stop."
        }

        when {
            lower == "help" || lower == "commands" ->
                return "Try: time, date, battery, remember [note], recall, open WhatsApp, scroll down/up, tap [visible label], type [text], go back, go home, send message, confirm send, cancel."

            lower == "time" || lower.contains("what time") ->
                return "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + "."

            lower == "date" || lower.contains("today's date") ->
                return "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + "."

            lower == "battery" || lower.contains("battery status") -> {
                val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
                return "Battery level: ${manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%."
            }

            lower.startsWith("remember ") -> {
                val note = command.substringAfter("remember ").trim()
                val notes = memory.getString("notes", "").orEmpty()
                memory.edit().putString("notes", (notes + "\n" + note).trim()).apply()
                return "Saved to local memory."
            }

            lower == "recall" || lower == "what do you remember" -> {
                val notes = memory.getString("notes", "").orEmpty()
                return if (notes.isBlank()) "Local memory is empty." else "Stored notes:\n$notes"
            }

            lower == "forget memory" -> {
                memory.edit().remove("notes").apply()
                return "Local memory cleared."
            }

            lower == "clear chat" -> {
                memory.edit().remove("history").apply()
                conversation.removeAllViews()
                return "Conversation cleared. Local notes are preserved."
            }

            lower == "status" ->
                return "D.U.M-E foundation online. Local storage active. Internet permission: none. Voice recognition prefers offline mode; offline availability depends on your phone's speech service. Local generative model is not installed yet."

            lower in listOf("hello", "hi", "hey") ->
                return "Hello. Systems are ready."

            lower.contains("who are you") ->
                return "I am D.U.M-E, your offline Android assistant."

            lower == "repeat my message for me" || lower == "repeat my message" || lower == "read my message" -> {
                val draft = service?.readFocusedFieldText()
                return if (draft.isNullOrBlank()) "I can't access the message text in this app."
                       else "Your message says: $draft"
            }

            lower == "send" || lower == "send message" -> {
                waitingForSendConfirmation = true
                return "Ready to send. I will not read the message aloud unless you ask me to. Say 'confirm send' to send, or 'cancel' to stop."
            }

            lower == "confirm send" || lower == "yes send" ->
                return "No message is waiting for confirmation. Say 'send message' first when you are ready."

            lower in listOf("cancel", "cancel send") -> {
                waitingForSendConfirmation = false
                return "Cancelled."
            }

            lower == "scroll down" || lower == "scroll up" -> {
                if (service == null) return "Phone control is not enabled. Tap ENABLE CONTROL and turn on D.U.M-E Control Service."
                val ok = service.scroll(lower == "scroll down")
                return if (ok) "Scrolling." else "I couldn't scroll this screen."
            }

            lower == "go back" || lower == "back" -> {
                val ok = service?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK) == true
                return if (ok) "Going back." else "Phone control is not enabled."
            }

            lower == "go home" || lower == "home" -> {
                val ok = service?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME) == true
                return if (ok) "Going to Home." else "Phone control is not enabled."
            }

            lower.startsWith("tap ") || lower.startsWith("click ") -> {
                if (service == null) return "Phone control is not enabled. Tap ENABLE CONTROL first."
                val label = command.substringAfter(" ").trim()
                val ok = service.clickVisibleLabel(label)
                return if (ok) "Tapped '$label'." else "I couldn't tap '$label'. If it is a sensitive control such as Send or Pay, it needs a separate confirmation."
            }

            lower.startsWith("type ") -> {
                if (service == null) return "Phone control is not enabled. Tap ENABLE CONTROL first."
                val textToType = command.substringAfter("type ", "").trim()
                val ok = service.typeIntoFocusedField(textToType)
                return if (ok) "Text entered." else "I couldn't type. Tap a text field first."
            }

            lower.startsWith("open ") -> {
                val appName = command.substringAfter("open ").trim()
                val launch = findLauncherIntent(appName)
                if (launch != null) {
                    startActivity(launch)
                    return "Opening $appName."
                }
                return "I couldn't find an installed app named $appName."
            }
        }

        return "I don't know that command yet. Say 'help' for available commands."
    }

    private fun findLauncherIntent(name: String): Intent? {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        val match = apps.firstOrNull {
            val label = pm.getApplicationLabel(it).toString()
            label.equals(name, ignoreCase = true) || label.contains(name, ignoreCase = true)
        } ?: return null
        return pm.getLaunchIntentForPackage(match.packageName)
    }

    private fun reply(text: String) {
        addMessage("D.U.M-E", text)
        saveHistory("D.U.M-E: $text")
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dume_reply")
    }

    private fun addMessage(sender: String, message: String) {
        val label = TextView(this).apply {
            text = sender
            textSize = 10f
            letterSpacing = 0.12f
            setTextColor(cyan)
            typeface = Typeface.DEFAULT_BOLD
        }
        val body = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(white)
            setPadding(0, dp(4), 0, dp(12))
        }
        conversation.addView(label)
        conversation.addView(body)
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun saveHistory(line: String) {
        val old = memory.getString("history", "").orEmpty()
        val lines = (old.lines() + line).takeLast(80)
        memory.edit().putString("history", lines.joinToString("\n")).apply()
    }

    private fun restoreHistory() {
        val lines = memory.getString("history", "").orEmpty()
        if (lines.isNotBlank()) {
            conversation.removeAllViews()
            lines.lines().forEach { line ->
                val split = line.indexOf(": ")
                if (split > 0) addMessage(line.substring(0, split), line.substring(split + 2))
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
