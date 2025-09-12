# Quick Start Guide - Bundled Python MediaPipe Service

Get your KMP app running with a bundled Python MediaPipe service in 5 minutes!

## 🚀 Quick Setup (5 minutes)

### 1. Setup Python Environment

**macOS/Linux:**
```bash
cd composeApp/src/desktopMain/resources
./setup_python_env.sh
```

**Windows:**
```cmd
cd composeApp\src\desktopMain\resources
setup_python_env.bat
```

### 2. Test Your Setup

```bash
# Activate virtual environment
source venv/bin/activate  # Unix
# or
venv\Scripts\activate.bat  # Windows

# Run test script
python test_setup.py
```

### 3. Build the Binary

**macOS/Linux:**
```bash
./build_python_binary.sh
```

**Windows:**
```cmd
build_python_binary.bat
```

### 4. Copy to Resources

**macOS:**
```bash
cp -r dist/pose_server.app ./
```

**Windows:**
```cmd
copy dist\pose_server.exe .\
```

**Linux:**
```bash
cp dist/pose_server ./
```

### 5. Test with Your App

Your `DesktopMediaPipeService` will automatically detect and use the bundled binary!

## 📁 What Gets Created

```
resources/
├── pose_server.app/          # macOS bundle
├── pose_server.exe           # Windows executable  
├── pose_server               # Linux binary
├── venv/                     # Python environment
└── dist/                     # Build output
```

## 🔧 Troubleshooting

**Binary not found?**
- Run `./setup_python_env.sh` first
- Check if PyInstaller is installed: `pip install pyinstaller`

**Build fails?**
- Ensure Python 3.8+ is installed
- Check virtual environment is activated
- Verify all dependencies in `requirements.txt`

**Binary won't start?**
- Make executable: `chmod +x pose_server`
- Check working directory
- Verify model files are bundled

## 📚 Next Steps

1. **Read the full documentation**: `PYTHON_SERVICE_README.md`
2. **Customize the build**: Edit `pose_server.spec`
3. **Test thoroughly**: Run your KMP app
4. **Package for distribution**: Include binary in your app bundle

## 🆘 Need Help?

- Check the troubleshooting section in `PYTHON_SERVICE_README.md`
- Verify your Python environment with `test_setup.py`
- Check the service logs for detailed error information

---

**That's it!** Your KMP app now has a professional, bundled MediaPipe service that works without requiring Python installation on end user machines.
