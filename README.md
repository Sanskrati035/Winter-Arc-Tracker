<div align="center">

# Frostline

### Your 90-day winter challenge, in your pocket.

Track habits, drink more water, build streaks, earn points, and finish the winter stronger than you started it.

![Platform](https://img.shields.io/badge/platform-Android%20%7C%20Web-5b7bb0)
![Works offline](https://img.shields.io/badge/works-offline-8fb3cc)
![No account](https://img.shields.io/badge/no%20account-needed-b3a6d4)
![License](https://img.shields.io/badge/license-MIT-e6cc9c)

</div>

---

## What is Frostline?

Frostline is a calm, good-looking tracker for a **90-day winter challenge**. Set your own goals, tick them off each day, log your water, and watch your streak grow. It runs entirely on your phone. There is no sign-up, no ads, and no server.

## Features

| | |
|---|---|
| **Today** | See where you are in the 90 days and tick off today's goals |
| **Streaks and points** | Build a streak, earn points, and unlock achievement medals |
| **Water tracker** | Log what you drink against your daily goal |
| **Rest days** | Recovery counts too. Rest days never break your streak |
| **Progress** | Charts for daily completion, goal consistency and water intake |
| **Booth** | Take or pick a photo, add a Frostline frame with your day, streak and points, then save or share it |
| **Themes** | Several looks, from frosty dark to warm day and mono |
| **Offline** | Works without internet once installed |
| **Private** | Your progress stays on your own phone |

## Install on Android

### Option 1: APK (full app with a home-screen widget)
1. Open the **Actions** tab of this repository.
2. Click the latest green run, then download **Frostline-apk** from *Artifacts*.
3. Unzip it to get `app-debug.apk`, and send it to your phone.
4. Tap the file. If Android asks, allow installs from this source, then tap **Install**.
5. Long-press your home screen, choose **Widgets**, and drag **Frostline** onto it.

> The first install may show a Play Protect warning because the app is not from the Play Store. Choose **Install anyway**.

### Option 2: Web app (PWA)
Open the hosted link in Chrome, tap the three-dot menu, then **Install app**.
On iPhone: open it in Safari, tap **Share**, then **Add to Home Screen**.

## Project structure

```
Frostline/
├── .github/workflows/build-apk.yml   # builds the APK on GitHub
├── app/
│   └── src/main/
│       ├── assets/www/               # the web app (index.html, Chart.js, icons)
│       ├── java/com/frostline/app/   # WebView shell, camera, gallery, share, widget
│       └── res/                      # icons, widget layout, file provider paths
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Privacy

Frostline has no accounts, no analytics, and no tracking. Everything you enter is saved in the app's local storage on your device. Clearing the app's data or uninstalling it erases your progress, and the app asks for camera access only so you can take photos in the Booth.

## License

Released under the MIT License. Feel free to use, change and share it.

---

<div align="center">

Made for the cold months. Stay warm, stay consistent.

</div>
