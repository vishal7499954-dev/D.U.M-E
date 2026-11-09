package com.personal.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.content.Context
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var output: TextView
    private lateinit var input: EditText
    private lateinit var memory: android.content.SharedPreferences
    private var torchOn = false
    private val cyan = Color.rgb(53, 201, 255)
    private val bg = Color.rgb(5, 13, 26)
    private val panel = Color.rgb(10, 29, 49)
    private val voiceRequest = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memory = getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE)
        tts = TextToSpeech(this, this)
        buildUi()
        show("JARVIS systems online. I am your local assistant. Try: help, battery, flashlight on, remember ..., recall, open settings, or search ...")
    }

    private fun buildUi() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 34, 22, 22)
            setBackgroundColor(bg)
        }
        val title = TextView(this).apply {
            text = "J.A.R.V.I.S."
            textSize = 29f; setTextColor(cyan); gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "PERSONAL ANDROID ASSISTANT  •  LOCAL MODE"
            textSize = 10f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
        }
        val orb = TextView(this).apply {
            text = "◉"
            textSize = 76f; setTextColor(cyan); gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
        }
        output = TextView(this).apply {
            textSize = 15f; setTextColor(Color.rgb(220, 241, 255))
            setPadding(16, 18, 16, 18); setBackgroundColor(panel)
        }
        input = EditText(this).apply {
            hint = "Ask JARVIS or enter a command..."
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            setSingleLine(false); setBackgroundColor(panel)
        }
        val send = Button(this).apply { text = "EXECUTE COMMAND"; setOnClickListener { handle(input.text.toString()); input.setText("") } }
        val mic = Button(this).apply { text = "🎙  TALK TO JARVIS"; setOnClickListener { listen() } }
        val online = Button(this).apply { text = "OPTIONAL ONLINE SEARCH"; setOnClickListener {
            val q = input.text.toString().trim()
            if (q.isNotEmpty()) try { startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", q)) }
            catch (_: Exception) { show("No web search app is available.") }
            else show("Type a search query first.")
        } }
        layout.addView(title); layout.addView(subtitle); layout.addView(orb)
        layout.addView(output, LinearLayout.LayoutParams(-1, 0, 1f))
        layout.addView(input, LinearLayout.LayoutParams(-1, 56))
        layout.addView(send); layout.addView(mic); layout.addView(online)
        setContentView(layout)
    }

    private fun show(text: String) {
        output.text = "JARVIS  /  ${text}"
        if (::tts.isInitialized && ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }
    private var ttsReady = false
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            ttsReady = true
        }
    }

    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try { startActivityForResult(i, voiceRequest) }
        catch (_: Exception) { show("Speech recognition is unavailable. You can type commands instead.") }
    }
    @Deprecated("Legacy activity result API for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == voiceRequest && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!words.isNullOrBlank()) handle(words)
        }
    }

    private fun handle(raw: String) {
        val cmd = raw.trim()
        val lower = cmd.lowercase(Locale.ROOT)
        if (cmd.isBlank()) { show("Please give me a command."); return }
        when {
            lower == "help" || lower.contains("what can you do") ->
                show("I can answer simple offline commands, save and recall notes, report battery level, toggle the flashlight, open Settings, and launch optional web search. A full local language model is not yet bundled in this starter build.")
            lower == "battery" || lower.contains("battery status") -> {
                val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                show("Battery level is ${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)} percent.")
            }
            lower.startsWith("remember ") -> {
                val note = cmd.substringAfter("remember ", "").trim()
                if (note.isNotEmpty()) {
                    val old = memory.getString("notes", "") ?: ""
                    memory.edit().putString("notes", (old + "\n" + note).trim()).apply()
                    show("Saved that to local memory.")
                } else show("Tell me what you want me to remember.")
            }
            lower == "recall" || lower.contains("what do you remember") -> {
                val notes = memory.getString("notes", "") ?: ""
                show(if (notes.isBlank()) "Your memory is empty. Say remember followed by a note." else "Here is what I remember: $notes")
            }
            lower.contains("forget memory") -> {
                memory.edit().remove("notes").apply()
                show("Local notes cleared.")
            }
            lower.contains("flashlight on") || lower.contains("torch on") -> setTorch(true)
            lower.contains("flashlight off") || lower.contains("torch off") -> setTorch(false)
            lower == "open settings" || lower.contains("phone settings") -> {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                show("Opening Android Settings.")
            }
            lower.startsWith("open ") -> openApp(cmd.substringAfter("open ").trim())
            lower.startsWith("search ") -> {
                val q = cmd.substringAfter("search ").trim()
                try { startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", q)); show("Opening online search.") }
                catch (_: Exception) { show("Online search is unavailable on this device.") }
            }
            else -> show("I heard: $cmd. I am currently running in lightweight command mode. Add a local language model to enable open-ended AI conversations.")
        }
    }

    private fun openApp(name: String) {
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val match = packages.firstOrNull { packageManager.getApplicationLabel(it).toString().equals(name, true) }
        if (match == null) { show("I couldn't find an installed app named $name."); return }
        val launch = packageManager.getLaunchIntentForPackage(match.packageName)
        if (launch != null) { startActivity(launch); show("Opening $name.") }
        else show("That app does not provide a launch screen.")
    }

    private fun setTorch(enabled: Boolean) {
        try {
            val cm = getSystemService(CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
            if (id == null) { show("This device has no available camera flashlight."); return }
            cm.setTorchMode(id, enabled); torchOn = enabled
            show(if (enabled) "Flashlight enabled." else "Flashlight disabled.")
        } catch (e: SecurityException) { show("Android denied flashlight access. Check app permissions.") }
        catch (_: Exception) { show("Unable to change flashlight state.") }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
