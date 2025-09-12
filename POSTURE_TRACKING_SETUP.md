# Posture Tracking Setup Guide

This guide will help you set up the posture tracking feature for the Posturely app using MediaPipe Pose Landmarker.

## What's Been Implemented

### iOS Implementation
1. **PoseLandmarkerService.swift** - Handles MediaPipe pose detection with livestream mode
2. **CameraViewController.swift** - Camera feed with real-time pose detection and skeleton visualization
3. **PostureTrackingBridge.swift** - Bridge between native iOS and Compose UI
4. **Podfile** - MediaPipe Tasks Vision dependency

### Compose UI Implementation
1. **PostureTrackingScreen.kt** - Modern UI for posture tracking interface
2. **Updated App.kt** - Navigation and main app interface
3. **MainScreen.kt** - Welcome screen with app information

## Setup Instructions

### 1. iOS Setup

#### Prerequisites
- Xcode 15.0 or later
- iOS 15.0 or later
- Physical iOS device (camera functionality required)

#### Steps
1. **Open the project in Xcode**
   ```bash
   cd iosApp
   open iosApp.xcworkspace  # Use .xcworkspace, not .xcodeproj
   ```

2. **Add the MediaPipe model to your project**
   - In Xcode, right-click on your project in the navigator
   - Select "Add Files to [ProjectName]"
   - Navigate to `iosApp/iosApp/Models/pose_landmarker.task`
   - Make sure "Add to target" is checked for your app target

3. **Configure camera permissions**
   - Open `Info.plist`
   - Add the following key:
     ```xml
     <key>NSCameraUsageDescription</key>
     <string>This app needs camera access to detect your posture and provide real-time feedback.</string>
     ```

4. **Build and run on a physical device**
   - Connect your iOS device
   - Select your device as the target
   - Build and run the project

### 2. Android Setup (Future Implementation)

The Android implementation will be added next, following a similar pattern with:
- Camera2 API integration
- MediaPipe Tasks Vision for Android
- Native bridge for Compose communication

### 3. Testing the Implementation

1. **Launch the app** - You should see the main screen with "Posturely" branding
2. **Tap "Start Posture Tracking"** - Navigate to the posture tracking screen
3. **Grant camera permissions** when prompted
4. **Position yourself in front of the camera** - The app will detect your pose and display skeleton overlay
5. **View pose data** - Real-time landmark coordinates and posture information

## Features Implemented

### Real-time Pose Detection
- ✅ Camera feed integration
- ✅ MediaPipe Pose Landmarker with livestream mode
- ✅ Real-time skeleton visualization
- ✅ Pose landmark data extraction

### UI Components
- ✅ Modern Material 3 design
- ✅ Navigation between screens
- ✅ Real-time pose data display
- ✅ Start/Stop controls
- ✅ Status indicators

### Native Integration
- ✅ iOS camera access
- ✅ MediaPipe Tasks Vision integration
- ✅ Bridge for Compose communication
- ✅ Error handling and status updates

## Next Steps

### Immediate Tasks
1. **Test on physical iOS device** - Camera functionality requires a real device
2. **Add the model file to Xcode project** - Follow the setup instructions above
3. **Implement Android version** - Similar implementation for Android platform

### Future Enhancements
1. **Posture Analysis Algorithms** - Calculate posture quality scores
2. **Real-time Feedback** - Audio/visual cues for posture correction
3. **Data Persistence** - Save posture tracking sessions
4. **Analytics Dashboard** - Track posture improvement over time
5. **Customizable Settings** - Adjust detection sensitivity and feedback preferences

## Troubleshooting

### Common Issues

1. **Camera not working**
   - Ensure you're testing on a physical device
   - Check camera permissions in Settings
   - Verify Info.plist has camera usage description

2. **MediaPipe model not found**
   - Make sure `pose_landmarker.task` is added to your Xcode project
   - Verify the file is included in your app target

3. **Build errors**
   - Use `iosApp.xcworkspace` instead of `iosApp.xcodeproj`
   - Run `pod install` if dependencies are missing
   - Clean build folder and rebuild

4. **Pose detection not working**
   - Ensure good lighting conditions
   - Position yourself clearly in the camera view
   - Check that the device has sufficient processing power

## Technical Details

### MediaPipe Integration
- Uses MediaPipe Tasks Vision 0.10.21
- Livestream mode for real-time processing
- 33 pose landmarks detection
- Confidence thresholds for reliable detection

### Architecture
- **Native Layer**: iOS camera + MediaPipe processing
- **Bridge Layer**: Communication between native and Compose
- **UI Layer**: Compose Multiplatform interface
- **Data Flow**: Camera → MediaPipe → Pose Data → Compose UI

### Performance Considerations
- Real-time processing on device
- Optimized for 30fps camera feed
- Background thread processing to avoid UI blocking
- Memory-efficient landmark data structures

## Support

If you encounter any issues during setup or implementation, please:
1. Check the troubleshooting section above
2. Verify all dependencies are properly installed
3. Ensure you're using the correct Xcode workspace
4. Test on a physical iOS device with camera access 