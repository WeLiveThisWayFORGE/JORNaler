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

The widget shows ideas and drafts, most recently updated first. Each row shows the title or first line and a small type and status label. Tap a row to open it in the editor. **+** opens quick capture. **Today** opens today’s journal. The widget refreshes when pages change, in light and dark.

The widget title uses the system serif. Home-screen widgets cannot load the app’s bundled Literata font.

## Install the debug APK

Build the debug APK, then install that file. It is signed with the Android debug key, so it is meant for sideloading, not the Play Store. There is no published GitHub release yet.

On a phone with USB debugging:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK onto the phone and open it with the Files app. Allow installs from that source if Android asks. A rebuild signed with the same debug keystore upgrades an existing install of `com.jornaler.app` and keeps the local notebook.

A debug build can also drop a few sample pages into an empty notebook:

```bash
adb shell am start -n com.jornaler.app/.MainActivity --ez seed true
```

## Build

You need JDK 17 or newer and the Android SDK (compile SDK 37, build-tools 36 or 37).

Create `local.properties` in the project root (this file is gitignored):

```
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

- Application id: `com.jornaler.app`
- minSdk 26, targetSdk 37
- Version 1.0.0

## Privacy

Pages live in a Room database on the device (`jornaler.db`). The manifest removes `INTERNET` and `ACCESS_NETWORK_STATE`. Android backup may include that database if the phone’s backup is turned on. The JSON export is the copy you can move yourself.

Glance schedules widget updates with WorkManager, which adds `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE`. Those permissions do not let the app open a network connection.

There is no in-app lock. The phone’s lock screen is what keeps the notebook private.

## Fonts

Literata, Nunito, and Fraunces are included under the SIL Open Font License. The license texts are in `third_party/fonts/`.
