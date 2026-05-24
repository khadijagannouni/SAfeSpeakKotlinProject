# SafeSpeak assets

Drop a TensorFlow Lite toxicity classifier here as `toxicity.tflite`
to switch the runtime classifier from the heuristic fallback to real
ML inference.

The app loads the model via memory-mapped `assets/toxicity.tflite`
(see `ToxicityClassifier.tryLoadModel`). If the file is absent — as
is the case in this report build — the pipeline automatically falls
back to the transparent lexicon-based scorer so the full architecture
still runs end-to-end and the moderation pipeline stays exercised.

To plug in a real model:

1. Train or download an English text-classification TFLite model
   that outputs a single sigmoid score (toxic probability).
2. Quantize to int8 to keep the on-device footprint under ~15 MB.
3. Copy as `app/src/main/assets/toxicity.tflite`.
4. Replace the `// hook: real model inference would go here` block
   in `ToxicityClassifier.classify` with the tokenization + run call.

The `androidResources.noCompress += "tflite"` directive in
`app/build.gradle.kts` keeps the file uncompressed so it can be
memory-mapped at zero allocation cost.
