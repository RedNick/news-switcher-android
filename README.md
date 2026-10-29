# News Switcher for Android

The [hourly news switcher](https://rednick.github.io/news-switcher/) as an app for Android phones, tablets and Android TV. It plays a 24/7 live news stream full screen and moves to the next channel one minute before every hour.

The app shows the live website, so channel changes and new features made to the [web version](https://github.com/RedNick/news-switcher) appear in the app without reinstalling it.

What the app adds on top of the website:

- Starts with the sound on (browsers force the page to start muted).
- The screen never sleeps while it's open.
- No status bar or navigation bar.
- Shows up on the Android TV home screen and works with a TV remote.

## Download

**Latest APK:** https://github.com/RedNick/news-switcher-android/releases/latest/download/news-switcher.apk

A new APK is built automatically every time this repo's `main` branch changes. Installing a newer one over the old one keeps your settings.

## Installing on a phone or tablet

1. Open the download link above on the phone.
2. Open the downloaded `news-switcher.apk`.
3. If Android asks, allow your browser to **install unknown apps**, then go back and tap **Install**.

## Installing on Android TV (including Google TV)

Most TVs have no web browser, so use the free **Downloader** app (by AFTVnews):

1. On the TV, install **Downloader** from the Play Store.
2. Allow it to install apps: **Settings > Apps > Security & restrictions > Unknown sources > Downloader** (the exact menu names vary by TV).
3. Open Downloader, type the download link above into the address box and press **Go**.
4. Choose **Install** when the download finishes.

News Switcher then appears in the TV's apps row.

## TV remote buttons

| Button | What it does |
| --- | --- |
| Left / Right (or Channel −/+) | Previous / next channel |
| Up / Down | Move the highlight between on-screen buttons and settings |
| OK | Press the highlighted button. With nothing highlighted, show/hide the extras |
| Menu | Open the settings menu |
| Back | Close the menu. Press twice to exit |

On a phone, use the on-screen buttons as you would on the website.

## Signing key

Android only installs an update if it's signed with the same key as the app already installed. The key is kept out of the repo and stored as two repository secrets (**Settings > Secrets and variables > Actions**):

- `SIGNING_KEYSTORE_BASE64`: the keystore file, base64-encoded
- `SIGNING_KEYSTORE_PASSWORD`: its password (the key alias is `news-switcher`)

Keep a private backup of the keystore. If it's lost, the next APK won't install over the old one: uninstall the app and install it fresh. Builds run without the secrets still work, but they're signed with a throwaway key.

## Building it yourself

Open the folder in Android Studio, or run `./gradlew assembleRelease` with the Android SDK installed. The APK ends up in `app/build/outputs/apk/release/`.

## Files

- `app/src/main/java/io/github/rednick/newsswitcher/MainActivity.kt`: the app. A full-screen web view of the switcher page, plus remote buttons, sound and screen settings.
- `app/src/main/assets/tv-remote.js`: added to the page so a TV remote can move between and press its buttons.
- `app/src/main/AndroidManifest.xml`: lets the app install on both phones and TVs.
- `app/src/main/res/`: app name, icon and TV banner.
- `.github/workflows/build-apk.yml`: builds and publishes the APK.
