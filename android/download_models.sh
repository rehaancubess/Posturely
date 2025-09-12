#!/bin/bash

# Download MediaPipe Pose Landmarker models for Android
echo "Downloading MediaPipe Pose Landmarker models for Android..."

# Create assets directory if it doesn't exist
mkdir -p composeApp/src/androidMain/assets

# Download the lite model
echo "Downloading lite model..."
curl -L -o composeApp/src/androidMain/assets/pose_landmarker_lite.task \
  "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task"

# Download the full model
echo "Downloading full model..."
curl -L -o composeApp/src/androidMain/assets/pose_landmarker_full.task \
  "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task"

echo "Models downloaded to composeApp/src/androidMain/assets/"
echo "Make sure to add these files to your Android project assets in build.gradle.kts" 