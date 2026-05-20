# 📅 Smart Time Table (Multi-Platform Hub) 🚀

Welcome to the official repository of **Smart Time Table**—a high-performance, premium timetable management suite designed specifically for modern students, teachers, and classroom environments. 

This repository houses two optimized editions of the application, custom-built to provide the ultimate scheduling experience on both mobile and desktop platforms.

---

## 📂 Project Architecture

To keep both codebases clean, stable, and highly performant, the project is structured into two dedicated directories on this unified branch:

### 📱 [Android Edition](android/)
*   **Status:** 🚧 Incomplete (Development Suspended)
*   **Location:** 📂 Located inside the [`/android`](android/) directory.
*   **Core Tech:** ⚙️ Built using Kotlin, Jetpack Compose, and Android Material 3.
*   **Purpose:** 🎯 Designed to overlay a floating timetable grid over active apps.
*   **Key Feature:** ✨ Floating overlay widget using background foreground services.
*   **Input Limit:** 🚫 Pointer click events on the vertical slider are currently unresponsive.
*   **Releases:** 📦 Early test packages are available in the [`android/APKs/`](android/APKs/) folder.
*   👉 **[Explore the Android Code & Documentation](android/)**

### 💻 [Windows Edition](windows/)
*   **Status:** ✅ Stable / Fully Operational (Production Ready)
*   **Location:** 📂 Located inside the [`/windows`](windows/) directory.
*   **Core Tech:** ⚙️ Built using Kotlin Multiplatform and Compose Multiplatform.
*   **Purpose:** 🎯 Tailored for classrooms, smart boards, and desktop environments.
*   **Key Feature:** ✨ Smart-Hide vertical sidebar overlay with spring-physics animation.
*   **Integration:** 🔌 Native Windows System Tray integration with single-click flyout controls.
*   **Reliability:** 💾 Custom JSON profile structure to bypass system storage limitations.
*   **Installers:** 💿 Ready-to-install `.exe` and `.msi` packages generated via WiX.
*   **Role:** 📌 Primary active branch with regular feature updates and maintenance.
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

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Designed and custom-built specifically for school classroom interactive smart boards.*  
*Developed by Nikhil Kumar Yadav.*
