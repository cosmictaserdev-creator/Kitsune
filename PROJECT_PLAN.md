# Whispry Keys + Whispry: project direction and implementation handoff

Updated: 16 November 2026.

This is the current project handoff. It supersedes conflicting details in the earlier
`WHISPRY_KEYBOARD_PROPOSAL.md` and `WHISPRY_KEYS_BRIDGE_PLAN.md` documents in Documents.
It is mirrored as `PROJECT_PLAN.md` in both application repositories; update both copies together.

## 1. Product goal

Build **Whispry Keys**, a separately installed Android keyboard derived from FrostKeys, with
voice typing powered by the separate **Whispry** application. Ordinary keyboard typing remains
usable without Whispry. Whispry owns recording, provider connections, API keys, processing,
entitlement and transcript history; the keyboard owns its interface, editor insertion and clipboard.

Keep the main application named Whispry. Rename only the keyboard companion to Whispry Keys.
Use the user's Jetpack SF Symbols library and Compose HIG for settings and supporting controls.

**Immediate priority:** get basic voice typing verified between both installed apps. Tones and
additional writing/context tools are deliberately deferred until the recording bridge is stable.

## 2. Workspaces and branches

| Project | Local directory | Branch / status |
| --- | --- | --- |
| Keyboard | `/Users/aryan/Documents/FrostKeys` | `feat/hig-settings`; local Git repository initialized from the downloaded source export |
| Main app | `/Users/aryan/Documents/whispry-git` | `feat/keyboard-dictation-bridge`, based on `feat/monetization-hardening` |
| Old app export | `/Users/aryan/Documents/whispry` | Original downloaded master export; retain as a reference, not the integration baseline |
| Upstream HIG reference | `/Users/aryan/Documents/compose-hig` | Cloned reference; keyboard consumes its own adapted `hig/` module |
| Symbol library reference | `/Users/aryan/Documents/Jetpack_SF_Symbols` | Cloned reference; app dependency currently pinned to `1.0.4` |

Whispry's `feat/monetization-hardening` baseline is commit `5de989c`, dated 2 October 2026. It has
the proprietary license declaration, meetings, billing and dictionary features missing from master.
The `feature/ai-edition`, `feature/i18n-and-verification`, `feature/landscape`, and `notifications`
branches were confirmed to be ancestors of master. Do not redo the integration against the old export.

Both implementation workspaces currently contain **uncommitted changes**. Preserve them.
The keyboard has a local baseline commit, but no remote is configured. A GitHub fork of the actual
FrostKeys source has not been completed by this work. An existing account repository named HeliBoard
is not evidence that the FrostKeys changes have been forked. Do not overwrite or push to it blindly.

## 3. Agreed interaction design

### Keyboard top row

- The idle keyboard includes a microphone action in its top toolbar.
- On activation, ordinary toolbar items move away toward the physical right edge with a natural,
  damped spring and shallow curved motion. Preserve toolbar height so the host app does not jump.
- The freed top-row area becomes the Whispry voice interface: sound-reactive glow, recording/status
  text, Cancel, Pause/Resume and a Finish action.
- The glow responds to real microphone amplitude. Transcription currently starts after recording
  stops; do not imply that the existing provider pipeline provides live partial transcripts.
- Pause preserves the same take; Resume continues it. Cancel discards the active session and
  ignores late processing results.
- **Enter means finish, transcribe/format, insert and copy. It must not send the message, invoke
  the editor's Send action, or add a newline as part of finishing dictation.**
- Keep normal Enter behavior outside a voice session.

### No floating companion UI

- Whispry companion controls belong **inside the keyboard**, not in a floating overlay.
- Do not create Whispry's floating pill or keyboard-riding logo for companion sessions.
- Proposed hidden-keyboard behavior: continue the explicitly active session with notification
  controls; reopening the keyboard reattaches to the same session.
- Notification and microphone permissions must have real setup/recovery flows. A denied notification
  permission must not be mistaken for having reachable notification controls.
- Existing standalone Whispry triggers are a separate mode. Companion recording does not require
  accessibility or overlay access merely to record and insert text.

### Double-tap tools panel: later milestone

- A confirmed single tap starts dictation; double-tap must not accidentally start a short recording.
- Double-tap on the top-row microphone toggles a tools panel over the lower keyboard area.
- Blur/dim and deactivate the keys underneath. Keep the top-row anchor and panel controls outside
  that disabled layer. Older devices use a solid/dimmed treatment without blur.
- Context tools eventually include Tones, Translate, Meetings, History, Snippets, Dictionary,
  Memory, My Info and App Tones, backed by Whispry's existing data and entitlement rules.
- A second double-tap, the **panel Close button**, or Back reverses the animation and restores keys.
- **Panel Close dismisses tools; recording Cancel cancels recording. These are different actions.**
- Opening/closing tools must not start, stop or recreate a recording. Session state and panel state
  are independent. Restore the active voice row when closing tools during recording.
- Provide explicit accessible actions so double-tap is not the only route to tools.

### Missing or incompatible Whispry

- Both microphone gestures must explain that Whispry is required and offer installation/setup.
- Preserve ordinary typing. Production installation goes to the official Play listing.
- Handle outdated bridge versions, disabled apps and signing mismatches with actionable messages.
- The keyboard supports Android 7; voice functionality needs Android 8 or later because of Whispry's
  minimum SDK. Add an explicit compatibility gate instead of offering an impossible installation.

## 4. Settings design and future themes

Settings now use a HIG-based shell with bottom tabs: **Keyboard, Themes, Whispry, Settings**.
The migration includes shared typography, search, switches, buttons, surfaces, text fields and
dialogs, with existing specialized settings preserved behind compatibility adapters.

- Keyboard: languages/layouts, typing, feedback, toolbar, gestures, dictionaries, number/symbol layouts,
  and a built-in text field for testing.
- Themes: Included, My themes, Collections; mini keyboard previews resolve the actual keyboard palettes.
  Theme selection uses existing preferences; the existing detailed appearance editor remains available.
- Whispry: detects and opens the configured companion package. These settings links are not the
  implementation of all in-keyboard tools.
- Settings: reduced transparency, setup, advanced options, connected services and credits.

The adapted `hig/` module has an Android 7 minimum and an `AdaptiveBackdrop` boundary. Android below
12, software rendering, or reduced-transparency mode uses opaque surfaces without evaluating the
glass effect callbacks. Glass is a rendering enhancement, not a requirement to use the controls.

The Collections catalog is currently empty. No original paid assets or Play product catalog has
been supplied, and no checkout is implemented. Do not fabricate prices, products or entitlements.

Future paid themes must separate original independently licensed artwork/content from the GPL
keyboard and renderer. GPL-derived theme code cannot simply be made proprietary. Validate licensing
for the actual pack contents; a file format alone does not settle the issue. Plan Play one-time
products, verified/restorable purchases and separately controlled asset delivery. Do not promise
that assets delivered to a user's device are impossible to extract.

See `docs/THEME_COLLECTIONS.md` in the keyboard project. HIG's Apache notices and the symbol library's
MIT code license/glyph-rights disclaimer have been retained. Review the underlying symbol rights
before Play distribution; the wrapper license does not itself establish those rights.

## 5. Architecture and abstractions

Use feature-scoped unidirectional state flow, Kotlin coroutines/Flow and existing injection.
Do not rewrite FrostKeys' whole Java/View keyboard. Use its existing composition/input machinery
and Compose integration points. Do not duplicate Whispry's provider or entitlement logic in the keyboard.

```text
Keyboard mic / pause / finish / cancel
                  |
     WhispryKeyboardController + VoiceRow
                  |
       versioned AIDL contract / callbacks
                  |
          KeyboardBridgeService
                  |
             KeyboardSession
                  |
  existing recorder, settings, provider repositories,
  transcript processing and transcript storage

Result -> keyboard editor adapter -> field insertion + clipboard
```

| Boundary | Responsibility |
| --- | --- |
| Session controller | Authoritative recording and processing state, one recording, cancellation and recovery |
| Foreground service | Active lifetime and notification controls; client binding alone does not own recording |
| AIDL adapter | Trust checks, protocol mapping, commands and callbacks |
| Keyboard controller | Connection state, user actions, rendering state and current-editor validation |
| Voice row / later panel | Render state and emit intents; animations never control the microphone lifecycle |
| Editor delivery | Commit text, copy the same text, deduplicate callbacks, report each operation separately |

Keep session state separate from local panel state. Use interfaces at real external/hardware/editor
boundaries and reuse existing repositories. Avoid a universal event bus, generic base repositories,
or a new abstraction for every trivial button.

### Current contract

Both apps contain the same independently authored MIT-licensed AIDL files under
`app/src/main/aidl/com/cosmictaser/whispry/bridge/`:

- `connect(callback)` -> snapshot; `disconnect(callback)`.
- `prepareStart(targetPackage)` -> immutable recording-start PendingIntent.
- `prepareRetry(sessionId)` -> immutable processing-retry PendingIntent.
- `command(sessionId, action)` -> Pause/Resume toggle, Finish or Cancel.
- `acknowledge(sessionId, resultId)` -> clear only the matching delivered result.
- Callback `onState(Bundle)`; protocol version is currently 1.

Snapshots contain session identity, phase, destination package, message, amplitude, elapsed time,
pending text/result identity, an error category, microphone permission and retry availability.
API keys never cross this contract.

Authentication currently requires a signature permission, the expected keyboard package and
matching signing certificates. This works for the paired local debug builds. Before production,
arrange compatible Play signing keys or deliberately redesign the trust/authorization mechanism;
do not remove trust checks to make mismatched builds connect.

## 6. Implemented bridge code

### Whispry

New implementation is in `app/src/main/java/com/example/whispry/bridge/`:

- `KeyboardSession.kt`: app-owned recording state, existing AudioRecorder reuse, pause/resume,
  finish/cancel/retry, configured transcription provider, optional existing processing, history save,
  persisted pending result and stale-session/result checks.
- `KeyboardBridgeService.kt`: authenticated AIDL binding, active foreground lifetime, recording and
  processing start PendingIntents, notifications and state callbacks. It does not create an overlay.
- `KeyboardRecordingActivity.kt`: visible setup/start fallback for microphone permissions and
  devices that reject a background microphone start.
- Companion mode cancels the watchdog and adds guards to application scheduling, boot handling and
  the watchdog worker. It does not require a permanent idle recording service.

Current phases: IDLE, PREPARED, STARTING, LISTENING, PAUSED, TRANSCRIBING, FORMATTING, READY, ERROR,
CANCELLED, COMPLETED. Interrupted capture is restored as an error, never silently resumed.

### Keyboard

New implementation is in `app/src/main/java/helium314/keyboard/latin/whispry/`:

- `WhispryKeyboardController.kt`: authenticated bridge client, connection/reconnection, recording
  actions, destination generation tracking, result insertion/copying and delivery acknowledgement.
- `VoiceRow.kt`: top-row status/glow, Cancel, Pause/Resume, Finish, Retry and explicit Insert controls.
- `LatinIME.java`: hooks input/lifecycle events, routes the voice action to Whispry and intercepts
  Enter during voice sessions. Existing normal keyboard text insertion machinery is reused.
- The voice row replaces the top suggestion/toolbar area. The current spring moves the toolbar as
  a group; the requested individual staggered curved animation is still a refinement.

There is no completed double-tap context menu or new tones interface yet; the user deferred those
tools until the core bridge is working.

## 7. Test installation and signing decision

The phone's original `com.cosmictaser.whispry` app has a different signing key from the local build.
An in-place update was rejected by Android. **Do not uninstall it or clear its data.**

The user explicitly chose a separate test app and configured its provider key locally.

| Installed test app | Package | Build |
| --- | --- | --- |
| Whispry Bridge | `com.cosmictaser.whispry.bridge` | Debug, versionCode 10300 / versionName 1.3.0 |
| Whispry Keys | `com.cosmictaser.whispry.keys.debug` | Debug, versionCode 1 / versionName 2.5.9 |

The keyboard test build targets the Bridge package through `BuildConfig.WHISPRY_PACKAGE`.
The default build configuration still targets the production Whispry package unless overridden.
The Bridge package's application label and signature permission are derived from its build ID.

Device used: Samsung SM-M346B, Android 16 / API 36, arm64. Wireless ADB was successfully paired
and connected. Discover its current serial/port with `adb devices -l`; wireless ports can change.
No pairing codes, API keys or other credentials belong in this document.

At the last permission check, microphone access was granted to the Bridge app, but notification
permission was not granted. Recheck and establish notification controls before testing continued
recording with the keyboard hidden.

## 8. Verification completed and limits

Completed:

- Both debug APKs built successfully and the paired test builds were installed on the phone.
- The redesigned settings app was launched and inspected on the phone without an observed launch crash.
- Keyboard signature permission for the Bridge app was confirmed granted.
- Whispry session tests: 3 passed, covering missing-key prevention of capture, duplicate Finish
  uploading/saving only once with matching acknowledgement, and a late provider result after Cancel.
- HIG solid-surface tests: 3 passed across SDK 24, 30 and 34, exercising interactive controls while
  the glass effect callback must not execute in forced solid mode.
- The user reported completing the separate Bridge app's provider setup.

**Not yet verified:** a successful real phone recording -> provider transcription -> insertion
into the intended field -> clipboard delivery. A phone test was being prepared when the user
requested this written handoff. Do not label the complete bridge “working end to end” yet.

Also not yet verified on the phone: Pause/Resume, notification Finish/Cancel, hidden-keyboard
continuation, retry, interruption recovery, and a real foreground-start fallback.

Test XML evidence:

- Whispry: `app/build/test-results/testDebugUnitTest/TEST-com.example.whispry.bridge.KeyboardSessionTest.xml`.
- Keyboard HIG: `hig/build/test-results/testDebugUnitTest/TEST-zone.ien.hig.compat.SolidSurfaceTest.xml`.

Temporary shell logs are not durable evidence and may no longer exist. Keep a fresh test report
inside the project once the real device acceptance checks are finished.

## 9. Next engineering work, in order

### P0: verify and harden the core bridge

1. Coordinate a short phone test with the user. Open Whispry Keys' own test field, verify the
   foreground app before each automated interaction, then tap the keyboard microphone. Do not
   type or test in messaging apps or capture unrelated screens while the user is using the phone.
2. Confirm the signature-bound service connects, then check real microphone startup. Validate the
   PendingIntent path on Samsung API 36; if rejected, verify the visible Whispry start activity.
3. Ask the user to speak a harmless phrase. Finish with Enter. Confirm the same final text is in
   the intended test field, clipboard and Bridge history, with no Send/newline side effect.
4. Test pause/resume and cancel. Verify there is one recorder, no late result after cancel and no
   floating companion UI. Check that capture stops and the foreground service exits when complete.
5. Test closing/reopening the keyboard during recording and processing, including notification
   actions. Preserve a pending result instead of inserting into a different field.
6. Recheck editor identity around asynchronous startup. `begin()` currently captures the target
   package before the remote call but assigns `requestGeneration` after it returns. Freeze both
   together and cancel preparation if the editor changes before capture starts. Otherwise a fast
   switch between fields in the same app can be incorrectly accepted.
7. Tighten reconnection behavior: an old ERROR with retry audio must not be discarded by an
   automatic new Start request; transient IDLE/PREPARED callbacks must not flash the old toolbar
   or regress the local starting state.
8. Audit insertion reporting: `LatinIME.onTextInput()` does not return a host commit-success flag.
   Do not acknowledge a failed/rejected editor commit as successful. Keep insertion and clipboard
   outcomes independent; retrying a copy must never insert the text a second time.
9. Finish the companion-only lifecycle audit, including pending task-removal/restart paths and
   notification permission recovery. Do not disable the original app's existing triggers silently.

### P1: failure cases and persistence

- Offline and timeout; invalid API key/model/endpoint; provider rate limits and server errors.
- Missing/disabled/uninstalled app, revoked permissions, microphone busy and active meeting capture.
- Rapid repeated mic/Finish/Cancel actions, Binder death, rotation and keyboard/host process death.
- Preserve finalized retry audio privately. Implement the planned 24-hour retry-cache maximum and
  distinguish finalized retry files from interrupted/corrupt captures. Current success/cancel/new
  session cleanup exists, but complete expiry/recovery validation is not finished.
- Introduce structured app-owned failure categories and permitted recovery actions. Currently
  provider messages are forwarded with broad categories; not every failure has a distinct action.
- Preserve raw text visibly when formatting fails; do not silently label a failed translation as
  successful. Avoid hidden retries in both apps or automatic provider switching.
- Ensure entitlement changes and retries do not consume usage/meeting credits more than once.
- Add explicit minimum-Android and missing-companion setup screens.

### P2: visual and context-tool completion

- Finish interruption-safe per-item toolbar departure/return with curved staggered springs.
- Add single/double-tap disambiguation and the blurred noninteractive-key tools panel.
- Add tones, translation and the other context tools incrementally through the app-owned bridge.
- Preserve local-first meetings: Save recording is not automatic upload, insertion or copying.
- Add accessible controls, reduced-motion behavior, RTL, one-handed/split and light/dark checks.

### P3: release preparation

- Establish the correct keyboard remote/fork and commit reviewed source changes separately from
  build artifacts, logs and private device captures. Backend `hig/build/` is currently untracked
  generated output; keep it out of commits.
- Set deliberate monotonic keyboard release versioning; the source-export Git baseline currently
  makes the keyboard debug versionCode 1.
- Align supported compile SDK and Android Gradle plugin versions. Current keyboard builds use
  compile SDK 37 on AGP 8.13.2 with a compatibility warning; evaluate the plugin upgrade before release.
- Resolve production signing/trust and verify update behavior without data loss.
- Complete Play/internal testing, accurate privacy/data disclosures and applicable source/notices.
- Add real paid theme artwork, products, verified billing and restore flows only when supplied.

## 10. Rebuild and install the paired test apps

Use Java 21 and the installed Android SDK. On this Mac:

```sh
export JAVA_HOME=/Users/aryan/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export ANDROID_HOME=/Users/aryan/Library/Android/sdk
```

In `/Users/aryan/Documents/whispry-git`:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest \
  --tests 'com.example.whispry.bridge.KeyboardSessionTest' \
  -PwhispryBridgeAppId=com.cosmictaser.whispry.bridge --console=plain
```

In `/Users/aryan/Documents/FrostKeys`:

```sh
./gradlew :app:assembleDebug :hig:testDebugUnitTest \
  -PwhispryPackage=com.cosmictaser.whispry.bridge --console=plain
```

Artifacts:

- Backend: `/Users/aryan/Documents/whispry-git/app/build/outputs/apk/debug/app-debug.apk`.
- Keyboard: `/Users/aryan/Documents/FrostKeys/app/build/outputs/apk/debug/WhispryKeys_2.5.9-debug.apk`.

Use `/Users/aryan/Library/Android/sdk/platform-tools/adb devices -l` to find the current phone serial.
Install the Bridge APK, then the keyboard APK, with `adb -s <PHONE_SERIAL> install -r <APK>`.
Do not uninstall or overwrite the original Whispry package to bypass the signing mismatch.

For an eventual correctly signed update of the original package, the backend also accepts
`-PwhispryVersionCode=<CODE>` for a deliberate version-code override. That property does not solve
a signing mismatch and must not be used as if it did.

## 11. Working rules for the next session

- Resume from this handoff and existing changes; do not restart from the downloaded master.
- Keep credentials in Whispry; never print or copy them into the keyboard, logs or documentation.
- Preserve the original installed application and its data.
- Continue testing the minimal voice bridge before adding tones or other tools.
- Treat device speech delivery as pending until actually observed and recorded in a test report.
- No app, source changes, paid content or purchase configuration has been published by this work.
