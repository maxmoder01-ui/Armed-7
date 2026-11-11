# Armed 7

Android module-framework foundation targeting ARMv7 devices.

## Project information

- Application ID: `com.kayser.armed7`
- Version: `0.1.0-alpha`
- Minimum Android: API 23
- Target ABI: `armeabi-v7a`
- Language: Kotlin

## Alpha 0.1

Includes a native Android interface and a package manager for declarative `.kmod` packages.

Features:
- Module manifest validation
- API compatibility checks
- Archive path protection
- 20 MB package size limit

## Limitations

Alpha 0.1 does not execute DEX, JAR or native code, inject code into Among Us, or modify the game process.

Compatibility with specific Among Us versions is not established.

## Build

GitHub Actions builds a debug APK using JDK 17, Gradle 8.9 and Android SDK.

APK output:

`app/build/outputs/apk/debug/app-debug.apk`
