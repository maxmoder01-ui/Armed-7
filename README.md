
# Armed 7

Independent Android module manager for ARMv7 devices.

## Project information

- Application: Armed 7
- Version: 1.0.0
- Application ID: com.kayser.armed7
- Minimum Android: API 23
- Target ABI: armeabi-v7a
- Language: Kotlin
- Module extension: .kmod
- Module API: 1

## Features

- Independent module catalog
- Import and uninstall .kmod packages
- Module manifest validation
- Game version and ABI detection
- Module compatibility checks
- Enable and disable module states
- JSON and TXT resource viewer
- PNG, JPG and WEBP image preview
- Archive path traversal protection
- Duplicate archive path detection
- 20 MB package extraction limit
- 500 archive entry limit
- 64 KB manifest limit
- Private application storage

## KMOD package structure

A module package is a ZIP archive renamed to .kmod.

Example:

module.kmod
├── manifest.json
├── assets/
│   ├── config.json
│   └── preview.png
└── resources/
    └── description.txt

## Important limitations

Armed 7 manages declarative module packages independently.

It does not:
- Execute DEX or JAR code.
- Execute native libraries.
- Inject code into Among Us.
- Modify the Among Us process.
- Bypass game protections.

Enabling a module currently changes its state inside Armed 7 only. It does not activate that module inside Among Us.

Actual in-game functionality requires a compatible and authorized game-side integration.

Compatibility with specific Among Us versions must be verified separately.

## Build

The project uses:
- JDK 17
- Gradle 8.9
- Android SDK 35

GitHub Actions can build the debug APK.

Output:

app/build/outputs/apk/debug/app-debug.apk
