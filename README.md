# 📅 Smart Time Table (Multi-Platform Hub) 🚀

Welcome to the official repository of **Smart Time Table**—a high-performance, premium timetable management suite designed specifically for modern students, teachers, and classroom environments. 

This repository houses two optimized editions of the application, custom-built to provide the ultimate scheduling experience on both mobile and desktop platforms.

---

## 📂 Project Architecture

To keep both codebases clean, stable, and highly performant, the project is structured into two dedicated directories on this unified branch:

### 📱 [Android Edition](android/)
*   **Location**: [`/android`](android/)
*   **Language & Stack**: Kotlin, Jetpack Compose, Material 3.
*   **Key Feature**: **System-Wide Floating Overlay Widget**—displays your timetable on top of other running apps (e.g., during online classes) using `SYSTEM_ALERT_WINDOW` permission.
*   **Releases**: Pre-compiled APKs ready for instant download in the [`android/APKs/`](android/APKs/) folder.
*   👉 **[Explore the Android Code & Documentation](android/)**

### 💻 [Windows Pro Edition](windows/)
*   **Location**: [`/windows`](windows/)
*   **Language & Stack**: Kotlin Multiplatform, Compose Multiplatform, Gradle.
*   **Key Feature**: **Physics-Based "Smart-Hide" Overlay System**—smooth slide-out vertical handle that collapses/expands with dynamic physics gestures. Features native System Tray integration and desktop flyout menus.
*   **Releases**: Native `.exe` and `.msi` installers generated using WiX Toolset.
*   👉 **[Explore the Windows Code & Documentation](windows/)**

---

## ✨ Features Comparison 🎨

| Feature | 📱 Android Edition | 💻 Windows Pro Edition |
| :--- | :---: | :---: |
| **User Interface** | Material 3 Grid | Physics-based Smart-Hide Slider |
| **Always-on-top Overlay** | Yes (Floating service) | Yes (Spring-physics sidebar) |
| **Dark / Light Mode** | Fully Dynamic (System Sync) | Smooth Glassmorphic Theme |
| **System Integration** | Foreground Service | System Tray Menu & Flyout |
| **Database Caching** | Local Android storage | Local Profile JSON files |

---

## 🛠️ Global Development Setup

Both editions are managed using standard Gradle build systems. Ensure you have the following general tools installed:
*   **☕ Java Development Kit (JDK 17+)**
*   **🛠️ Android Studio** (for the Android project)
*   **🔨 WiX Toolset v3.11** (required for compiling Windows EXE/MSI installers)

For specific build, packaging, and distribution commands, please reference the respective subdirectories:
*   Build instructions for Android: [android/README.md](android/README.md)
*   Build instructions for Windows: [windows/README.md](windows/README.md)

---
*Developed with ❤️ by Nikhil Kumar Yadav.*
