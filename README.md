# Jornaler

A private notebook for poems, journal pages, and quick ideas. Everything stays on the phone. There is no account, no network access, and no analytics.

Jornaler is a native Android app (Kotlin, Jetpack Compose, Room, Glance). The home-screen widget shows work in progress, the way a task list sits on a home screen, but for writing.

## What you can do

- **Poems, journal pages, and ideas.** A poem or idea moves from Idea to Drafting to Finished. A journal page is dated and does not use a status. Titles are optional; a journal with no title uses the date.
- **Shelves.** In progress (ideas and drafts), Poems, Journal, and Finished. Search looks through titles, text, tags, and mood. Pin a page to keep it at the top.
- **A quiet editor.** Autosave, a serif made for reading, line breaks and indentation left as you typed them, and a word and line count.
- **Journal.** “Write today” opens today’s page or starts it. Add a mood if you want. The month calendar opens any day.
- **Quick capture.** The + button starts an idea (or a poem, or today’s journal) without walking through the whole editor first.
- **Backup.** Export every page to one JSON file, and import it on another phone. Newer edits are kept when the same page exists on both sides. Share a poem as plain text.

## Add the widget

1. Long-press an empty spot on the home screen.
2. Choose **Widgets**.
3. Find **Jornaler** and drag it onto the home screen.
4. Drag the edges to resize it. It scrolls if there are more pages than fit.

The list is ideas and drafts, most recently updated first. Tap a row to open it. **+** starts a quick capture (an idea, unless you pick something else). **Today** opens today’s journal. The widget refreshes when you edit, pin, finish, or delete a page, in both light and dark.

## Build

You need JDK 17+ and the Android SDK (compile SDK 37, build-tools 36 or 37). Point the SDK at the project with `local.properties`:

```
sdk.dir=/path/to/Android/Sdk
```

```
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

A debug build can drop a few sample pages into an empty notebook:

```
adb shell am start -n com.jornaler.app/.MainActivity --ez seed true
```

- Application id: `com.jornaler.app`
- minSdk 26, targetSdk 37
- Debug package is sideloadable and signed with the debug keystore

## Privacy

The manifest strips `INTERNET` and `ACCESS_NETWORK_STATE`. Pages live in a Room database on the device (`jornaler.db`). Android backup may include that database if the phone’s backup is turned on. The JSON export is the copy you can move yourself.

Glance schedules widget updates with WorkManager, which adds `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE`. Those do not let the app open a network connection.

## Fonts

Literata, Nunito, and Fraunces are included under the SIL Open Font License. See `third_party/fonts/`.
