# MediaPipe Pose Detection for Desktop App

This directory contains the Python-based MediaPipe pose detection service that integrates with your Kotlin desktop application.

## What's New

- **Real MediaPipe Pose Detection**: Replaces the mock JavaScript implementation with actual MediaPipe Python API
- **Python Subprocess Integration**: Runs MediaPipe as a separate Python process for better performance and reliability
- **Real-time Pose Landmarks**: Provides actual 33 pose landmarks with coordinates and confidence scores
- **Proper Posture Analysis**: Calculates real posture metrics based on MediaPipe landmark data

## Setup Instructions

### 1. Prerequisites

- Python 3.9, 3.10, or 3.11 (MediaPipe compatible versions)
- pip package manager

### 2. Install Dependencies

Run the setup script to automatically install all required packages:

```bash
cd composeApp/src/desktopMain/resources
chmod +x setup_python.sh
./setup_python.sh
```

This will:
- Create a Python virtual environment
- Install MediaPipe 0.10.21 and all dependencies
- Set up the environment for pose detection

### 3. Test the Setup

Verify that everything is working:

```bash
cd composeApp/src/desktopMain/resources
source venv/bin/activate
python test_mediapipe.py
```

You should see "All tests passed! MediaPipe is ready to use."

## How It Works

### Architecture

1. **Kotlin App** → **Python Subprocess** → **MediaPipe API**
2. **Image Frames** are converted to base64 and sent to Python
3. **Python processes** frames using MediaPipe Pose Landmarker
4. **Results** are sent back to Kotlin via JSON over stdin/stdout
5. **Real-time updates** flow through StateFlow observables

### Communication Protocol

The Kotlin app communicates with Python using JSON commands:

```json
// Initialize MediaPipe
{"type":"init","model_path":"pose_landmarker_lite.task"}

// Process frame
{"type":"detect","frame_data":"base64_image_data","timestamp":"1234567890"}

// Close service
{"type":"close"}
```

Python responds with:

```json
// Initialization response
{"type":"init_response","success":true,"message":"Initialized successfully"}

// Detection result
{"type":"detection_result","result":{"landmarks":[...],"success":true}}

// Error
{"type":"error","message":"Error description"}
```

## Files

- `mediapipe_pose_detector.py` - Main Python service for MediaPipe pose detection
- `requirements.txt` - Python package dependencies
- `setup_python.sh` - Automated setup script
- `test_mediapipe.py` - Test script to verify installation
- `README.md` - This documentation

## Usage in Kotlin

The `DesktopMediaPipeService` class handles all communication with Python:

```kotlin
val mediaPipeService = DesktopMediaPipeService()
mediaPipeService.setupPoseLandmarker()
mediaPipeService.startTracking()

// Process frames
mediaPipeService.detectAsync(bufferedImage, timestamp)

// Get real-time updates
mediaPipeService.landmarksFlow.collect { landmarks ->
    // Handle real pose landmarks
}
```

## Troubleshooting

### Common Issues

1. **Python version incompatible**: Ensure you're using Python 3.9-3.11
2. **MediaPipe not found**: Run `./setup_python.sh` to install dependencies
3. **Virtual environment not activated**: Always activate with `source venv/bin/activate`

### Debug Mode

Enable debug logging by checking the console output. The service provides detailed logs for:
- Python process startup
- MediaPipe initialization
- Frame processing
- Error handling

### Performance Notes

- First run may download MediaPipe models (~50MB)
- Processing latency: ~30-100ms per frame depending on hardware
- Memory usage: ~200-500MB for MediaPipe runtime

## Next Steps

1. **Test the integration** by running your desktop app
2. **Monitor console logs** for MediaPipe initialization and processing
3. **Verify pose detection** by checking landmark data in real-time
4. **Optimize performance** by adjusting confidence thresholds if needed

The integration should now provide real MediaPipe pose detection with actual 33 landmarks, replacing the previous mock implementation!
