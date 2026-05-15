# FluxSync

FluxSync is a Kotlin Multiplatform file synchronization app for Android and Windows/Desktop. The MVP target is a symmetric peer-to-peer transfer tool that can bond Wi-Fi TCP and USB/ADB paths, stream chunks directly to disk, and resume unfinished `.part` transfers from receiver-owned checkpoint metadata.

This repository was scaffolded from the FluxSync TRD and Codex prompt suite in [`docs/`](docs/).

## Project Status

The app has been implemented as a first-pass KMP codebase with shared core logic, shared Compose UI, and Android/Desktop shells.

Current coverage includes:

- Kotlin Multiplatform Gradle project with `:core`, `:ui`, `:android`, and `:desktop`
- Shared transfer models, JSON control protocol, ACK/NACK tracking, checkpoint metadata, queueing, conflict resolution, and rolling logs
- WebSocket control channel and raw TCP data channel abstractions
- Android and Desktop actuals for file I/O, crypto, logging, mDNS discovery, data channel, and `.part` scanning
- Shared strict dark-mode Compose UI for discovery, pairing, active transfer, settings, and developer console
- Android foreground transfer service with wake lock and progress notification
- Desktop ADB poller, system tray behavior, and file/folder picker hooks
- Common test coverage for core utility behavior and protocol serialization

See [`docs/IMPLEMENTATION_CHECKLIST.md`](docs/IMPLEMENTATION_CHECKLIST.md) for the detailed requirement mapping.

## Repository Layout

```text
.
├── android/   Android application shell
├── core/      Shared transfer engine, protocol, discovery, pairing, logging
├── desktop/   Compose Desktop application shell for Windows JVM
├── docs/      TRD, prompt suite, and implementation checklist
├── gradle/    Version catalog
└── ui/        Shared Compose Multiplatform UI and ViewModels
```

## Prerequisites

The local machine needs the Kotlin/Android toolchain before the project can be compiled:

- JDK 21
- Android SDK with API 35
- Android Platform Tools, including `adb`, for wired transfer support
- Gradle wrapper generated for the repo, or a local Gradle install

At the time of scaffolding, `java`, `gradle`, and `gradlew.bat` were not available on PATH, so compile/test verification could not be run.

## Build

After installing the prerequisites and adding a Gradle wrapper:

```powershell
.\gradlew.bat :core:allTests
.\gradlew.bat :android:assembleDebug
.\gradlew.bat :desktop:packageDistributionForCurrentOS
```

If using a system Gradle install before the wrapper exists:

```powershell
gradle wrapper
.\gradlew.bat :core:allTests
```

## Development Order

Recommended next steps:

1. Install JDK 21, Android SDK API 35, and Android Platform Tools.
2. Generate the Gradle wrapper.
3. Run compile/tests and fix compiler-reported integration issues.
4. Wire Android file/folder picker Activity Result flows into `TransferViewModel`.
5. Run Wi-Fi transfer end to end between Windows and Android.
6. Validate ADB reverse wired transfer and failover.
7. Harden subnet binding around discovered peer interfaces.
8. Decide whether BLE fallback is required for MVP or should be deferred.
9. Add release signing config and package Android/Desktop builds.

## Remaining Dependencies

Some remaining work depends on external platform/tooling choices:

- BLE fallback needs a Windows/Desktop BLE approach, such as WinRT/JNA interop or a JVM BLE library.
- Android release builds need a real keystore and signing configuration.
- End-to-end transfer validation needs one Android API 30+ device and one Windows 10/11 machine on the same LAN.
- Wired bonding needs `adb` installed and visible on PATH.

## Notes

- The UI is intentionally dark-mode only per the TRD.
- Transfer history is intentionally excluded from the MVP.
- Unfinished `.part` files are retained for resumption unless cleared through the in-app storage utility.
- Android uses `MANAGE_EXTERNAL_STORAGE`, which implies sideload distribution or Play Store exemption approval.
