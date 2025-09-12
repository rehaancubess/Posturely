#!/usr/bin/env python3
"""
Test script to verify MediaPipe setup and environment
Run this to ensure everything is working before building the binary
"""

import sys
import os

def test_imports():
    """Test if all required packages can be imported"""
    print("Testing package imports...")
    
    try:
        import mediapipe as mp
        print(f"✓ MediaPipe {mp.__version__} imported successfully")
    except ImportError as e:
        print(f"✗ MediaPipe import failed: {e}")
        return False
    
    try:
        import cv2
        print(f"✓ OpenCV {cv2.__version__} imported successfully")
    except ImportError as e:
        print(f"✗ OpenCV import failed: {e}")
        return False
    
    try:
        import numpy as np
        print(f"✓ NumPy {np.__version__} imported successfully")
    except ImportError as e:
        print(f"✗ NumPy import failed: {e}")
        return False
    
    try:
        from PIL import Image
        print(f"✓ Pillow {Image.__version__} imported successfully")
    except ImportError as e:
        print(f"✗ Pillow import failed: {e}")
        return False
    
    try:
        import PyInstaller
        print(f"✓ PyInstaller {PyInstaller.__version__} imported successfully")
    except ImportError as e:
        print(f"✗ PyInstaller import failed: {e}")
        return False
    
    return True

def test_mediapipe_functionality():
    """Test basic MediaPipe functionality"""
    print("\nTesting MediaPipe functionality...")
    
    try:
        import mediapipe as mp
        from mediapipe.tasks import python
        from mediapipe.tasks.python import vision
        
        # Test if we can create base options
        base_options = python.BaseOptions()
        print("✓ BaseOptions created successfully")
        
        # Test if we can create pose landmarker options
        options = vision.PoseLandmarkerOptions(
            base_options=base_options,
            running_mode=vision.RunningMode.VIDEO,
            num_poses=1
        )
        print("✓ PoseLandmarkerOptions created successfully")
        
        return True
        
    except Exception as e:
        print(f"✗ MediaPipe functionality test failed: {e}")
        return False

def test_model_files():
    """Test if model files are accessible"""
    print("\nTesting model file accessibility...")
    
    model_files = [
        "pose_landmarker_full.task",
        "pose_landmarker_lite.task"
    ]
    
    all_found = True
    for model_file in model_files:
        if os.path.exists(model_file):
            size = os.path.getsize(model_file)
            print(f"✓ {model_file} found ({size:,} bytes)")
        else:
            print(f"✗ {model_file} not found")
            all_found = False
    
    return all_found

def test_environment():
    """Test environment variables and paths"""
    print("\nTesting environment...")
    
    print(f"Python executable: {sys.executable}")
    print(f"Python version: {sys.version}")
    print(f"Working directory: {os.getcwd()}")
    
    # Check if we're in a virtual environment
    if hasattr(sys, 'real_prefix') or (hasattr(sys, 'base_prefix') and sys.base_prefix != sys.prefix):
        print("✓ Running in virtual environment")
    else:
        print("⚠ Not running in virtual environment")
    
    # Check PATH
    path_dirs = os.environ.get('PATH', '').split(os.pathsep)
    print(f"PATH contains {len(path_dirs)} directories")
    
    return True

def main():
    """Run all tests"""
    print("=== MediaPipe Setup Test ===\n")
    
    tests = [
        ("Package Imports", test_imports),
        ("MediaPipe Functionality", test_mediapipe_functionality),
        ("Model Files", test_model_files),
        ("Environment", test_environment)
    ]
    
    results = []
    for test_name, test_func in tests:
        try:
            result = test_func()
            results.append((test_name, result))
        except Exception as e:
            print(f"✗ {test_name} test crashed: {e}")
            results.append((test_name, False))
    
    # Summary
    print("\n=== Test Summary ===")
    passed = 0
    total = len(results)
    
    for test_name, result in results:
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"{test_name}: {status}")
        if result:
            passed += 1
    
    print(f"\nResults: {passed}/{total} tests passed")
    
    if passed == total:
        print("🎉 All tests passed! Your environment is ready for building.")
        print("\nNext steps:")
        print("1. Run: ./build_python_binary.sh (Unix) or build_python_binary.bat (Windows)")
        print("2. Copy the binary to your resources directory")
        print("3. Test with your KMP app")
    else:
        print("❌ Some tests failed. Please fix the issues before building.")
        return 1
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
