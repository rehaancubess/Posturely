#!/bin/bash

# Download MediaPipe Pose Landmarker models for iOS
echo "Downloading MediaPipe Pose Landmarker models for iOS..."

# Create models directory if it doesn't exist
mkdir -p iosApp/iosApp/Models

# Download the lite model
echo "Downloading lite model..."
curl -L -o iosApp/iosApp/Models/pose_landmarker_lite.task \
  "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task"

# Download the full model
echo "Downloading full model..."
curl -L -o iosApp/iosApp/Models/pose_landmarker.task \
  "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task"

echo "Models downloaded successfully to iosApp/iosApp/Models/"
echo "Please add these files to your Xcode project:"
echo "1. Open Xcode"
echo "2. Right-click on your project in the navigator"
echo "3. Select 'Add Files to [ProjectName]'"
echo "4. Navigate to iosApp/iosApp/Models/"
echo "5. Select both pose_landmarker_lite.task and pose_landmarker.task"
echo "6. Make sure 'Add to target' is checked for your app target" 