# -*- mode: python ; coding: utf-8 -*-

block_cipher = None

a = Analysis(
    ['mediapipe_pose_detector.py'],
    pathex=[],
    binaries=[],
    datas=[
        ('pose_landmarker_full.task', '.'),
        ('pose_landmarker_lite.task', '.'),
    ],
    hiddenimports=[
        'mediapipe',
        'mediapipe.tasks',
        'mediapipe.tasks.python',
        'mediapipe.tasks.python.vision',
        'cv2',
        'numpy',
        'PIL',
        'PIL.Image',
        'json',
        'base64',
        'io',
        'os',
        'signal',
        'threading',
        'time',
        'pathlib',
        'glob'
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='pose_server',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,  # Set to True for debugging, False for production
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)

# macOS specific
app = BUNDLE(
    exe,
    name='pose_server.app',
    icon=None,
    bundle_identifier=None,
    info_plist={
        'CFBundleName': 'Pose Server',
        'CFBundleDisplayName': 'Pose Server',
        'CFBundleVersion': '1.0.0',
        'CFBundleShortVersionString': '1.0.0',
        'NSHighResolutionCapable': 'True',
    },
)
