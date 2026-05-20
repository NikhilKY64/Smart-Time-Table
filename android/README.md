# 📱 Smart Time Table (Android Edition) 🚀

> [!WARNING]
> **Android Edition Status: Work in Progress (Unstable)**
> The Android edition of this application is currently in an **experimental / pre-alpha state** and does not function correctly on all devices. Development is active, but you may experience bugs or crashes. 
> 
> 🤝 **Contributions are highly welcome!** If you are an Android developer and want to help stabilize the Compose UI or overlay service, please feel free to open a Pull Request!

A high-performance, premium timetable application designed for Android devices. Featuring an **experimental system-wide floating overlay service**, it is designed to help students keep their schedule accessible.

---

## ✨ Key Features 🌟

*   **📺 Floating System Overlay**: Draw your timetable directly over other applications. Double-tap or toggle to keep your schedule at a glance without having to leave active apps.
*   **🎨 Premium Material 3 UI**: Modern, glassmorphic card design with beautiful color palettes, smooth animations, and clean layouts built entirely in **Jetpack Compose**.
*   **🌙 Fully Dynamic Theme**: Seamless support for Light and Dark modes matching system preferences.
*   **⚙️ Lightweight & Instant**: Launches in milliseconds, maintains a tiny system footprint, and caches your schedules locally for offline usage.
*   **📦 Pre-compiled APKs**: Ready-to-install packages available directly in the `APKs/` folder.

---

## 🛠️ Getting Started & Build Instructions

### 📋 Prerequisites
*   **☕ JDK 17**: Ensure your JVM is set to Java 17.
*   **🤖 Android SDK**: Android API level 24 (Android 7.0 Nougat) or higher.
*   **🔨 Build System**: Gradle (Kotlin DSL).

### 💻 Development Setup
To build and run the application locally on an Android device or emulator:
1. Open the project root folder in **Android Studio**.
2. Sync the project with Gradle files.
3. Connect your Android device or start an emulator.
4. Run the `:app` module.

Alternatively, build the debug APK directly from your command line:
```powershell
.\gradlew.bat assembleDebug
```

---

## 📦 Releases & APKs
If you want to install the app immediately on your Android device without compiling the source:
1. Navigate to the **`APKs/`** folder in this repository.
2. Download the latest version:
   *   **`APKs/SmartTimeTableV2.apk`** (Latest v2 Release)
   *   **`APKs/SmartTimeTableV1.1.apk`** (Stable v1.1 Release)
3. Transfer the file to your Android phone and install it (make sure to allow installation from unknown sources).

---

## 🔒 Permissions Used
To enable its unique floating widget features, the application requests the following Android system permissions:
*   `SYSTEM_ALERT_WINDOW`: Required to display the timetable overlay on top of other running applications.
*   `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`: Ensures the overlay service stays active and does not get terminated prematurely by Android's background limits.

---
*Developed with ❤️ for a smarter, more productive school routine.*
