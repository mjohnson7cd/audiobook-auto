# Android Automotive OS (Google built-in)

Audiobook Auto ships a separate **automotive** build for cars that run Android Automotive OS
(AAOS) with Google built-in, such as the 2026 Chevrolet Silverado EV. These cars have no Android
Auto phone projection: the app installs from the car's own Play Store and runs on the head unit.

## Build variants

The `formfactor` flavor dimension (`app/build.gradle.kts`) has two flavors:

| Flavor | Output | versionCode | Notes |
|---|---|---|---|
| `mobile` | `app-mobile-*.apk/.aab` | `69` | Phone app + Android Auto projection |
| `automotive` | `app-automotive-*.apk/.aab` | `1000069` (`1_000_000 + mobile`) | AAOS only, no launcher |

Both flavors share the `applicationId` (one Play listing). The automotive build must go on the
dedicated **Android Automotive OS only** Play track, and its versionCode has to differ from the
mobile one.

```bash
./gradlew :app:assembleMobileDebug :app:assembleAutomotiveDebug
./gradlew :app:bundleMobileRelease :app:bundleAutomotiveRelease
./gradlew :app:testMobileDebugUnitTest :app:testAutomotiveDebugUnitTest
# Publish (Gradle Play Publisher). A --track flag overrides the automotive default
./gradlew publishAutomotiveReleaseBundle --track=automotive:internal
```

`BuildConfig.IS_AUTOMOTIVE` is `true` only in the automotive flavor.

## Manifest (`app/src/automotive/AndroidManifest.xml`)

This file is merged over the main manifest. It:

- requires `android.hardware.type.automotive` and marks wifi, screen orientations, touchscreen,
  camera, and bluetooth as not required
- sets `android:appCategory="audio"` and adds the `com.android.automotive` meta-data
  (`@xml/automotive_app_desc`)
- removes the Android Auto projection meta-data (`com.google.android.gms.car.*`) and the Cast
  options provider
- keeps `MainActivity` (shared code references it), but unexported and with no launcher intent
  filter
- removes `AuthReturnActivity`, because the car has no browser OAuth
- declares `CarSignInActivity` and `CarSettingsActivity`. Both are **parked-only**: they have no
  `distractionOptimized` meta-data, so the system blocks them while the car is driving

## In-car sign-in (`features/car/CarSignInActivity`)

1. Creates a **short** Plex PIN (`PlexLoginService.postLinkPin`, `strong=false`) through
   `PlexAuthCoordinator(..., LINK_CODE_TIMEOUT_MS, useLinkCode = true)`.
2. Shows the 4-character code in large type with "go to **plex.tv/link**". The user enters the
   code on a phone or computer. The app polls for up to 10 minutes, and "Get a new code" restarts
   the flow.
3. Walks through user → server → library. Any step with only one choice is picked automatically.
   Protected Plex Home users get a PIN prompt.
4. Starts the first library sync, then shows "Signed in".

Each step comes from `IPlexLoginRepo.loginEvent`, so a sign-in that was interrupted resumes at
the right step.

**Entry points:** the media center's error "Sign in" button (see below) and Car settings.

## Car settings (`features/car/CarSettingsActivity`)

The media center's settings button opens this screen through the
`android.intent.action.APPLICATION_PREFERENCES` intent. It offers:

- sign in, switch server/library, sync now, sign out
- skip forward/back intervals and playback speed (tap a row to cycle through its values)

It also serves as the MediaSession activity on AAOS, because there is no launcher activity.

## MediaPlayerService behaviour on AAOS

- While the user isn't fully signed in, error playback states carry
  `android.media.extras.ERROR_RESOLUTION_ACTION_LABEL/_INTENT` ("Sign in" →
  `CarSignInActivity`). See `features/car/CarErrorResolution.kt`. `onGetRoot` sets this error
  right away, so a fresh install shows the Sign in button on first open.
- The service syncs the library itself when the server connects and the local library is empty
  or older than the refresh rate, because the car has no Home screen to start a sync.
- `prefsRepo.allowAuto` is always `true`.
- Messages that tell the user to "use your phone" are overridden in
  `src/automotive/res/values/strings.xml`.

Flavor hooks live in `features/car/CarIntegration.kt`: the `src/mobile` copy is a no-op and the
`src/automotive` copy returns the car activities.

## Testing

- Generic **Automotive with Google Play** emulator (API 34/35), then the GM Google built-in
  emulator (developer.gm.com). Run config: *Launch: Nothing*.
- Clear the app data, then open it from the media center. It must show the Sign in button, and
  the code flow must complete.
- Extended controls → Car data → Drive with speed > 0. Sign-in and settings must be blocked, and
  playback must continue.
