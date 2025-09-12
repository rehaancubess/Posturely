#!/bin/bash

# Setup Python Environment for MediaPipe Service
# This script creates a virtual environment and installs all required dependencies

set -e

echo "=== Setting up Python Environment for MediaPipe Service ==="

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if Python 3.8+ is available
PYTHON_CMD=""
for cmd in python3 python3.13 python3.12 python3.11 python3.10 python3.9 python3.8 python; do
            if command -v "$cmd" &> /dev/null; then
            PYTHON_VERSION=$("$cmd" --version 2>&1 | grep -oE '[0-9]+\.[0-9]+')
            if [[ "$PYTHON_VERSION" > "3.7" ]] || [[ "$PYTHON_VERSION" == "3.13" ]] || [[ "$PYTHON_VERSION" == "3.12" ]]; then
            PYTHON_CMD="$cmd"
            break
        fi
    fi
done

if [ -z "$PYTHON_CMD" ]; then
    echo "Error: Python 3.8 or higher is required but not found."
    echo "Please install Python 3.8+ and try again."
    exit 1
fi

echo "Using Python: $PYTHON_CMD ($PYTHON_VERSION)"

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    "$PYTHON_CMD" -m venv venv
else
    echo "Virtual environment already exists."
fi

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Upgrade pip
echo "Upgrading pip..."
pip install --upgrade pip

# Install required packages
echo "Installing required packages..."
pip install -r requirements.txt

# Install PyInstaller for building standalone binaries
echo "Installing PyInstaller..."
pip install pyinstaller

# Verify installation
echo "Verifying installation..."
python -c "
import mediapipe as mp
import cv2
import numpy as np
from PIL import Image
import PyInstaller
print('✓ All required packages installed successfully!')
print(f'✓ MediaPipe version: {mp.__version__}')
print(f'✓ OpenCV version: {cv2.__version__}')
print(f'✓ NumPy version: {np.__version__}')
print(f'✓ Pillow version: {Image.__version__}')
print(f'✓ PyInstaller version: {PyInstaller.__version__}')
"

echo ""
echo "=== Python Environment Setup Complete ==="
echo "You can now build the standalone binary using:"
echo "  ./build_python_binary.sh"
echo ""
echo "Or manually with:"
echo "  source venv/bin/activate"
echo "  pyinstaller pose_server.spec"
