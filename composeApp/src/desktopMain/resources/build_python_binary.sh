#!/bin/bash

# Build Python MediaPipe Service Binary
# This script creates a standalone binary that can be distributed with the KMP app

set -e

echo "=== Building Python MediaPipe Service Binary ==="

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "Error: Virtual environment not found. Please run setup_python_env.sh first."
    exit 1
fi

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Check if PyInstaller is installed
if ! python -c "import PyInstaller" 2>/dev/null; then
    echo "Installing PyInstaller..."
    pip install pyinstaller
fi

# Clean previous builds
echo "Cleaning previous builds..."
rm -rf build/ dist/ __pycache__/

# Build the binary
echo "Building with PyInstaller..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "Building for macOS..."
    pyinstaller --clean pose_server.spec
    echo "Binary created at: dist/pose_server.app"
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "win32" ]]; then
    # Windows
    echo "Building for Windows..."
    pyinstaller --clean --onefile mediapipe_pose_detector.py --name pose_server
    echo "Binary created at: dist/pose_server.exe"
else
    # Linux
    echo "Building for Linux..."
    pyinstaller --clean --onefile mediapipe_pose_detector.py --name pose_server
    echo "Binary created at: dist/pose_server"
fi

echo "=== Build Complete ==="
echo "The binary is ready to be bundled with your KMP app."
echo ""
echo "Next steps:"
echo "1. Copy the binary from dist/ to your app's resources"
echo "2. Update your KMP code to use the bundled binary"
echo "3. Test the bundled version"
