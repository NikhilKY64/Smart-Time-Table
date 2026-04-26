# Smart Time Table (Windows Pro)

A high-performance, polished timetable application for Windows, featuring a physics-based "Smart-Hide" overlay system designed specifically for students and smart board users.

## 🚀 Getting Started

### Prerequisites
*   **JDK 17 or 21**: Ensure your Gradle JVM is set to Java 17 or higher.
*   **WiX Toolset (v3.11)**: Required if you want to generate the professional `.exe` or `.msi` installers. [Download here](https://wixtoolset.org/releases/).

### Development
To run the app directly from the source:
```powershell
.\gradlew.bat :app:run
```

---

## 📦 How to create a New Version (.EXE)

When you are ready to release a new version, follow these steps:

### 1. Update Version Numbers (Optional)
Open `app/build.gradle.kts` and update these two lines:
*   `versionName = "1.2.21"`
*   `packageVersion = "1.2.21"`

### 2. Run the Build Command
Use the "Distribution" command to create the final installer:
```powershell
.\gradlew.bat :app:packageReleaseDistributionForCurrentOS
```
or
```powershell
.\gradlew :app:packageReleaseDistributionForCurrentOS
```

*   **Location**: After it finishes, your installer will be in `app/build/compose/binaries/main/exe/`.

### 3. Create a Portable Folder
If you just want a folder that runs without installing:
```powershell
.\gradlew.bat :app:createDistributable
```
*   **Location**: `app/build/compose/binaries/main/app/`

---

## 🛠️ Maintenance & Data Safety

### Where is the data saved?
Your timetables and profiles are **not** saved in the app folder. They are saved in your user home directory to ensure they stay safe even if you uninstall the app:
*   **Path**: `C:\Users\<YourUser>\.smart-timetable\profiles.json`
*   **Backup**: To back up your timetables, just copy this `profiles.json` file.

### Troubleshooting
*   **"Unable to delete directory" error**: This means the app is still running. Close the app from the System Tray (near the clock) and try the build again.
*   **Installer not showing in Search**: We have enabled `shortcut = true` and `menu = true`. If you don't see them, uninstall the old version from Windows Settings before installing the new one.

---

## ✨ Features Checklist
*   [x] **Spring Physics**: Smooth vertical expansion and horizontal scrolling.
*   [x] **Taskbar Integration**: Minimizes to taskbar; restores and expands on click.
*   [x] **Smart-Hide**: Automatically wakes up and expands near period start times.
*   [x] **Auto-Slide Toggle**: Enable/Disable automatic expansion via Settings or Tray.
*   [x] **Universal Teacher Manager**: Map teachers to subjects once; syncs across all profiles.
*   [x] **Refined Tray Menu**: Quick access to smart controls and an easy-exit close button.
*   [x] **Visual Polish**: 12dp rounded corners, soft shadows, and linear gradients.
*   [x] **Robust Storage**: Profiles and Subject-Teacher mappings saved locally in your Home folder.
