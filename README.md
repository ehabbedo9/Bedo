# Daily Bible lock (قراءتي اليومية)

Android app (Kotlin) that locks Facebook, Instagram and Twitter/X every day until
the chosen number of Bible chapters has been read. Arabic, right-to-left, yellow and brown.

## Build

1. Install Android Studio (Koala 2024.1 or newer) on a computer.
2. File > Open, choose this folder, and wait for the Gradle sync (it downloads
   Gradle 8.7 and the Android Gradle plugin 8.5.2).
3. Run on a phone (Android 8.0 or newer), or Build > Build APK(s).

## How it works

- First launch: choose the daily number of chapters (default 3) and where to start, then
  the apps to lock, then a guided screen for the two permissions ("Display over other
  apps" and "Usage access") that returns to the app by itself once each is turned on.
- `LockService` runs as a foreground service, checks which app is in front twice a
  second, and covers a locked app with a full-screen overlay until the reading is done.
- A chapter counts only after scrolling to its end and staying a minimum time
  (0.25 s per word, between 30 s and 5 min). Change these in `ReaderActivity`.
- Reading runs in order from the book and chapter the user picks (Genesis 1 by default);
  the day resets at local midnight.
- Changing the daily number applies from the next day. The locked-app list can only
  be edited after today's reading is complete.

## Where things are

- `app/src/main/java/com/dailybible/lock/` - all code
  - `LockedApps.kt` - the list of locked apps and their package names
  - `Prefs.kt` - saved state (target, position, streak)
  - `LockService.kt` - detection and overlay
- `app/src/main/assets/bible/` - Arabic Smith & Van Dyck Bible with full tashkeel (public domain, eBible.org arb-vd), one file per book
- `app/src/main/res/` - layouts, colours, fonts (Cairo and Amiri, SIL Open Font License; see `licenses/`)

## Known limits

- The lock covers the apps only, not the same sites opened in a browser.
- Someone can still switch the lock off by revoking the permissions or uninstalling the app.
- Some phones (Xiaomi, Oppo, Huawei) stop background services; allow "autostart" and
  remove battery restrictions for the app on those phones.
