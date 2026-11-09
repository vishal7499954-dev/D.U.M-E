# JARVIS Android — starter project

This is a real, installable Android starter app for Android 8+ (targeted to Android 12-compatible behavior), styled as a blue holographic JARVIS console.

## Included and functional
- Text-to-speech using the Android system TTS engine (download an offline voice in Android settings).
- Speech input through Android speech recognition, requesting offline recognition where the installed provider supports it.
- Local note memory using SharedPreferences (`remember ...`, `recall`, `forget memory`).
- Battery status.
- Flashlight on/off.
- Open installed apps by their exact displayed app name.
- Open Android Settings.
- Optional online search.
- No account, API key, analytics SDK, or network permission is included.

## Important limitations
This starter does **not** contain a generative language model. Its offline brain is a small command router, not ChatGPT/JARVIS-level conversational AI. Android's speech recognizer may still require a downloaded offline language pack and may ignore the offline preference depending on the device/provider. Text-to-speech works offline only when an offline voice is installed.

A local LLM is the next integration milestone. For a phone with 4 GB RAM or less, test a 0.5B–1.5B quantized GGUF model, not a 7B model. A true integrated LLM requires bundling/building a native inference runtime (for example llama.cpp JNI) and downloading a compatible model separately; it is intentionally not falsely represented as already included here.

## Build an APK
1. Install Android Studio on a computer.
2. Extract this ZIP.
3. Open the `JARVIS-Android` folder in Android Studio.
4. Let Gradle sync. Use JDK 17 if prompted.
5. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Find the debug APK under `app/build/outputs/apk/debug/app-debug.apk`.
7. Transfer it to the Android phone and install it. You may need to allow installation from that file manager.

## Commands
- `help`
- `battery`
- `flashlight on` / `flashlight off`
- `open Settings` (or exact installed app label)
- `remember buy milk`
- `recall`
- `forget memory`
- `search weather tomorrow` (online)

## Privacy and safety
- Notes are stored locally in app-private preferences.
- No dangerous permissions for contacts, SMS, calls, accessibility, or device administration are requested.
- The app does not execute arbitrary AI-generated actions.
