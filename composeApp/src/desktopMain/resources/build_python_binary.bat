@echo off
REM Build Python MediaPipe Service Binary for Windows
REM This script creates a standalone binary that can be distributed with the KMP app

echo === Building Python MediaPipe Service Binary for Windows ===

REM Get the directory where this script is located
cd /d "%~dp0"

REM Check if virtual environment exists
if not exist "venv" (
    echo Error: Virtual environment not found. Please run setup_python_env.bat first.
    pause
    exit /b 1
)

REM Activate virtual environment
echo Activating virtual environment...
call venv\Scripts\activate.bat

REM Check if PyInstaller is installed
python -c "import PyInstaller" 2>nul
if errorlevel 1 (
    echo Installing PyInstaller...
    pip install pyinstaller
)

REM Clean previous builds
echo Cleaning previous builds...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
if exist __pycache__ rmdir /s /q __pycache__

REM Build the binary
echo Building with PyInstaller...
pyinstaller --clean --onefile mediapipe_pose_detector.py --name pose_server

echo === Build Complete ===
echo Binary created at: dist\pose_server.exe
echo.
echo Next steps:
echo 1. Copy the binary from dist\ to your app's resources
echo 2. Update your KMP code to use the bundled binary
echo 3. Test the bundled version
echo.
pause
