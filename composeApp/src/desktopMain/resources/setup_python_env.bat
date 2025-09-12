@echo off
REM Setup Python Environment for MediaPipe Service on Windows
REM This script creates a virtual environment and installs all required dependencies

echo === Setting up Python Environment for MediaPipe Service ===

REM Get the directory where this script is located
cd /d "%~dp0"

REM Check if Python is available
python --version >nul 2>&1
if errorlevel 1 (
    echo Error: Python is not found in PATH.
    echo Please install Python 3.8 or higher and add it to PATH.
    echo You can download it from: https://www.python.org/downloads/
    pause
    exit /b 1
)

REM Check Python version
for /f "tokens=2" %%i in ('python --version 2^>^&1') do set PYTHON_VERSION=%%i
echo Using Python: %PYTHON_VERSION%

REM Create virtual environment if it doesn't exist
if not exist "venv" (
    echo Creating virtual environment...
    python -m venv venv
) else (
    echo Virtual environment already exists.
)

REM Activate virtual environment
echo Activating virtual environment...
call venv\Scripts\activate.bat

REM Upgrade pip
echo Upgrading pip...
python -m pip install --upgrade pip

REM Install required packages
echo Installing required packages...
pip install -r requirements.txt

REM Install PyInstaller for building standalone binaries
echo Installing PyInstaller...
pip install pyinstaller

REM Verify installation
echo Verifying installation...
python -c "import mediapipe as mp; import cv2; import numpy as np; from PIL import Image; import PyInstaller; print('✓ All required packages installed successfully!'); print(f'✓ MediaPipe version: {mp.__version__}'); print(f'✓ OpenCV version: {cv2.__version__}'); print(f'✓ NumPy version: {np.__version__}'); print(f'✓ Pillow version: {Image.__version__}'); print(f'✓ PyInstaller version: {PyInstaller.__version__}')"

echo.
echo === Python Environment Setup Complete ===
echo You can now build the standalone binary using:
echo   build_python_binary.bat
echo.
echo Or manually with:
echo   venv\Scripts\activate.bat
echo   pyinstaller --clean --onefile mediapipe_pose_detector.py --name pose_server
echo.
pause
