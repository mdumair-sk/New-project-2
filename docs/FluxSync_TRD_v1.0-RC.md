# FluxSync — Technical Requirements Document
**Version:** 1.0-RC (MVP Release)
**Architecture:** Symmetric Peer-to-Peer · Kotlin Multiplatform (KMP)
**Target Platforms:** Android API 30+ · Windows 10/11
**Status:** Release Candidate · 2026-05-14

---

## 1. Product Overview

FluxSync is a high-performance, multipath file synchronization utility designed to maximize transfer bandwidth by bonding wired (USB via ADB) and wireless (Wi-Fi TCP) connections simultaneously. The core engine operates on a zero-hold architecture, streaming data directly to disk to minimize RAM overhead and ensure stability during large-scale transfers.

---

## 2. Non-Technical Requirements (NTR)

### 2.1 User Experience & Interface

| ID | Requirement |
|----|-------------|
| NTR-101 | **[Strict Dark Mode]** The application UI must exclusively utilize a Dark Mode theme across all platforms. |
| NTR-102 | **[Zero-Friction Naming]** Devices must be automatically assigned randomized, localized monikers upon initial launch (e.g., "Umair's Clever Tomato"). |
| NTR-103 | **[Dashboard Metrics]** The active transfer screen must display a live speed metric with a user-configurable refresh rate (500 ms instantaneous or a 2-second moving average), alongside total progress and estimated time remaining. |
| NTR-104 | **[Clear Separation]** The UI must provide distinct, explicitly separated actions for "Select Files" and "Select Folder". |
| NTR-105 | **[Queue Management]** The UI must allow users to view the Waitlist of the current batch and dynamically enqueue additional items during an active transfer. |
| NTR-106 | **[Wasted Space Utility]** A dedicated section must display the storage occupied by unfinished `.part` files and provide an "Empty Trash" function to clear them. |
| NTR-107 | **[Session History]** The UI will only display active transfers and queued files. Persistent transfer history logs are explicitly excluded from the MVP. |

### 2.2 Security & Trust Flow

| ID | Requirement |
|----|-------------|
| NTR-201 | **[Symmetric Handshake]** Device pairing requires both peers to view and manually confirm a randomly generated 6-digit PIN. An unconfirmed pairing session expires automatically after **60 seconds**, at which point both peers must re-initiate discovery. |
| NTR-202 | **[Trusted Devices]** Once paired, AES-256 keys derived from the PIN are saved indefinitely. Future connections between Trusted Devices must occur automatically without PIN confirmation. |
| NTR-203 | **[Trust Revocation]** A "Trusted Devices" settings menu must allow users to manually remove authorized peers. |

### 2.3 Environment & Operations

| ID | Requirement |
|----|-------------|
| NTR-301 | **[Network Restriction]** The application will strictly bind the connection to the subnet where the peer device was discovered. |
| NTR-302 | **[VPN Detection]** The system must detect active VPN configurations and display a warning indicating that local mDNS discovery may fail. |

---

## 3. Technical Requirements (TR)

### 3.1 Core Engine & KMP Architecture

| ID | Requirement |
|----|-------------|
| TR-101 | **[Shared Core]** All logic for chunking, hashing, packet assembly, and multipath distribution must reside in the `commonMain` KMP module. |
| TR-102 | **[Agnostic Transport Layer]** The engine must treat all active sockets (ADB or Wi-Fi) as generic `TransportLink` objects, dispatching data based on each pipe's asynchronous pull capability. Backpressure is managed via Kotlin Flow and channel capacity constraints; a saturated pipe suspends its consumer coroutine until capacity is available. |
| TR-103 | **[Control Channel]** A dedicated WebSocket connection must handle all control signals, utilizing standard JSON serialization for payloads (e.g., `TransferRequest`, `ChunkACK`, `Heartbeat`, `NACK`, `SessionEnd`). |
| TR-104 | **[Instant Acknowledgement]** The receiver must transmit an application-layer JSON `ChunkACK` via the WebSocket immediately upon successful disk write of each chunk. |
| TR-105 | **[Control Channel Resilience]** Upon detecting a WebSocket disconnect during an active transfer, the engine must pause chunk dispatch and attempt reconnection at 2-second intervals for a maximum of **30 seconds**. If reconnection succeeds, the transfer resumes. If the timeout elapses, the transfer is aborted and logged. Unacknowledged chunks are tracked for resumption via TR-308. |

### 3.2 Multipath Network Bonding

| ID | Requirement |
|----|-------------|
| TR-201 | **[Discovery Mechanism]** Primary peer discovery must operate over mDNS. The advertised service type is `_fluxsync._tcp.local.` |
| TR-202 | **[Discovery Fallbacks]** If mDNS fails, the system must fallback to Bluetooth LE broadcast and provide a manual IP entry interface. |
| TR-203 | **[Wired Auto-Tunnel]** The Windows client must continuously poll for USB-connected Android devices. Upon detection, it must execute `adb reverse tcp:<port> tcp:<port>` to establish the wired bridge. |
| TR-204 | **[Dynamic Failover]** In the event of a `SocketException` on any pipe (e.g., USB cable disconnection), unacknowledged chunks must be instantly re-queued, allowing the transfer to continue uninterrupted on surviving pipes. |
| TR-205 | **[Concurrency Limits]** The number of concurrent `TransportLink`s is unbounded at the application layer and limited only by platform socket capacity. The engine registers each link dynamically and removes it upon failure or graceful close. |

### 3.3 Control Protocol — Payload Schemas

All control payloads are UTF-8 encoded JSON transmitted over the WebSocket control channel (TR-103).

#### 3.3.1 TransferRequest (Sender → Receiver)
```json
{
  "type":         "TransferRequest",
  "transferId":   "<uuid-v4>",
  "fileName":     "video.mp4",
  "totalSize":    10737418240,
  "chunkSize":    2097152,
  "totalChunks":  5120,
  "xxhash":       "<hex-string>",
  "relativePath": "Projects/Media/video.mp4"
}
```

#### 3.3.2 ChunkACK (Receiver → Sender)
```json
{
  "type":       "ChunkACK",
  "transferId": "<uuid-v4>",
  "sequenceId": 42
}
```

#### 3.3.3 NACK (Receiver → Sender)
Issued when a disk write fails or a chunk-level XXHash verification fails. The sender must re-transmit the identified chunk on the next available pipe.
```json
{
  "type":       "NACK",
  "transferId": "<uuid-v4>",
  "sequenceId": 42,
  "reason":     "DISK_WRITE_FAILURE" | "CHECKSUM_MISMATCH"
}
```

#### 3.3.4 Heartbeat (Bidirectional)
```json
{
  "type":      "Heartbeat",
  "timestamp": 1747238400000
}
```

#### 3.3.5 SessionEnd (Sender → Receiver)
Issued by the sender upon successful completion or explicit cancellation. The receiver must respond with a matching `SessionEnd` to confirm graceful teardown.
```json
{
  "type":       "SessionEnd",
  "transferId": "<uuid-v4>",
  "reason":     "COMPLETE" | "CANCELLED" | "ERROR",
  "message":    "<optional human-readable detail>"
}
```

### 3.4 Session Lifecycle

| State | Description & Transition |
|-------|--------------------------|
| `IDLE` | No active transfer. Engine listening for discovery and incoming connections. |
| `NEGOTIATING` | `TransferRequest` sent. Awaiting receiver pre-allocation confirmation. Timeout: 30 s. |
| `TRANSFERRING` | Chunks actively dispatched across `TransportLink`s. |
| `PAUSED_RECONNECT` | WebSocket dropped. Chunk dispatch suspended. Reconnect attempts every 2 s, max 30 s. |
| `VERIFYING` | All chunks received. Receiver executing XXHash on assembled `.part` file. |
| `FINALIZING` | Hash matched. `.part` file renamed to original extension. |
| `COMPLETE` | `SessionEnd (COMPLETE)` exchanged. Resources released. |
| `ABORTED` | `SessionEnd (CANCELLED \| ERROR)` issued. `.part` file retained for resumption unless explicitly cleared (NTR-106). |

### 3.5 File I/O & Storage Management

| ID | Requirement |
|----|-------------|
| TR-301 | **[Sequential Execution]** While the UI accepts batch selections, the engine must transfer exactly one file at a time. The next file begins only after the current file reaches `COMPLETE` state. |
| TR-302 | **[Pre-Allocation]** Upon receiving a `TransferRequest`, the receiver must pre-allocate the `.part` file using sparse file allocation on NTFS (Windows) and `fallocate(2)` on Android where supported, falling back to zero-fill. This verifies disk capacity and reduces fragmentation without mandating a full write cycle. |
| TR-303 | **[Zero-Hold RAM]** Incoming `FluxPacket` payloads must bypass RAM buffering. The engine must calculate the disk offset (`SequenceID × ChunkSize`) and execute a `RandomAccessFile` write immediately upon receipt. |
| TR-304 | **[Configurable Buffer]** `ChunkSize` defaults to **2 MB** and is user-adjustable in settings (range: 512 KB – 16 MB) to allow high-end hardware to optimize disk controller throughput. |
| TR-305 | **[Atomic Verification]** A final XXHash-64 calculation must be executed on the fully assembled `.part` file. Only upon matching the sender's original hash is the file renamed to its original extension. A mismatch triggers a full-file abort. |
| TR-306 | **[Exact Recreation]** Deeply nested folder structures transferred via batch must be recreated recursively on the receiving device using the `relativePath` field from `TransferRequest`. |
| TR-307 | **[Conflict Resolution]** File name collisions must automatically resolve by appending a sequence number (e.g., `filename(1).ext`). Collision detection applies to the final file name, not the `.part` file. |
| TR-308 | **[Checkpoint Resumption]** Transfers must maintain a `meta.json` manifest owned by the receiver and stored alongside the `.part` file. The manifest logs all received `SequenceID`s. Upon resume, the receiver pushes the manifest to the sender via the control channel, and the sender transmits only unacknowledged chunks. |

#### 3.5.1 meta.json Schema
```json
{
  "transferId":     "<uuid-v4>",
  "fileName":       "video.mp4",
  "totalChunks":    5120,
  "chunkSize":      2097152,
  "xxhash":         "<hex-string>",
  "receivedChunks": [0, 1, 2, 7, 42]
}
```

### 3.6 Transfer Constraints

| ID | Requirement |
|----|-------------|
| TR-350 | **[Max File Size]** No enforced upper bound on individual file size. Practical limits are governed by available disk space on the receiver, validated via pre-allocation (TR-302). |
| TR-351 | **[Max Batch Size]** No enforced upper bound on batch queue depth. Items are held in memory as an ordered list; the engine processes them sequentially. |

### 3.7 Telemetry & Logging

| ID | Requirement |
|----|-------------|
| TR-401 | **[In-App Console]** The UI must contain a "Show Details" view exposing a real-time, scrolling console of high-level events and stack traces. |
| TR-402 | **[Persistent Logs]** A rolling log file must be written to disk, capped at 5 MB per file, retaining a maximum of the last 3 sessions. |
| TR-403 | **[Log Format]** Telemetry data must use the device's local timezone and adhere to the following format: `[YYYY-MM-DD HH:MM:SS TZ] [CHANNEL] [LEVEL] - Message` — e.g., `[2026-05-14 20:55:12 IST] [CORE] [ERROR] - SocketTimeoutException: Pipe reset by peer` |

---

## 4. Platform-Specific Constraints

### 4.1 Android Client

| ID | Requirement |
|----|-------------|
| TR-501 | **[File System Access]** The client must utilize the `MANAGE_EXTERNAL_STORAGE` permission to bypass SAF Tree URIs for maximum I/O velocity and seamless directory recreation. This permission requires Play Store exemption approval or sideload distribution. |
| TR-502 | **[Service Persistence]** File transfers must execute within an Android Foreground Service. |
| TR-503 | **[Power Management]** The application must acquire a `PARTIAL_WAKE_LOCK` upon transfer initialization to prevent the OS from throttling CPU or Wi-Fi radios when the screen is disabled. |
| TR-504 | **[Notification Integration]** The system notification drawer must display a persistent, non-dismissible (while active) progress bar detailing transfer speed and percentage. |

### 4.2 Windows Client

| ID | Requirement |
|----|-------------|
| TR-601 | **[Binary Dependency]** The application assumes `adb` is installed and configured in the system PATH. If not detected on launch, the UI must display a persistent banner warning with a link to Android Platform Tools. Wired transport is disabled until resolved; Wi-Fi transport continues unaffected. |
| TR-602 | **[Window Lifecycle]** Interacting with the window Close control must minimize the application to the system tray, preserving the Ktor listener and mDNS broadcast state. |
| TR-603 | **[Launch Behavior]** The application must not auto-start upon OS boot. Launch is strictly manual. |

---

## Appendix A — Error & Reason Code Reference

| Code | Context | Description |
|------|---------|-------------|
| `DISK_WRITE_FAILURE` | `NACK.reason` | Receiver could not write chunk to disk. Sender retransmits the chunk. |
| `CHECKSUM_MISMATCH` | `NACK.reason` | Per-chunk or full-file XXHash mismatch. Sender retransmits chunk or aborts. |
| `COMPLETE` | `SessionEnd.reason` | Transfer finished successfully and verified. |
| `CANCELLED` | `SessionEnd.reason` | User explicitly cancelled the transfer. |
| `ERROR` | `SessionEnd.reason` | Unrecoverable engine error. See `message` field for detail. |
| `WS_TIMEOUT` | Internal log | WebSocket reconnect window (30 s) elapsed; transfer aborted. |
| `PIN_EXPIRED` | Internal log | Pairing PIN not confirmed within 60 s; session invalidated. |
