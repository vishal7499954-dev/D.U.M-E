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
