# ✂️ SnapCrop

> The iOS-style **"Crop, Copy & Delete"** screenshot workflow for Android & GrapheneOS.

[![Platform](https://img.shields.io/badge/Platform-Android%2010+-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![APK Size](https://img.shields.io/badge/APK%20Size-1.43%20MB-brightgreen.svg)]()
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg)]()

---

## 💡 What is SnapCrop?

On iOS, taking a screenshot gives you an option to crop and tap **"Copy and Delete"**—placing the cropped image into your clipboard while immediately purging the file from your photo library. 

Standard Android and GrapheneOS do not offer this out-of-the-box, leaving thousands of temporary throwaway screenshots clogging up your storage. **SnapCrop fixes this.**

---

## ⚡ Features

* **📋 Copy & Delete:** Crops the image, places it directly on your system clipboard (ready to paste into Signal, WhatsApp, Discord, Messages, etc.), and deletes the original screenshot file so it never clutters your storage.
* **💾 Save or 🗑️ Delete:** Keep the cropped version in your gallery, or discard accidental screenshots immediately.
* **⚡ 0.00% Idle Battery Drain (Pure On-Demand):** Zero background services, zero polling, and zero persistent notifications. It only runs for the few seconds you are actively editing.
* **👻 Ghost in Recent Apps:** Automatically removes its own window from your Recent Apps switcher (`finishAndRemoveTask`) the moment you finish editing.
* **🛡️ 100% Private & Offline:** Zero internet permissions requested (`android.permission.INTERNET` is completely absent). Fully compatible with **GrapheneOS Storage Scopes** to sandbox access to `Pictures/Screenshots` only.
* **🪶 Featherweight:** Built in pure Kotlin and Jetpack Compose with **zero third-party dependencies**. Only **~1,600 lines of code** and a **1.43 MB** APK.

---

## 📱 How to Use on Your Phone

1. Take a screenshot normally (**Power + Volume Down**).
2. Tap the floating thumbnail in the corner or tap **Edit**.
3. When Android asks *"Open with..."*, select **SnapCrop** and tap **Always**.
4. Adjust the crop box using the corner brackets or edge handles.
5. Tap **Copy & Delete** $\rightarrow$ switch to any chat or notes app and tap **Paste**!

---

## 📥 Installation

Download the pre-compiled installer:
* **[SnapCrop.apk](SnapCrop.apk)** (~1.4 MB)

Install it using your file manager or transfer it to your phone using [LocalSend](https://localsend.org/) or ADB:
```bash
adb install SnapCrop.apk
```

---

## 🛠️ Building from Source

Prerequisites: JDK 17+ and Android SDK (API 34).

```bash
git clone https://github.com/YOUR_USERNAME/SnapCrop.git
cd SnapCrop
./gradlew assembleRelease
```
The output APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
