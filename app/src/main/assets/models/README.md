# MediaPipe Models Directory

This directory contains the AI models used by Osom for on-device task enhancement.

## Gemma3-1B-IT Model

**Required Model**: `gemma3-1b-it-int4.task`
- **Size**: ~600MB
- **Format**: MediaPipe .task format
- **Context Length**: 8192 tokens
- **Use Case**: Enhancing accessibility text into meaningful task descriptions

### How to Download

1. **Option 1: Google AI Edge Models** (Recommended)
   ```bash
   # Download from Google AI Edge model hub
   curl -L -o gemma3-1b-it-int4.task \
     "https://storage.googleapis.com/mediapipe-models/language_detector/gemma3-1b-it-int4.task"
   ```

2. **Option 2: Kaggle Models**
   - Visit: https://www.kaggle.com/models/google/gemma/frameworks/mediaPipe
   - Download Gemma 1B IT model in MediaPipe format
   - Place the `.task` file in this directory

3. **Option 3: Build from Source**
   - Use MediaPipe Model Maker to convert Gemma models
   - Follow: https://developers.google.com/mediapipe/solutions/genai/llm_inference

### File Placement

Place the downloaded model file as:
```
app/src/main/assets/models/gemma3-1b-it-int4.task
```

**Important**: The model file is not included in the repository due to its size (~600MB). You must download it separately before building the app.

### Verification

The model file should be exactly:
- **Filename**: `gemma3-1b-it-int4.task`
- **Size**: Approximately 600MB
- **Format**: Binary .task file for MediaPipe

Once downloaded, the MediaPipe backend will automatically load and use this model for task enhancement.