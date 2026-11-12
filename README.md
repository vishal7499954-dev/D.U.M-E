# D.U.M-E — Offline Android Assistant

D.U.M-E is a privacy-first Android assistant project designed for Android 12 devices, including the Samsung Galaxy M31.

## Current build: offline foundation

- Native Kotlin Android app.
- Dark, minimal assistant interface.
- Local conversation history stored on the device.
- Short, serious assistant responses.
- Basic built-in offline commands (help, time, date, battery, remember, recall, clear memory).
- Android text-to-speech, using the voice installed on the phone.
- No Internet permission and no cloud API.

## Important: local AI model status

This first foundation build is **not yet a generative AI model**. It uses a small offline command-and-response engine. The next milestone is integrating a quantized local language model through a native inference runtime such as llama.cpp. Model compatibility and speed must be tested on the Galaxy M31 before choosing the final model. The model will need to be downloaded once or transferred onto the phone; after setup, inference can run offline.

Offline speech recognition is not included yet because Android speech-recognition providers may send audio to a network service even when offline is requested. We will add a verified on-device speech engine separately.

## Build

1. Open this repository in Android Studio.
2. Use JDK 17.
3. Let Gradle sync and download the Android Gradle Plugin dependencies.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Install the generated debug APK on the Android device.

Minimum Android version: Android 8.0 (API 26). Target SDK: 35.

## Privacy

The app requests no Internet permission. Notes and chat history are stored in app-private local storage. No account, telemetry, analytics, or remote AI service is configured.


## Background wake-word development

The current background-listener prototype uses Android's on-device SpeechRecognizer when the device provides it. Tap **START LISTENING** while D.U.M-E is open and grant microphone/notification permissions. Android shows an ongoing notification with a Stop action.

The listener looks for "Hey D.U.M-E". After detecting it, it listens for one command and places the recognized command in the notification. Tap that notification to open D.U.M-E and run the command. This tap is intentional: Android restricts apps from silently opening activities over other apps, and commands should not execute unexpectedly while the phone is locked.

This is not yet a verified always-on production wake-word engine. Android's speech recognizer may stop, may not provide offline recognition for the selected language, and may be restricted by device battery management. A dedicated local wake-word engine and fully offline speech-to-text engine still need integration and on-device testing.

APK builds are intentionally manual only while development continues.
