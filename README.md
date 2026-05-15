# 📅 Smart Time Table (Windows Pro) 🚀

A high-performance, premium timetable application for Windows. Featuring a **physics-based "Smart-Hide" overlay system**, it's designed specifically for students, teachers, and smart board environments.

---

## ✨ What's New in v1.2.22 🆕

*   **🚀 Single-Instance Wakeup**: Clicking the desktop icon now intelligently triggers the existing app's slider instead of creating duplicates.
*   **🖱️ Desktop Flyout Menu**: A native-feeling single-click flyout from the system tray for lightning-fast control.
*   **🛡️ Boot Intelligence**: Correctly handles Windows startup launches to stay hidden and keep your desktop clean.
*   **🎯 Interaction Polish**: Slider handle physics now respect your touch/drag gestures and won't auto-collapse while in use.
*   **🔗 "Open App" Shortcut**: Replaced "Maximize view" with a clearer "Open App" button in the slider panel for better navigation.

---

## 🛠️ Getting Started

### 📋 Prerequisites
*   **☕ JDK 17 or 21**: Ensure your Gradle JVM is set to Java 17+.
*   **🔨 WiX Toolset (v3.11)**: Required for generating `.exe` or `.msi` installers. [Download here](https://wixtoolset.org/releases/).

### 💻 Development
To run the app directly from the source:
```powershell
.\gradlew.bat :app:run
```

---

## 📦 Distribution & Releases

### 🏗️ How to create a New Version (.EXE)
1.  **🔢 Update Version**: Open `app/build.gradle.kts` and update `versionName` & `packageVersion` to `1.2.22`.
2.  **🔨 Build Installer**:
    ```powershell
    .\gradlew.bat :app:packageReleaseDistributionForCurrentOS
    ```
3.  **📂 Output**: Your installer will be in `app/build/compose/binaries/main/exe/`.

### 🚀 Create a Portable Version
```powershell
.\gradlew.bat :app:createDistributable
```
*   **Location**: `app/build/compose/binaries/main/app/`

---

## 💾 Data & Security

### 🏠 Where is my data?
Your data is stored safely in your user profile, separate from the app folder:
*   **📍 Path**: `%USERPROFILE%\.smart-timetable\`
*   **📝 Files**: `profiles.json` (Timetables) and `teachers.json` (Teacher Mappings).
*   **🛡️ Safety**: Uninstalling or updating the app will **never** delete your timetables.

---

## 🔧 Troubleshooting
*   **🚫 Build Errors?**: If you see "Unable to delete directory", close the app from the System Tray first.
*   **🔍 App not in Search?**: Uninstall older versions before installing a new one to refresh Windows shortcuts.

---

## ✅ Core Features
*   [x] **🍃 Spring Physics**: Ultra-smooth vertical expansion.
*   [x] **📥 Taskbar Sync**: Restore and expand directly from the taskbar.
*   [x] **⏰ Smart-Hide**: Intelligent wake-up logic for period transitions.
*   [x] **🔘 Auto-Slide**: Toggleable automatic expansion.
*   [x] **🎨 Premium UI**: 12dp rounded corners, glassmorphism effects, and dynamic gradients.
*   [x] **📁 Robust Storage**: Bypasses Registry limits for unlimited schedule capacity.

---
*Developed with ❤️ for the modern classroom.*
