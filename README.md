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

The same build is also uploaded to Google Play's **internal testing** track (see [Google Play](#google-play)).

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

Android only installs an update if it's signed with the same key as the app already installed. The key is kept out of the repo. Any existing `.jks` or `.keystore` file works. Add it as repository secrets under **Settings > Secrets and variables > Actions**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The keystore file, base64-encoded (see below) |
| `ANDROID_KEYSTORE_PASSWORD` | The keystore's password |
| `ANDROID_KEY_ALIAS` | The key's alias. Only needed if it isn't `news-switcher` |
| `ANDROID_KEY_PASSWORD` | The key's own password. Only needed if it differs from the keystore password |

To base64-encode the file:

- macOS: `base64 -i my-key.jks | pbcopy` (copies it to the clipboard)
- Linux: `base64 -w0 my-key.jks`
- Windows: open PowerShell in the folder with the key (in File Explorer, type `powershell` in the address bar and press Enter), then run `[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\my-key.jks))) | Set-Clipboard`. It copies the result to the clipboard and prints nothing.
- Windows, from Command Prompt: `certutil -encodehex my-key.jks key-base64.txt 0x40000001`, then copy everything in `key-base64.txt`.

Keep a private backup of the keystore. If it's lost, the next APK won't install over the old one: uninstall the app and install it fresh. Builds run without the secrets still work, but they're signed with a throwaway key.

## Google Play

Every push to `main` uploads a Play bundle (`.aab`) to the **internal testing** track of the app `uk.rdfn.newsswitcher`. Testers you add in Play Console get updates from the Play Store. Promote a build to production in Play Console when you're ready.

What it needs:

- The repository secret `PLAY_SERVICE_ACCOUNT_JSON`: the JSON key of a Google Cloud service account that has been invited in Play Console (**Users and permissions**) with permission to release to testing tracks.
- The app's first bundle uploaded by hand. Google doesn't allow the API to make the first upload. Download `news-switcher-play-bundle` from the latest run in the Actions tab, unzip it, and upload `news-switcher.aab` to the internal testing track in Play Console.

Play App Signing: when Play asks for the app signing key on the first upload, choose to use your own key (the same `.jks` as above). Then copies installed from GitHub and from Play are signed the same way and update each other. If you let Google generate a key instead, the two are separate as far as Android is concerned, and you'd need to uninstall one before installing the other.

The package name `uk.rdfn.newsswitcher` is permanent once on Play.

## Building it yourself

Open the folder in Android Studio, or run `./gradlew assembleRelease` with the Android SDK installed. The APK ends up in `app/build/outputs/apk/release/`.

## Files

- `app/src/main/java/io/github/rednick/newsswitcher/MainActivity.kt`: the app. A full-screen web view of the switcher page, plus remote buttons, sound and screen settings.
- `app/src/main/assets/tv-remote.js`: added to the page so a TV remote can move between and press its buttons.
- `app/src/main/AndroidManifest.xml`: lets the app install on both phones and TVs.
- `app/src/main/res/`: app name, icon and TV banner.
- `.github/workflows/build-apk.yml`: builds the APK and Play bundle, publishes the APK as a release and uploads the bundle to Google Play.
