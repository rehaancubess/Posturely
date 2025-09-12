# Python MediaPipe Service - Bundled Binary Approach

This document explains how to use the bundled Python MediaPipe service approach for your KMP app, which eliminates the need for end users to have Python installed.

## Overview

The bundled approach packages your Python MediaPipe service into a standalone binary that can be distributed with your KMP app. This provides several benefits:

- **No Python Installation Required**: End users don't need Python or any dependencies
- **Self-Contained**: All MediaPipe libraries and models are bundled together
- **Cross-Platform**: Works on macOS, Windows, and Linux
- **Professional Distribution**: Clean, app-store friendly packaging

## How It Works

1. **Build Time**: PyInstaller creates a standalone binary from your Python script
2. **Distribution**: The binary is bundled with your KMP app
3. **Runtime**: Your KMP app launches the bundled binary as a subprocess
4. **Communication**: KMP and Python communicate via file-based IPC (same as before)

## Setup Instructions

### 1. Prepare Python Environment

First, set up the Python environment with all required dependencies:

**macOS/Linux:**
```bash
cd composeApp/src/desktopMain/resources
chmod +x setup_python_env.sh
./setup_python_env.sh
```

**Windows:**
```cmd
cd composeApp\src\desktopMain\resources
setup_python_env.bat
```

This will:
- Create a virtual environment
- Install MediaPipe, OpenCV, NumPy, Pillow, and PyInstaller
- Verify all dependencies are working

### 2. Build the Standalone Binary

Build the standalone binary using PyInstaller:

**macOS/Linux:**
```bash
chmod +x build_python_binary.sh
./build_python_binary.sh
```

**Windows:**
```cmd
build_python_binary.bat
```

This creates:
- **macOS**: `dist/pose_server.app` (bundle) or `dist/pose_server` (binary)
- **Windows**: `dist/pose_server.exe`
- **Linux**: `dist/pose_server`

### 3. Bundle with Your KMP App

Copy the built binary to your app's resources:

**macOS:**
```bash
# Copy the .app bundle
cp -r dist/pose_server.app composeApp/src/desktopMain/resources/

# Or copy just the binary
cp dist/pose_server composeApp/src/desktopMain/resources/
```

**Windows:**
```cmd
copy dist\pose_server.exe composeApp\src\desktopMain\resources\
```

**Linux:**
```bash
cp dist/pose_server composeApp/src/desktopMain/resources/
```

### 4. Update Build Configuration

Ensure your build configuration includes the binary. The `DesktopMediaPipeService` automatically detects and uses the bundled binary.

## File Structure

After setup, your resources directory should look like:

```
composeApp/src/desktopMain/resources/
├── mediapipe_pose_detector.py          # Original Python script (fallback)
├── pose_server.app/                    # macOS bundle (or pose_server binary)
│   └── Contents/
│       └── MacOS/
│           └── pose_server
├── pose_server.exe                     # Windows executable
├── pose_server                         # Linux binary
├── pose_landmarker_full.task           # MediaPipe model
├── pose_landmarker_lite.task           # MediaPipe model (lite)
├── requirements.txt                    # Python dependencies
├── pose_server.spec                    # PyInstaller configuration
├── setup_python_env.sh                 # Environment setup (Unix)
├── setup_python_env.bat                # Environment setup (Windows)
├── build_python_binary.sh              # Build script (Unix)
├── build_python_binary.bat             # Build script (Windows)
└── venv/                               # Python virtual environment
```

## How the Service Works

### Automatic Detection

The `DesktopMediaPipeService` automatically detects the best available service:

1. **Priority 1**: Bundled binary (PyInstaller)
2. **Priority 2**: Python script with virtual environment
3. **Fallback**: Error if neither is available

### Service Types

```kotlin
enum class ServiceType {
    BUNDLED_BINARY,    // PyInstaller bundled binary
    PYTHON_SCRIPT,     // Python script with virtual environment
    UNKNOWN            // Not determined yet
}
```

### Binary Detection

The service looks for binaries in these locations:

**macOS:**
- `pose_server.app/Contents/MacOS/pose_server`
- `pose_server`
- `dist/pose_server.app/Contents/MacOS/pose_server`
- `dist/pose_server`

**Windows:**
- `pose_server.exe`
- `dist/pose_server.exe`

**Linux:**
- `pose_server`
- `dist/pose_server`

## Troubleshooting

### Common Issues

1. **Binary Not Found**
   - Ensure the binary was built successfully
   - Check file permissions (make executable on Unix)
   - Verify the binary is in the correct resources directory

2. **Binary Won't Start**
   - Check if the binary has execute permissions
   - Verify all dependencies are bundled correctly
   - Check the binary's working directory

3. **Model Loading Issues**
   - Ensure model files are bundled with the binary
   - Check the working directory setting
   - Verify model file paths in the PyInstaller spec

### Debug Mode

For debugging, you can modify the PyInstaller spec:

```python
# In pose_server.spec, change:
console=True,  # Shows console output for debugging
```

### Logs

The service provides detailed logging:
- Service type detection
- Binary/script paths
- Process startup status
- Communication status

## Performance Considerations

### Binary Size

The bundled binary will be larger than the Python script:
- **Python script**: ~1-5 MB
- **Bundled binary**: ~50-200 MB (includes all dependencies)

### Startup Time

- **First launch**: May be slower due to binary extraction
- **Subsequent launches**: Faster than Python script startup

### Memory Usage

- **Bundled binary**: Slightly higher memory usage
- **Python script**: Lower memory usage but requires Python runtime

## Distribution

### macOS

For macOS distribution:
- Use the `.app` bundle for better integration
- Sign the binary for Gatekeeper compatibility
- Include in your `.dmg` or `.app` package

### Windows

For Windows distribution:
- Include the `.exe` in your installer
- No additional dependencies required
- Works on all Windows versions (7+)

### Linux

For Linux distribution:
- Include the binary in your package
- Ensure proper permissions
- Consider AppImage for better compatibility

## Advanced Configuration

### Custom PyInstaller Options

Modify `pose_server.spec` for custom builds:

```python
# Add custom data files
datas=[
    ('pose_landmarker_full.task', '.'),
    ('pose_landmarker_lite.task', '.'),
    ('config.json', '.'),
    ('models/', 'models/'),
],

# Exclude unnecessary modules
excludes=['tkinter', 'matplotlib', 'jupyter'],

# Custom hooks
hookspath=['custom_hooks/'],
```

### Environment Variables

The service automatically sets:
- Working directory for model loading
- Virtual environment (if using Python script)
- Process environment

### Communication

The service uses the same file-based IPC:
- Commands: `/tmp/posture_commands/`
- Responses: `/tmp/posture_responses/`
- PID file: `/tmp/posture_python.pid`

## Migration from Python Script

If you're currently using the Python script approach:

1. **Build the binary** using the provided scripts
2. **Test the binary** to ensure it works correctly
3. **Update your app** to use the new service
4. **Remove Python dependencies** from your distribution

The service automatically falls back to the Python script if the binary isn't available, so the transition is seamless.

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Verify your Python environment setup
3. Check PyInstaller documentation
4. Review the service logs for detailed error information

## Next Steps

1. Set up your Python environment
2. Build the standalone binary
3. Test with your KMP app
4. Package for distribution
5. Deploy to end users

The bundled approach provides a professional, user-friendly experience while maintaining all the functionality of your MediaPipe pose detection service.
