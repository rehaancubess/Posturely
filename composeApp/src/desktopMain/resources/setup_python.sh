#!/bin/bash

echo "Setting up Python environment for MediaPipe Pose Detection..."

# Check if Python 3.11 is available (MediaPipe compatible)
if command -v python3.11 &> /dev/null; then
    PYTHON_CMD="python3.11"
    PIP_CMD="pip3.11"
    echo "Using Python 3.11 (MediaPipe compatible)"
elif command -v python3.10 &> /dev/null; then
    PYTHON_CMD="python3.10"
    PIP_CMD="pip3.10"
    echo "Using Python 3.10 (MediaPipe compatible)"
elif command -v python3.9 &> /dev/null; then
    PYTHON_CMD="python3.9"
    PIP_CMD="pip3.9"
    echo "Using Python 3.9 (MediaPipe compatible)"
elif command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
    PIP_CMD="pip3"
    echo "Using default Python 3 (may not be MediaPipe compatible)"
else
    echo "Python 3 is not installed. Please install Python 3.9, 3.10, or 3.11."
    echo "You can download it from: https://www.python.org/downloads/"
    exit 1
fi

# Check Python version
PYTHON_VERSION=$($PYTHON_CMD -c 'import sys; print(".".join(map(str, sys.version_info[:2])))')
echo "Found Python version: $PYTHON_VERSION"

# Check if pip is installed
if ! command -v $PIP_CMD &> /dev/null; then
    echo "$PIP_CMD is not installed. Please install pip."
    exit 1
fi

# Create virtual environment (optional but recommended)
echo "Creating virtual environment..."
$PYTHON_CMD -m venv venv

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Upgrade pip
echo "Upgrading pip..."
pip install --upgrade pip

# Install required packages
echo "Installing required packages..."
pip install -r requirements.txt

echo ""
echo "Setup complete! To use MediaPipe:"
echo "1. Activate the virtual environment: source venv/bin/activate"
echo "2. Run the Python script: python mediapipe_pose_detector.py"
echo ""
echo "Note: The first run may download MediaPipe models which can take a few minutes."
