# Dedicated offline wake-word engine

## Selected engine

D.U.M-E will use the upstream **sherpa-onnx KeywordSpotter** API from the k2-fsa project. It performs keyword spotting locally and supports Android. The intended English starting point is the small `sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01` model. The wake phrase will be customized to a phrase such as `hey dum e` after testing the model's supported keyword format.

## Why the native package is not committed yet

The upstream Android setup requires compiled Android native libraries (including the sherpa JNI library and ONNX Runtime) for the target ABI, in addition to Kotlin API classes and model files. The project currently has no verified upstream AAR or native-library bundle checked into `app/libs`. Adding a similarly named third-party Maven wrapper without verifying its API and native binaries could break the app or introduce an unreviewed dependency. Therefore the dedicated engine is not marked as integrated yet.

## Integration sequence

1. Pin one upstream sherpa-onnx release and obtain its Android native package.
2. Include the Kotlin API and the correct native libraries for the phone ABI (at minimum test `arm64-v8a`; add other ABIs only if needed).
3. Add the English KWS model and customized keyword file as installable local model assets. Do not fetch models at runtime.
4. Capture mono 16 kHz PCM audio with Android `AudioRecord`, feed frames to sherpa-onnx `OnlineStream`, and call `KeywordSpotter.decode` while the stream is ready.
5. On a confirmed wake phrase, stop the KWS stream and hand off to a separate offline command-recognition stage. Resume wake listening after the command session ends.
6. Test microphone foreground-service behavior, CPU/battery use, false activations, and screen-off behavior on the actual phone before calling this feature complete.

## Privacy and safety requirements

- No network permission or cloud speech endpoint.
- Show a persistent microphone notification with a Stop action.
- Do not try to bypass the Android lock screen.
- Do not execute a phone action from a wake-word result alone; require a recognized command and retain explicit confirmation for sensitive actions.
- Keep the existing Android on-device speech listener clearly labeled as a temporary fallback until the sherpa-onnx path is tested.

## Current status

**Planning / dependency packaging stage.** The current `DumeWakeService` still uses Android's on-device `SpeechRecognizer`; sherpa-onnx keyword spotting has not yet been wired into the microphone service. No APK build has been started for this change.
