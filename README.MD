# Compose Call Guard 🛡️

![Android](https://img.shields.io/badge/Android-12%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)

**Compose Call Guard** is a lightweight, privacy-focused Android call blocking application built with modern Android architecture. 

Unlike traditional blockers that rely on cloud databases, this app uses **Regular Expressions (Regex)** to give users granular, offline control over who can reach them. It features a stunning **Material You** interface that adapts to your device's wallpaper.

👉 **[Download Latest APK](https://github.com/AndrewWangDev/Compose-Call-Guard/releases)** 

---

## ✨ Key Features

-   **Regex Power**: Filter calls using powerful Regular Expressions (e.g., `^1\d{2}.*` blocks specific prefixes).
-   **Dual-Layer Filtering**: 
    1.  **Whitelist (Priority)**: Numbers matching these rules are always allowed.
    2.  **Blacklist**: Numbers matching these rules are silently rejected.
-   **Privacy First**: Runs 100% offline using Android's `CallScreeningService`. No contacts uploaded, no internet permission required.
-   **Material You Design**: Built entirely with Jetpack Compose and Material 3, supporting Dynamic Colors.
-   **History Log**: Keep track of blocked calls with timestamps and matched rules.

## 📸 Screenshots

| **Block History** | **Settings & Rules** | **Add Regex Rule** |
|:---:|:---:|:---:|
| <img src="screenshots/History.png" width="250"/> | <img src="screenshots/Setting.png" width="250"/> | <img src="screenshots/Add_number.png" width="250"/> |

## 🛠️ Tech Stack

* **Language**: Kotlin
* **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **Architecture**: MVVM (Model-View-ViewModel)
* **Database**: [Room](https://developer.android.com/training/data-storage/room) (SQLite abstraction)
* **Storage**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)
* **System API**: `CallScreeningService` (Android 10+)
* **Concurrency**: Kotlin Coroutines & Flow

## 🚀 Getting Started

### Prerequisites
* Android Studio Iguana or newer.
* JDK 17+.
* Android Device/Emulator running API Level 29 (Android 10) or higher.

### Installation
1.  Clone the repository:
    ```bash
    git clone [https://github.com/AndrewWangDev/Compose-Call-Guard.git](https://github.com/AndrewWangDev/Compose-Call-Guard.git)
    ```
2.  Open the project in Android Studio.
3.  Sync Gradle and Build.
4.  **Important**: After installation, go to `Settings -> Apps -> Default Apps -> Caller ID & Spam App` and select **Compose Call Guard**.

## 💡 How It Works

The app utilizes the `CallScreeningService` API. When an incoming call arrives:
1.  The system wakes up the service.
2.  The app checks the **Master Switch** in DataStore (Synchronously).
3.  It validates the number against **Whitelist Regexes** from Room Database.
4.  If not whitelisted, it checks against **Blacklist Regexes**.
5.  If matched, the call is rejected without notification, and the event is logged asynchronously.

## 🤝 Contribution

Contributions are welcome! Please fork the repository and submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Developed by [Andrew Wang](https://andrewwangdev.com/about/)*