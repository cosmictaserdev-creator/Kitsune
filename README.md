# Kitsune

A privacy-respecting Android keyboard with Apple-style settings, rich themes and optional
voice and writing tools through the separate **Whispry** app.

## Status
Early development. Voice typing needs the Whispry companion app (Android 8+); ordinary typing
works without it. Themes beyond the included ones and the Pro theme editor are planned.

## Building
```sh
./gradlew :app:assembleDebug            # needs JDK 21 and the Android SDK/NDK
./gradlew :app:assembleRelease          # signed if keystore.properties or KITSUNE_* env vars exist
```
Signed release builds from CI: see `.github/workflows/build-release-apk.yml`. Required repository
secrets: `KITSUNE_KEYSTORE_BASE64`, `KITSUNE_KEYSTORE_PASSWORD`, `KITSUNE_KEY_ALIAS`, `KITSUNE_KEY_PASSWORD`.

## License and credits
Kitsune is a derivative work and is licensed under the **GNU GPL v3.0** (see `LICENSE`); the complete
source is in this repository. Original copyright and license notices are preserved.

Kitsune is built on:
- **FrostKeys** by Ashwin Soni (Orion), a fork of HeliBoard (blur UI, AI tools, Klipy media).
- **[HeliBoard](https://github.com/HeliBorg/HeliBoard)**, based on **OpenBoard** and the **AOSP Keyboard** (Apache-2.0, see `LICENSE-Apache-2.0`).
- **Compose HIG** (Apache-2.0, see `hig/NOTICE.txt`) for the Apple-style components.

Original theme artwork and Whispry are separate works and are not covered by this license unless stated.
The FrostKeys icon (CC BY-SA 4.0, `LICENSE-CC-BY-SA-4.0`) is not part of the Kitsune brand.
