# JORNaler

JORNaler is a private notebook for poems, journal pages, and quick ideas. It stays on the phone: no account, no analytics, and no internet permission. The home-screen widget is the point of it. Ideas and drafts sit where you can see them, and a new spark is one tap away.

The app is native Android: Kotlin, Jetpack Compose, Room, and Jetpack Glance. The launcher name, widget name, and in-app title are **JORNaler**. The application id is `com.jornaler.app`.

## Features

- **Three kinds of pages.** Poem, journal entry, and idea. Each has a body, created and updated times, and optional tags. Titles are optional. A journal with no title shows the date.
- **Status for poems and ideas.** Idea, Drafting, and Finished. Change it from the page or by tapping the chip on a card. Journal pages are dated and have no status.
- **Shelves.** In progress (ideas and drafts), Poems, Journal, and Finished. Search covers titles, text, tags, and moods. Pin a page to keep it at the top.
- **Editor.** Autosave, a reading serif, line breaks and indentation left as typed, and a word count and line count. Poems can be shared as plain text. The shared text ends with `— JORNaler`.
- **Journal.** “Write today” opens today’s page or starts it. A mood emoji is optional. The month calendar opens any day.
- **Quick capture.** The + button starts an idea by default, or a poem, or today’s journal.
- **Backup.** Export every page to one JSON file and import it on another phone. When the same page exists on both sides, the newer edit is kept.
- **Themes.** Warm paper colors, light and dark, or follow the phone. Literata for writing, Fraunces for the title, Nunito for the rest of the interface.

## Add the widget

1. Long-press an empty spot on the home screen.
2. Choose **Widgets**.
3. Find **JORNaler** and drag it onto the home screen.
4. Drag the edges to resize it. The list scrolls when there is more than fits.

The widget shows ideas and drafts, most recently updated first. Each row shows the title or first line and a small type and status label. Tap a row to open it in the editor. Tap the title, or the small portrait beside it, to open the app. **+** opens quick capture. **Today** opens today’s journal when the widget is at least 190dp wide; a narrower widget keeps the title, the portrait, and **+**. The portrait is a square crop of her face, 32dp on a narrow widget and 40dp on a wider one, in the title row on every size. The list and the controls keep the rest of the space, on the paper color. The widget refreshes when pages change, in light and dark.

The widget title uses the system serif. Home-screen widgets cannot load the app’s bundled Literata font.

## Opening screen

While pages are loading, the app shows the full supplied portrait, fitted so the frame and the woman stay in proportion. The gold around it is the color of that frame. Android 12 and later still draws the system splash first: a gold field and a circular crop of her face. That splash is not held open. There is no extra delay. As soon as the notebook is ready, the shelves replace the painting.

## Download

Two APKs are published. Both use application id `com.jornaler.app` and the label **JORNaler**.

Debug build [v1.0.0](https://github.com/WeLiveThisWayFORGE/JORNaler/releases/tag/v1.0.0), file `JORNaler-debug.apk`, signed with the Android debug certificate (`CN=Android Debug, O=Android, C=US`):

https://github.com/WeLiveThisWayFORGE/JORNaler/releases/download/v1.0.0/JORNaler-debug.apk

SHA-256: `68b3ae5eefa6772010070a61184f123f66fb3d27da936439083bfb539fe2be8b`

Debug signing-certificate SHA-256: `36B9A6DF55514D96D9A5830A6128B786F50FF0774E0C19556AEC07233B42235F`

Release build [v1.0.1](https://github.com/WeLiveThisWayFORGE/JORNaler/releases/tag/v1.0.1), file `JORNaler-1.0.1-release.apk`, signed with the permanent release certificate:

https://github.com/WeLiveThisWayFORGE/JORNaler/releases/download/v1.0.1/JORNaler-1.0.1-release.apk

SHA-256: `6d1ee6bef77d9fa0f7549406908129a5815a6e24cad2f789b8cfbe647e47582e`

Signing-certificate SHA-256: `4F2940A94A564C016F003A881527305727CBF5980967950910FA1162CD85D50D`

This source is version 1.0.3. A signed 1.0.3 APK is not published.

On a phone with USB debugging:

```bash
adb install -r JORNaler-debug.apk
```

Or copy the APK onto the phone and open it with the Files app. Allow installs from that source if Android asks. Sideloading can still show that warning for a later release-signed APK as well, because this app is not installed from the Play Store.

An upgrade of this debug install has to be signed with this same debug certificate. A debug build made on another computer uses that computer’s own debug key and cannot replace this install. The notebook stays on the phone when the certificate matches.

A debug build can also drop a few sample pages into an empty notebook. A release build ignores this:

```bash
adb shell am start -n com.jornaler.app/.MainActivity --ez seed true
```

## Move from the debug app to a release-signed install

The debug app and a release-signed app use different certificates, so Android will not update one over the other. The notebook lives only in the debug app until you export it. Uninstalling deletes it. Android’s own backup is tied to the old certificate and will not restore those pages into the release install.

Do this before you uninstall:

1. Open the installed debug app.
2. On the home screen, open the **More** menu.
3. Tap **Export backup**.
4. Save `jornaler-backup.json` somewhere that survives an uninstall, such as Downloads or a computer. The app confirms with “Backup saved on this phone.”
5. Leave JORNaler and confirm that file is still there in Files.
6. Only then uninstall JORNaler (`com.jornaler.app`).

Then install the release-signed APK and import:

1. Install the published [v1.0.1 release APK](https://github.com/WeLiveThisWayFORGE/JORNaler/releases/download/v1.0.1/JORNaler-1.0.1-release.apk). A later build signed with that same certificate can replace it and keep the notebook.
2. Open JORNaler, open **More**, and tap **Import backup**.
3. Choose the JSON file you saved. The app reports how many pages were added, updated, or kept.

Later builds signed with that same release key install over the release app and keep the notebook. You do not export again for those updates. The first move off the debug certificate is the one that requires the backup. Android can still warn about sideloading those updates.

## Build

You need JDK 17 or newer and the Android SDK (compile SDK 37, build-tools 36 or 37).

Create `local.properties` in the project root (this file is gitignored):

```
sdk.dir=/path/to/Android/Sdk
```

Debug APK and unit tests:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Gradle signs it with the debug keystore on the machine that runs the build. That signature matches the v1.0.0 download only when the keystore is the one that signed the published APK.

Release APK:

```bash
./gradlew :app:testReleaseUnitTest :app:assembleRelease
```

With no signing environment, that writes an unsigned APK to `app/build/outputs/apk/release/app-release-unsigned.apk`. It is not installable, and it is not a published release. The release build type is not debuggable and does not use the debug keystore.

To sign with the permanent release key, set these in the environment for that command. Do not commit them.

- `JORNALER_KEYSTORE_PATH` — path to the keystore file
- `JORNALER_KEYSTORE_PASSWORD` — keystore password
- `JORNALER_KEY_ALIAS` — key alias
- `JORNALER_KEY_PASSWORD` — key password, only if it differs from the keystore password

All of the path, keystore password, and alias are required together. If one is set and another is missing, the build fails instead of writing an unsigned or debug-signed APK. When they are set, the signed file is `app/build/outputs/apk/release/app-release.apk`. This repository does not contain a keystore or a signing password. The signed file that is published is the v1.0.1 release above, not a 1.0.3 build.

The unit tests cover the notebook. `ReleaseBehaviorTest` checks that a non-debuggable build does not seed sample pages. `ReleaseBuildTest` runs with the release unit tests and checks that the release variant is not debuggable.

- Application id: `com.jornaler.app`
- Launcher and widget label: JORNaler
- minSdk 26, targetSdk 37
- Version 1.0.3 (versionCode 4). versionCode 4 is what lets a build signed with the same release certificate upgrade the 1.0.2 install.

## Privacy

Pages live in a Room database on the device (`jornaler.db`). The manifest removes `INTERNET` and `ACCESS_NETWORK_STATE`. Android backup may include that database if the phone’s backup is turned on. The JSON export is the copy you can move yourself.

Glance schedules widget updates with WorkManager, which adds `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE`. Those permissions do not let the app open a network connection.

There is no in-app lock. The phone’s lock screen is what keeps the notebook private.

## Fonts

Literata, Nunito, and Fraunces are included under the SIL Open Font License. The license texts are in `third_party/fonts/`.
