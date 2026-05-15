# FluxSync Implementation Checklist

## Expect / Actual Coverage

| Expect | Android actual | Desktop actual |
|---|---|---|
| `PlatformSocket` | yes | yes |
| `PeerDiscovery` | yes, Android `NsdManager` | yes, JmDNS |
| `ControlServer` | yes, Ktor CIO WebSocket | yes, Ktor CIO WebSocket |
| `DataChannelServer` / `DataChannelClient` | yes, framed TCP sockets | yes, framed TCP sockets |
| `PartFileScanner` | yes | yes |
| `readFileChunk`, `preallocateFile`, `writeFileChunk`, `writeMetaJson`, `readMetaJson`, `computeXxHash`, `renameFile`, `ensureDirectory` | yes | yes |
| `deriveKey`, `secureRandomBytes` | yes | yes |
| `currentTimestampFormatted`, `currentTimeMillis`, file log helpers | yes | yes |
| `VpnDetector` | yes | yes |

## Interfaces / Implementations

| Interface / Contract | Implementation |
|---|---|
| `ControlChannel` | `KtorControlChannel`, `ControlChannelManager` |
| WebSocket receiver | `ControlServer` actuals |
| Raw chunk data path | `DataChannelClient` / `DataChannelServer` actuals plus `ChunkFraming` |
| Peer persistence | `PeerRegistry` with Multiplatform Settings |
| Trust persistence | `TrustStore` with JSON records |
| Transfer queue | `TransferQueue` |
| Conflict resolution | `ConflictResolver` |
| Checkpoint resume manifest | `CheckpointManager` |
| Rolling logs | `LogManager`, `RollingFileLogger` |

## ViewModel / Screen Wiring

| ViewModel | Screen |
|---|---|
| `DiscoveryViewModel` | `DiscoveryScreen` |
| `PairingViewModel` | `PairingScreen` |
| `TransferViewModel` | `ActiveTransferScreen` |
| `SettingsViewModel` | `SettingsScreen` |
| `ConsoleViewModel` | `DevConsoleScreen` |

## TRD Coverage Snapshot

| Requirement | Status | Location |
|---|---|---|
| NTR-101 | Implemented | strict dark `FluxSyncTheme` |
| NTR-102 | Implemented | `MonikerGenerator`, service locators |
| NTR-103 | Implemented | `SpeedGauge`, `TransferViewModel` speed loop |
| NTR-104 | Implemented | separate Add Files / Add Folder controls |
| NTR-105 | Implemented | queue screen and dynamic enqueue hooks |
| NTR-106 | Implemented | `StorageBanner`, `PartFileScanner`, `emptyTrash()` |
| NTR-107 | Implemented | no persistent transfer history UI |
| NTR-201 | Implemented | `PairingSession` PIN expiry and confirm flow |
| NTR-202 | Implemented | `TrustStore` persistence |
| NTR-203 | Implemented | settings trusted-device removal |
| NTR-301 | Partial | peer address retained; strict subnet binding needs transport enforcement hardening |
| NTR-302 | Implemented | Android/Desktop `VpnDetector` |
| TR-101 | Implemented | core transfer/protocol logic in `:core` commonMain |
| TR-102 | Partial | `TransportLink` abstraction and backpressure channels exist; socket backpressure needs production load testing |
| TR-103 | Implemented | `ControlChannel`, `ControlServer` |
| TR-104 | Implemented | receiver sends `ChunkAck` after disk write |
| TR-105 | Implemented | `ControlChannelManager` reconnect flow |
| TR-201 | Implemented | `_fluxsync._tcp.local.` discovery |
| TR-202 | Partial | manual IP implemented; BLE fallback not implemented |
| TR-203 | Implemented | desktop `AdbPoller` reverse setup |
| TR-204 | Implemented | dispatcher requeues in-flight chunks on send failure |
| TR-205 | Implemented | dynamic link registry |
| TR-301 | Implemented | `TransferQueue` sequential model |
| TR-302 | Implemented | platform preallocation via sparse/setLength fallback |
| TR-303 | Implemented | offset-based `writeFileChunk` |
| TR-304 | Implemented | persisted chunk-size setting |
| TR-305 | Implemented | final hash verify then rename |
| TR-306 | Implemented | `relativePath`/destination path carried in request |
| TR-307 | Implemented | `ConflictResolver` |
| TR-308 | Implemented | `meta.json` checkpoint manager |
| TR-350 | Implemented | no max file-size guard |
| TR-351 | Implemented | unbounded queue list |
| TR-401 | Implemented | console screen |
| TR-402 | Implemented | rolling file logger |
| TR-403 | Implemented | timestamped log format |
| TR-501 | Implemented | manifest permission and request flow |
| TR-502 | Implemented | Android foreground service |
| TR-503 | Implemented | partial wake lock |
| TR-504 | Implemented | persistent progress notification |
| TR-601 | Implemented | ADB poller and UI warning state |
| TR-602 | Implemented | desktop tray close behavior |
| TR-603 | Implemented | no auto-start registration |

## Verification

Static source audit passed for generated source: no `!!`, no `println`, no `System.out`.

Compile and test execution are blocked in this workspace because neither `java`, `gradle`, nor a Gradle wrapper is available on PATH.
