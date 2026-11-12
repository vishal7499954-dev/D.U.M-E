package com.dume.assistant

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Bundle
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
 * D.U.M-E offline foundation.
 *
 * This is deliberately a local command assistant, not a pretend LLM.
 * A local language model will be integrated in a later milestone.
 */
class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val background = Color.rgb(7, 17, 31)
    private val surface = Color.rgb(15, 31, 49)
    private val cyan = Color.rgb(103, 232, 249)
    private val white = Color.rgb(232, 242, 250)

    private lateinit var conversation: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var memory: android.content.SharedPreferences
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memory = getSharedPreferences("dume_local_memory", Context.MODE_PRIVATE)
        tts = TextToSpeech(this, this)
        buildInterface()
        addMessage("D.U.M-E", "Systems online. Offline foundation active.\nType 'help' to see available commands.")
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
            text = "LOCAL SYSTEM  •  OFFLINE MODE"
            textSize = 10f
            letterSpacing = 0.16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(18))
        }

        scroll = ScrollView(this)
        conversation = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(14), dp(12), dp(14))
        }
        scroll.addView(conversation)
        scroll.setBackgroundColor(surface)

        input = EditText(this).apply {
            hint = "Message D.U.M-E..."
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 16f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(14), 0, dp(14), 0)
            setBackgroundColor(surface)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    submitMessage()
                    true
                } else false
            }
        }

        val send = Button(this).apply {
            text = "SEND"
            setTextColor(background)
            setBackgroundColor(cyan)
            setOnClickListener { submitMessage() }
        }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(input, LinearLayout.LayoutParams(0, dp(54), 1f))
            addView(send, LinearLayout.LayoutParams(dp(88), dp(54)))
        }

        root.addView(heading)
        root.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(controls, LinearLayout.LayoutParams(-1, dp(58)))
        setContentView(root)
    }

    private fun submitMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        input.setText("")
        addMessage("YOU", message)
        saveHistory("YOU: $message")
        val reply = respond(message)
        addMessage("D.U.M-E", reply)
        saveHistory("D.U.M-E: $reply")
        if (ttsReady) tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "dume_reply")
    }

    private fun respond(message: String): String {
        val command = message.trim()
        val lower = command.lowercase(Locale.ROOT)

        return when {
            lower == "help" || lower == "commands" ->
                "Available commands:\n• time / date\n• battery\n• remember [note]\n• recall\n• forget memory\n• clear chat\n• status"

            lower == "time" || lower.contains("what time") ->
                "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + "."

            lower == "date" || lower.contains("today's date") ->
                "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + "."

            lower == "battery" || lower.contains("battery status") -> {
                val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
                "Battery level: ${manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%."
            }

            lower.startsWith("remember ") -> {
                val note = command.substringAfter("remember ").trim()
                if (note.isBlank()) "Specify what I should remember."
                else {
                    val notes = memory.getString("notes", "").orEmpty()
                    memory.edit().putString("notes", (notes + "\n" + note).trim()).apply()
                    "Saved to local memory."
                }
            }

            lower == "recall" || lower == "what do you remember" -> {
                val notes = memory.getString("notes", "").orEmpty()
                if (notes.isBlank()) "Local memory is empty."
                else "Stored notes:\n$notes"
            }

            lower == "forget memory" -> {
                memory.edit().remove("notes").apply()
                "Local memory cleared."
            }

            lower == "clear chat" -> {
                memory.edit().remove("history").apply()
                conversation.removeAllViews()
                "Conversation cleared. Local notes are preserved."
            }

            lower == "status" ->
                "D.U.M-E foundation online. Storage: local. Network permission: none. Generative model: not installed yet."

            lower in listOf("hello", "hi", "hey") ->
                "Hello. Systems are ready."

            lower.contains("who are you") ->
                "I am D.U.M-E, your offline Android assistant."

            else ->
                "I can handle local commands, but my language model is not installed yet. Try 'help'."
        }
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
