# FluxSync — Codex Prompt Suite
**Target:** Production-ready Kotlin Multiplatform + Compose Multiplatform app
**Tool:** OpenAI Codex
**Order:** Execute prompts sequentially. Never skip. Each prompt assumes prior work exists.

---

## HOW TO USE THIS DOCUMENT

- Paste each prompt **in full** into Codex.
- After each prompt: review generated files, fix compile errors, commit.
- Never combine prompts — each is scoped to a single concern.
- When a prompt says "per the TRD", it refers to the spec embedded in Prompt 0.

---

---

# PROMPT 0 — Project Scaffold & Constraints

```
You are a senior Kotlin Multiplatform engineer. Your task is to scaffold a production-grade KMP project named FluxSync.

## Constraints (read every word before writing any code)

### Architecture
- Kotlin Multiplatform project with targets: androidMain, desktopMain (JVM), commonMain
- UI: Compose Multiplatform (shared UI across Android and Desktop/Windows)
- Build system: Gradle with Kotlin DSL (.kts files only)
- Minimum Android API: 30
- Desktop target: Windows 10/11 JVM

### Module Structure
Create the following Gradle modules:
1. :core — commonMain only. All engine logic lives here.
2. :ui — Compose Multiplatform shared UI. Depends on :core.
3. :android — Android application shell. Depends on :ui, :core.
4. :desktop — Desktop (JVM) application shell. Depends on :ui, :core.

### Core Dependencies (add to version catalog libs.versions.toml)
- Kotlin: 2.0.21
- Compose Multiplatform: 1.7.3
- Ktor (client + server, CIO engine): 3.0.3
- Kotlinx Coroutines: 1.9.0
- Kotlinx Serialization JSON: 1.7.3
- Multiplatform Settings (russhwolf): 1.2.0
- UUID (benasher44): 0.8.4
- XXHash (via Apache Commons Codec on JVM, native impl on Android via JNI): note as TODO
- Kermit logging (touchlab): 2.0.5
- Turbine (testing): 1.2.0
- MockK: 1.13.12

### What to generate
1. settings.gradle.kts — declare all 4 modules
2. root build.gradle.kts — version catalog, base plugin config
3. gradle/libs.versions.toml — all dependency versions above
4. :core/build.gradle.kts
5. :ui/build.gradle.kts
6. :android/build.gradle.kts — applicationId "com.fluxsync.app", versionCode 1, versionName "1.0.0"
7. :desktop/build.gradle.kts — compose desktop extension, mainClass "com.fluxsync.desktop.MainKt"
8. Empty src directory trees for all modules following KMP conventions
9. .gitignore appropriate for KMP + Android + Gradle

### Do NOT generate
- Any business logic yet
- Any UI composables yet
- Any placeholder "Hello World" code

Output only files. No explanation prose.
```

---

---

# PROMPT 1 — Core Data Models

```
You are working on FluxSync, a KMP file transfer app. The :core module's commonMain already exists from scaffolding.

## Task
Generate all shared data models in :core/src/commonMain/kotlin/com/fluxsync/core/model/

### File: TransferFile.kt
data class TransferFile(
  val transferId: String,       // UUID v4
  val fileName: String,
  val relativePath: String,     // e.g. "Projects/Media/video.mp4"
  val totalSize: Long,          // bytes
  val chunkSize: Int,           // bytes, default 2_097_152 (2MB)
  val totalChunks: Int,         // ceil(totalSize / chunkSize)
  val xxhash: String,           // hex string, XXHash-64 of source file
  val state: TransferState,
)

### File: TransferState.kt
sealed class TransferState:
  - Idle
  - Negotiating
  - Transferring(val receivedChunks: Set<Int>, val totalChunks: Int)
  - PausedReconnect(val attemptCount: Int)
  - Verifying
  - Finalizing
  - Complete
  - Aborted(val reason: AbortReason)

### File: AbortReason.kt
enum class AbortReason { WS_TIMEOUT, CHECKSUM_MISMATCH, USER_CANCELLED, ENGINE_ERROR }

### File: TransportLink.kt
data class TransportLink(
  val id: String,               // UUID
  val type: LinkType,           // ADB or WIFI
  val socket: Any,              // platform expect/actual — define expect class PlatformSocket
  val isActive: Boolean,
)
enum class LinkType { ADB, WIFI }

### File: Peer.kt
data class Peer(
  val id: String,               // UUID
  val moniker: String,          // e.g. "Clever Tomato"
  val isTrusted: Boolean,
  val address: String,          // IP or tunnel address
  val port: Int,
)

### File: ChunkPayload.kt (binary envelope, not JSON)
data class ChunkPayload(
  val transferId: String,
  val sequenceId: Int,
  val data: ByteArray,
) — override equals/hashCode on data field

### File: PartFileMeta.kt (mirrors meta.json)
@Serializable
data class PartFileMeta(
  val transferId: String,
  val fileName: String,
  val totalChunks: Int,
  val chunkSize: Int,
  val xxhash: String,
  val receivedChunks: List<Int>,
)

### Rules
- All models in commonMain
- Use @Serializable on any model that crosses the wire or is written to disk
- PlatformSocket: define as expect class in commonMain, generate empty actual class stubs in androidMain and desktopMain
- No Android or JVM imports anywhere in commonMain
- Use kotlinx.serialization only

Output only code files. No explanation.
```

---

---

# PROMPT 2 — Control Protocol (JSON Payloads)

```
You are working on FluxSync :core module. Data models from Prompt 1 exist.

## Task
Generate all WebSocket control channel payload classes in:
:core/src/commonMain/kotlin/com/fluxsync/core/protocol/

### Base sealed class: ControlMessage.kt
@Serializable
sealed class ControlMessage {
  abstract val type: String
}

Implement the following subclasses, each in its own file, all @Serializable with @SerialName on the class:

### TransferRequest.kt
type = "TransferRequest"
fields: transferId, fileName, totalSize, chunkSize, totalChunks, xxhash, relativePath

### ChunkAck.kt
type = "ChunkACK"
fields: transferId, sequenceId: Int

### Nack.kt
type = "NACK"
fields: transferId, sequenceId: Int, reason: NackReason
enum class NackReason { DISK_WRITE_FAILURE, CHECKSUM_MISMATCH }

### Heartbeat.kt
type = "Heartbeat"
fields: timestamp: Long (epoch ms)

### SessionEnd.kt
type = "SessionEnd"
fields: transferId, reason: SessionEndReason, message: String? = null
enum class SessionEndReason { COMPLETE, CANCELLED, ERROR }

### ControlMessageSerializer.kt
- Implement a custom KSerializer<ControlMessage> using JsonContentPolymorphicSerializer
- Dispatch on the "type" field
- Register it so Json.decodeFromString<ControlMessage>(raw) works correctly

### ProtocolConstants.kt
object ProtocolConstants {
  const val WS_PORT = 8765
  const val DATA_PORT = 8766
  const val MDNS_SERVICE_TYPE = "_fluxsync._tcp.local."
  const val WS_RECONNECT_INTERVAL_MS = 2_000L
  const val WS_RECONNECT_TIMEOUT_MS = 30_000L
  const val PIN_EXPIRY_MS = 60_000L
  const val DEFAULT_CHUNK_SIZE = 2_097_152       // 2 MB
  const val MIN_CHUNK_SIZE = 524_288             // 512 KB
  const val MAX_CHUNK_SIZE = 16_777_216          // 16 MB
  const val LOG_MAX_FILE_SIZE_BYTES = 5_242_880  // 5 MB
  const val LOG_MAX_SESSION_FILES = 3
}

Output only code. No explanation.
```

---

---

# PROMPT 3 — Transfer Engine (commonMain)

```
You are working on FluxSync :core module. Prompts 1 and 2 are complete.

## Task
Generate the core transfer engine in:
:core/src/commonMain/kotlin/com/fluxsync/core/engine/

### ChunkDispatcher.kt
- Holds a list of active TransportLink objects
- Exposes suspend fun dispatch(payload: ChunkPayload): dispatches to the least-loaded active link
- Uses a Channel<ChunkPayload> per link with BUFFERED capacity
- On SocketException on any link: removes it from active list, re-queues all unacknowledged chunks from that link's in-flight set back to a global retry Channel<Int> (sequenceIds)
- Exposes fun registerLink(link: TransportLink) and fun removeLink(id: String)
- Thread-safe: uses Mutex for link list mutations

### AckTracker.kt
- Tracks which sequenceIds have been acknowledged per transferId
- fun markAcked(transferId: String, sequenceId: Int)
- fun markNacked(transferId: String, sequenceId: Int) — adds back to retry queue
- fun isComplete(transferId: String, totalChunks: Int): Boolean
- fun getUnacknowledged(transferId: String, totalChunks: Int): List<Int>
- fun reset(transferId: String)
- Internal storage: ConcurrentHashMap equivalent using a Mutex-guarded MutableMap<String, MutableSet<Int>>

### TransferSession.kt
- Orchestrates a single file transfer (sender side)
- Constructor: file: TransferFile, dispatcher: ChunkDispatcher, ackTracker: AckTracker, controlSender: suspend (ControlMessage) -> Unit
- suspend fun start(): reads file via expect fun readFileChunk(path: String, offset: Long, length: Int): ByteArray
- Sends TransferRequest via controlSender
- Awaits receiver acknowledgement of pre-allocation (implement as a CompletableDeferred<Unit>)
- Streams chunks: for each sequenceId, reads chunk, wraps in ChunkPayload, dispatches via ChunkDispatcher
- Monitors retryChannel from AckTracker and re-dispatches NACKed chunks
- State transitions must be emitted via a StateFlow<TransferState>
- suspend fun pause() and suspend fun resume() must work correctly
- suspend fun cancel(): emits SessionEnd(CANCELLED), transitions to Aborted

### ReceiverSession.kt
- Orchestrates a single file transfer (receiver side)
- Constructor: request: TransferRequest, controlSender: suspend (ControlMessage) -> Unit
- suspend fun start(): pre-allocates .part file via expect fun preallocateFile(path: String, size: Long)
- suspend fun writeChunk(payload: ChunkPayload): writes chunk at offset (sequenceId * chunkSize), sends ChunkAck, updates PartFileMeta and writes meta.json via expect fun writeMetaJson(path: String, meta: PartFileMeta)
- suspend fun verify(): runs XXHash-64 via expect fun computeXxHash(path: String): String, compares with request.xxhash
- On match: renames .part to original via expect fun renameFile(from: String, to: String)
- On mismatch: emits SessionEnd(ERROR, "CHECKSUM_MISMATCH"), transitions to Aborted
- State transitions emitted via StateFlow<TransferState>

### expect declarations
All expect funs above go in:
:core/src/commonMain/kotlin/com/fluxsync/core/platform/FileOps.kt
Generate empty actual stubs in androidMain and desktopMain — do NOT implement them yet.

### Rules
- No Android/JVM imports in commonMain
- All coroutine scopes passed in as constructor parameters (no GlobalScope)
- No hardcoded paths — all paths passed as parameters
- StateFlow must never throw; use catch { } on all suspend chains

Output only code. No explanation.
```

---

---

# PROMPT 4 — WebSocket Control Channel

```
You are working on FluxSync :core module. Prompts 1–3 are complete.

## Task
Generate the WebSocket control channel layer using Ktor in:
:core/src/commonMain/kotlin/com/fluxsync/core/network/

### ControlChannel.kt
interface ControlChannel {
  suspend fun connect(address: String, port: Int)
  suspend fun send(message: ControlMessage)
  fun incoming(): Flow<ControlMessage>
  suspend fun disconnect()
  val isConnected: StateFlow<Boolean>
}

### KtorControlChannel.kt
- Implements ControlChannel using Ktor WebSocket client (CIO engine)
- On incoming raw String: deserialize via ControlMessageSerializer
- On outgoing ControlMessage: serialize to JSON, send as text frame
- Heartbeat: launch a coroutine sending Heartbeat every 15 seconds
- On WebSocket close/error: set isConnected = false, do NOT attempt reconnect here (reconnect logic is in ControlChannelManager)

### ControlChannelManager.kt
- Wraps KtorControlChannel
- On disconnect detected: implements reconnect loop per ProtocolConstants:
  - Retry every WS_RECONNECT_INTERVAL_MS
  - Give up after WS_RECONNECT_TIMEOUT_MS
  - On give up: emit AbortReason.WS_TIMEOUT to a SharedFlow<AbortReason>
- Exposes same interface as ControlChannel
- State: emits ControlChannelState (Connected, Reconnecting(attempt), Disconnected, Failed)

### ControlServer.kt (receiver side)
- Uses Ktor server (CIO) to accept a WebSocket connection on ProtocolConstants.WS_PORT
- On each incoming text frame: deserialize and emit to a SharedFlow<ControlMessage>
- Exposes suspend fun start() and suspend fun stop()
- Only accepts one connection at a time (reject additional)

### Rules
- CIO engine only — no OkHttp
- All flows are cold or SharedFlow with replay=1
- No blocking calls; everything suspend or Flow
- CoroutineScope injected via constructor

Output only code. No explanation.
```

---

---

# PROMPT 5 — mDNS Discovery

```
You are working on FluxSync :core module. Prompts 1–4 are complete.

## Task
Generate peer discovery in:
:core/src/commonMain/kotlin/com/fluxsync/core/discovery/

### DiscoveryResult.kt
sealed class DiscoveryResult {
  data class Found(val peer: Peer): DiscoveryResult()
  data class Lost(val peerId: String): DiscoveryResult()
  object Searching: DiscoveryResult()
}

### PeerDiscovery.kt (expect/actual interface)
expect class PeerDiscovery(scope: CoroutineScope) {
  fun start(): Flow<DiscoveryResult>
  suspend fun advertise(peer: Peer)
  suspend fun stop()
}
Generate empty actual stubs in androidMain and desktopMain.

### ManualEntryDiscovery.kt (commonMain)
class ManualEntryDiscovery {
  // Converts a manually entered IP:port string into a Peer
  fun resolve(input: String, localPeer: Peer): Result<Peer>
  // Validates format, returns Result.failure on invalid input
}

### PeerRegistry.kt (commonMain)
- In-memory registry of discovered + trusted peers
- fun addOrUpdate(peer: Peer)
- fun remove(peerId: String)
- fun getTrusted(): List<Peer>
- fun getAll(): StateFlow<List<Peer>>
- Persists trusted peers via multiplatform-settings (inject Settings as constructor param)
- Serializes Peer to/from JSON string for settings storage

### Rules
- commonMain must not reference NsdManager or JmDNS directly
- All platform-specific mDNS code deferred to actual implementations
- PeerRegistry must survive process death (persisted via Settings)

Output only code. No explanation.
```

---

---

# PROMPT 6 — Pairing & Trust (PIN Flow)

```
You are working on FluxSync :core module. Prompts 1–5 are complete.

## Task
Generate the pairing and trust subsystem in:
:core/src/commonMain/kotlin/com/fluxsync/core/pairing/

### PairingSession.kt
- Generates a random 6-digit numeric PIN: fun generatePin(): String
- Starts a 60-second expiry timer (ProtocolConstants.PIN_EXPIRY_MS) using a coroutine
- On expiry: transitions to PairingState.Expired
- suspend fun confirmPin(input: String): Boolean — returns true if input matches generated PIN
- On confirmation: derives AES-256 key from PIN using PBKDF2 (expect fun deriveKey(pin: String, salt: ByteArray): ByteArray)
- Emits PairingState via StateFlow

### PairingState.kt
sealed class PairingState {
  object Idle: PairingState()
  data class AwaitingConfirmation(val pin: String, val expiresAt: Long): PairingState()
  data class Confirmed(val encryptedKey: ByteArray): PairingState()
  object Expired: PairingState()
  object Failed: PairingState()
}

### TrustStore.kt
- Persists trusted peer records using multiplatform-settings
- data class TrustedRecord(val peerId: String, val moniker: String, val keyHex: String)
- fun save(record: TrustedRecord)
- fun delete(peerId: String)
- fun getAll(): List<TrustedRecord>
- fun isTrusted(peerId: String): Boolean
- Serialize TrustedRecord as JSON string in settings

### expect declarations
expect fun deriveKey(pin: String, salt: ByteArray): ByteArray
— in :core/src/commonMain/kotlin/com/fluxsync/core/platform/CryptoOps.kt
Generate empty actual stubs in androidMain and desktopMain.

### Rules
- PIN never logged
- Key bytes zeroed after use where possible
- No hardcoded salt — generate random salt per pairing session, store alongside key

Output only code. No explanation.
```

---

---

# PROMPT 7 — Platform Actuals: Android

```
You are working on FluxSync Android module. :core commonMain expect declarations from Prompts 3, 5, 6 exist as empty stubs in androidMain.

## Task
Implement all androidMain actual declarations:

### FileOps.android.kt
actual fun readFileChunk(path: String, offset: Long, length: Int): ByteArray
- Use RandomAccessFile, seek to offset, read exactly length bytes

actual fun preallocateFile(path: String, size: Long)
- Use FileOutputStream + FileChannel.truncate or fallocate via Os.posix_fallocate if available, fallback to writing zeros in 1MB blocks

actual fun writeMetaJson(path: String, meta: PartFileMeta)
- Serialize meta using Json.encodeToString, write atomically (write to .tmp, rename)

actual fun computeXxHash(path: String): String
- Stream file in 1MB chunks, compute XXHash-64 using net.openhft:zero-allocation-hashing (add to androidMain deps) — return lowercase hex string

actual fun renameFile(from: String, to: String)
- Use File.renameTo, throw IOException on failure

### PeerDiscovery.android.kt
actual class PeerDiscovery — implement using Android NsdManager
- advertise(): NsdManager.registerService with type _fluxsync._tcp.local.
- start(): NsdManager.discoverServices, emit Found/Lost as peers appear/disappear
- stop(): unregisterService + stopServiceDiscovery
- Handle all NsdManager listener callbacks, post to Flow via Channel

### CryptoOps.android.kt
actual fun deriveKey(pin: String, salt: ByteArray): ByteArray
- Use javax.crypto.SecretKeyFactory PBKDF2WithHmacSHA256
- iterations: 100_000, keyLength: 256 bits

### ForegroundTransferService.kt (androidMain, NOT commonMain)
- Android Service subclass
- Runs in foreground with PARTIAL_WAKE_LOCK
- Notification channel: "fluxsync_transfer" with importance HIGH
- Notification: non-dismissible while transfer active, shows filename + speed + percentage
- Exposes a Binder for the UI to bind and observe StateFlow<TransferState>
- Acquires WakeLock on startTransfer(), releases on complete/abort

### AndroidManifest additions (generate as manifest merge file)
- MANAGE_EXTERNAL_STORAGE
- FOREGROUND_SERVICE
- FOREGROUND_SERVICE_DATA_SYNC
- CHANGE_WIFI_MULTICAST_STATE
- ACCESS_WIFI_STATE
- INTERNET
- WAKE_LOCK
- NSD_PERMISSIONS (ACCESS_NETWORK_STATE)

### Rules
- No blocking calls on main thread
- All file ops on Dispatchers.IO
- NsdManager callbacks dispatched to Dispatchers.Main then re-emitted to Flow

Output only code. No explanation.
```

---

---

# PROMPT 8 — Platform Actuals: Desktop (Windows/JVM)

```
You are working on FluxSync :desktop module. :core commonMain expect declarations from Prompts 3, 5, 6 exist as empty stubs in desktopMain.

## Task
Implement all desktopMain actual declarations:

### FileOps.desktop.kt
actual fun readFileChunk(path: String, offset: Long, length: Int): ByteArray
- RandomAccessFile, seek, read

actual fun preallocateFile(path: String, size: Long)
- Use FileChannel.open with StandardOpenOption.WRITE + SPARSE on Windows via Files.createFile then channel.write at size-1 position; catch UnsupportedOperationException and fall back to zero-fill

actual fun writeMetaJson(path: String, meta: PartFileMeta)
- Atomic write via temp file + Files.move(ATOMIC_MOVE)

actual fun computeXxHash(path: String): String
- Stream in 1MB chunks using net.openhft:zero-allocation-hashing on JVM

actual fun renameFile(from: String, to: String)
- Files.move with REPLACE_EXISTING

### PeerDiscovery.desktop.kt
actual class PeerDiscovery — implement using JmDNS (add dependency: javax.jmdns:jmdns:3.5.9)
- advertise(): JmDNS.registerService
- start(): JmDNS.addServiceListener for _fluxsync._tcp.local., emit Found/Lost
- stop(): JmDNS.unregisterAllServices + close

### CryptoOps.desktop.kt
actual fun deriveKey(pin: String, salt: ByteArray): ByteArray
- Same PBKDF2WithHmacSHA256, 100_000 iterations, 256-bit key via javax.crypto

### AdbPoller.kt (desktopMain only)
- Coroutine-based loop polling every 3 seconds
- Executes: ProcessBuilder("adb", "devices") — parses output for connected devices
- On device detected: executes adb reverse tcp:8765 tcp:8765 and adb reverse tcp:8766 tcp:8766
- On device removed: emits LinkType.ADB removal event to ChunkDispatcher
- Exposes Flow<AdbEvent> where AdbEvent is sealed: Connected(serial), Disconnected(serial)
- If adb binary not found in PATH (IOException on ProcessBuilder): emit AdbNotFound event

### SystemTrayManager.kt (desktopMain only)
- Uses java.awt.SystemTray
- Tray icon: FluxSync icon (embed as resource)
- Menu items: "Open FluxSync", "Quit"
- On window close event intercepted: hide window, show tray icon
- On "Open FluxSync" click: restore window

### WindowController.kt (desktopMain only)
- Intercepts Compose Desktop window close request
- Delegates to SystemTrayManager instead of exiting
- Exposes fun exit() for actual quit

### Rules
- All file ops on Dispatchers.IO
- AdbPoller on Dispatchers.IO with SupervisorJob
- No UI code in this prompt — pure platform logic only

Output only code. No explanation.
```

---

---

# PROMPT 9 — Shared UI: Design System & Theme

```
You are working on FluxSync :ui module. All :core logic from Prompts 1–8 exists.

## Task
Generate the shared Compose Multiplatform design system in:
:ui/src/commonMain/kotlin/com/fluxsync/ui/theme/

### Color.kt
Define a strict Dark Mode only palette:
- Background: #0D0D0D
- Surface: #1A1A1A
- SurfaceVariant: #242424
- Primary: #4FC3F7      (light blue accent)
- PrimaryVariant: #0288D1
- Success: #66BB6A
- Warning: #FFA726
- Error: #EF5350
- OnBackground: #E8E8E8
- OnSurface: #C8C8C8
- OnPrimary: #000000
- Divider: #2C2C2C
- SpeedGreen: #00E676    (live speed metric)

### Type.kt
- Define TextStyles for: DisplayLarge, DisplaySmall, TitleLarge, TitleMedium, BodyLarge, BodyMedium, BodySmall, LabelMono (monospace, for speed/size metrics and console log)
- Use Inter for body/titles (embed or reference system font), JetBrains Mono for LabelMono

### Theme.kt
- FluxSyncTheme composable wrapping MaterialTheme
- Always dark — never check system setting
- Apply custom colors and typography

### Spacing.kt
object Spacing { val xs=4.dp, sm=8.dp, md=16.dp, lg=24.dp, xl=32.dp, xxl=48.dp }

### Shape.kt
- Define card radius (12.dp), button radius (8.dp), chip radius (100.dp)

### Rules
- No hardcoded colors outside Color.kt
- No hardcoded dimensions outside Spacing.kt / Shape.kt
- No platform-specific imports in commonMain

Output only code. No explanation.
```

---

---

# PROMPT 10 — Shared UI: Components

```
You are working on FluxSync :ui module. Prompt 9 (design system) is complete.

## Task
Generate reusable Compose components in:
:ui/src/commonMain/kotlin/com/fluxsync/ui/components/

### SpeedGauge.kt
@Composable fun SpeedGauge(bytesPerSecond: Long, modifier: Modifier)
- Displays speed as "XX.X MB/s" or "X.X GB/s" auto-scaled
- Color: SpeedGreen when > 0, OnSurface when 0
- Font: LabelMono, large
- Animated: animateFloatAsState on value changes

### ProgressBar.kt
@Composable fun FluxProgressBar(progress: Float, modifier: Modifier)
- Custom drawn progress bar (Canvas), not Material LinearProgressIndicator
- Fill color: Primary, track color: SurfaceVariant
- Animated with animateFloatAsState
- Shows percentage label inside bar if width permits

### PeerChip.kt
@Composable fun PeerChip(peer: Peer, isConnected: Boolean, onClick: () -> Unit)
- Shows moniker + connection indicator dot (green/grey)
- Trusted peers show a small lock icon

### TransferQueueItem.kt
@Composable fun TransferQueueItem(file: TransferFile, state: TransferState, onCancel: () -> Unit)
- Shows filename, size, state label, progress if Transferring
- Cancel button only visible during Transferring/Negotiating

### PinDisplay.kt
@Composable fun PinDisplay(pin: String, expiresInSeconds: Int)
- Shows 6 digits in large boxes with spacing
- Countdown ring/arc around the display
- Fades to Error color as time runs out

### ConsoleLog.kt
@Composable fun ConsoleLog(entries: List<LogEntry>, modifier: Modifier)
data class LogEntry(val timestamp: String, val channel: String, val level: LogLevel, val message: String)
enum class LogLevel { DEBUG, INFO, WARN, ERROR }
- Scrollable LazyColumn, auto-scrolls to bottom on new entry
- Level-colored prefix: ERROR=Error, WARN=Warning, INFO=Primary, DEBUG=OnSurface dimmed
- Font: LabelMono

### StorageBanner.kt
@Composable fun StorageBanner(partFilesBytes: Long, onEmptyTrash: () -> Unit)
- Shows "X.X MB of incomplete transfers" with "Empty Trash" button
- Hidden if partFilesBytes == 0L

### AdbWarningBanner.kt
@Composable fun AdbWarningBanner(onDismiss: () -> Unit)
- Yellow warning card: "adb not found in PATH. Wired transfer disabled. Install Android Platform Tools."
- Link text that opens https://developer.android.com/tools/releases/platform-tools

### Rules
- All composables accept Modifier parameter
- No state inside components — stateless/hoisted pattern only
- No platform imports

Output only code. No explanation.
```

---

---

# PROMPT 11 — Shared UI: Screens

```
You are working on FluxSync :ui module. Prompts 9–10 are complete.

## Task
Generate all screens in:
:ui/src/commonMain/kotlin/com/fluxsync/ui/screens/

### Navigation.kt
- Define sealed class Screen: Discovery, Pairing(peerId), ActiveTransfer(transferId), Settings, DevConsole
- Use a simple backstack (MutableList<Screen> in a ViewModel) — no third-party nav library
- AppNavigator class: fun navigate(screen: Screen), fun back(), val currentScreen: StateFlow<Screen>

### DiscoveryScreen.kt
@Composable fun DiscoveryScreen(viewModel: DiscoveryViewModel)
- Shows list of discovered peers via PeerChip
- "+" FAB to open manual IP entry dialog
- Manual entry dialog: TextField for IP:port, Connect button
- Empty state: pulsing animation + "Searching for devices…"
- Shows VPN warning banner if VPN detected (bool from VM)

### PairingScreen.kt
@Composable fun PairingScreen(viewModel: PairingViewModel)
- Shows PinDisplay with generated PIN
- "Confirm" button — enabled only if other device has shown same PIN
- On Expired: auto-pop back with snackbar "PIN expired"
- On Confirmed: navigate to ActiveTransfer

### ActiveTransferScreen.kt
@Composable fun ActiveTransferScreen(viewModel: TransferViewModel)
- Top: PeerChip (connected peer), connection quality indicator
- Center: SpeedGauge + FluxProgressBar + ETA display ("~2m 34s remaining")
- Queue section: LazyColumn of TransferQueueItem
- "Add Files" and "Add Folder" buttons — always separate, never combined (NTR-104)
- "Show Details" toggle button — expands ConsoleLog panel at bottom
- StorageBanner if applicable
- AdbWarningBanner if applicable

### SettingsScreen.kt
@Composable fun SettingsScreen(viewModel: SettingsViewModel)
- ChunkSize slider (512KB to 16MB, snap to: 512KB, 1MB, 2MB, 4MB, 8MB, 16MB), shows current value
- Speed refresh rate toggle: "500ms" / "2s average"
- Trusted Devices list: each row shows moniker + "Remove" button
- App version display

### DevConsoleScreen.kt
@Composable fun DevConsoleScreen(viewModel: ConsoleViewModel)
- Full-screen ConsoleLog
- Filter chips: ALL / ERROR / WARN / INFO
- "Copy to Clipboard" button
- "Clear" button

### Rules
- All screens receive ViewModel — no direct core access from composables
- ViewModel classes: define interfaces/stubs only in this prompt (implementations in Prompt 12)
- collectAsState() for all StateFlow observations
- No hardcoded strings — define all in a Strings.kt object in :ui

Output only code. No explanation.
```

---

---

# PROMPT 12 — ViewModels & State Layer

```
You are working on FluxSync :ui module. Prompts 9–11 are complete. :core engine from Prompts 1–8 exists.

## Task
Generate all ViewModels in:
:ui/src/commonMain/kotlin/com/fluxsync/ui/viewmodel/

Each ViewModel must:
- Extend a base class: abstract class FluxViewModel(protected val scope: CoroutineScope)
- Expose only StateFlow / SharedFlow — no exposed MutableState
- Handle all exceptions with scope.launch { runCatching { } }

### DiscoveryViewModel
- Injected: PeerRegistry, PeerDiscovery, ManualEntryDiscovery, VpnDetector (expect class, stub)
- Exposes: peers: StateFlow<List<Peer>>, isSearching: StateFlow<Boolean>, vpnDetected: StateFlow<Boolean>
- fun startDiscovery(), fun stopDiscovery(), fun connectManual(input: String)
- On connectManual: validate via ManualEntryDiscovery, add to PeerRegistry, navigate to Pairing

### PairingViewModel
- Injected: PairingSession, TrustStore, AppNavigator
- Exposes: pairingState: StateFlow<PairingState>
- fun initiatePairing(peer: Peer), fun confirmPin(input: String)
- On Confirmed: save to TrustStore, navigate to ActiveTransfer

### TransferViewModel
- Injected: TransferSession (sender) or ReceiverSession, AckTracker, ChunkDispatcher, AppNavigator
- Exposes:
  - transferState: StateFlow<TransferState>
  - speedBytesPerSec: StateFlow<Long>   (calculated from acked bytes delta per refresh interval)
  - etaSeconds: StateFlow<Long>
  - queue: StateFlow<List<TransferFile>>
  - partFilesBytes: StateFlow<Long>
  - adbAvailable: StateFlow<Boolean>
  - logEntries: StateFlow<List<LogEntry>>
- fun addFiles(paths: List<String>), fun addFolder(path: String)
- fun cancelCurrent()
- fun emptyTrash()
- Speed calculation: sliding window over last 2 seconds of acked bytes

### SettingsViewModel
- Injected: Settings (multiplatform-settings), TrustStore
- Exposes: chunkSizeBytes: StateFlow<Int>, speedRefreshFast: StateFlow<Boolean>, trustedDevices: StateFlow<List<TrustedRecord>>
- fun setChunkSize(bytes: Int), fun toggleSpeedRefresh(), fun removeTrustedDevice(peerId: String)

### ConsoleViewModel
- Injected: log entries source (SharedFlow<LogEntry> from Kermit sink)
- Exposes: entries: StateFlow<List<LogEntry>>, filter: StateFlow<LogLevel?>
- fun setFilter(level: LogLevel?), fun clear()

### KermitLogSink.kt
- Implement a Kermit LogWriter that emits LogEntry to a MutableSharedFlow<LogEntry>
- Wire this as the global Kermit logger in platform main entry points

### Rules
- No Android/Desktop imports
- CoroutineScope provided by platform shell (not created inside VM)
- All ViewModels instantiated via a simple ServiceLocator (no DI framework)

Output only code. No explanation.
```

---

---

# PROMPT 13 — Android Shell & Entry Point

```
You are working on FluxSync :android module. All :core and :ui work from prior prompts exists.

## Task
Generate the Android application shell in :android/src/main/

### MainActivity.kt
- ComponentActivity
- setContent { FluxSyncTheme { AppRoot(navigator, viewModelStore) } }
- Binds to ForegroundTransferService via ServiceConnection
- Requests MANAGE_EXTERNAL_STORAGE permission on first launch with rationale dialog
- Handles edge-to-edge display (WindowCompat.setDecorFitsSystemWindows = false)

### AppRoot.kt
- @Composable fun AppRoot(navigator: AppNavigator, store: ViewModelStore)
- Switches between screens based on navigator.currentScreen
- Wraps in a Scaffold with no top bar (full custom)
- Passes correct ViewModel to each screen

### ServiceLocator.android.kt
- Instantiates all ViewModels with Android-aware CoroutineScope (lifecycleScope)
- Wires: PeerDiscovery (NsdManager), TrustStore, PairingSession, ChunkDispatcher, AckTracker, ControlChannelManager, TransferSession/ReceiverSession
- Singleton pattern

### VpnDetector.android.kt
actual class VpnDetector {
  actual fun isVpnActive(): Boolean
  — checks ConnectivityManager for VPN NetworkCapabilities
}

### Rules
- No business logic in MainActivity
- All ViewModel construction in ServiceLocator
- minSdk 30, targetSdk 35
- compileSdk 35
- Enable R8 full mode in release build

Output only code. No explanation.
```

---

---

# PROMPT 14 — Desktop Shell & Entry Point

```
You are working on FluxSync :desktop module. All :core and :ui work from prior prompts exists.

## Task
Generate the Desktop application shell in :desktop/src/main/

### Main.kt
fun main() = application {
  val windowState = rememberWindowState(width=1100.dp, height=720.dp)
  val navigator = ServiceLocator.navigator
  Window(
    onCloseRequest = { SystemTrayManager.minimizeToTray() },
    state = windowState,
    title = "FluxSync",
    undecorated = false,
  ) {
    FluxSyncTheme { AppRoot(navigator, ServiceLocator.viewModelStore) }
  }
}
- Wire SystemTrayManager on window creation
- Start AdbPoller in a SupervisorScope on application launch
- On AdbPoller AdbNotFound: set adbAvailable = false in TransferViewModel

### ServiceLocator.desktop.kt
- Instantiates all ViewModels with application-scoped CoroutineScope
- Wires: PeerDiscovery (JmDNS), TrustStore, PairingSession, ChunkDispatcher, AckTracker, ControlChannelManager, AdbPoller, SystemTrayManager

### VpnDetector.desktop.kt
actual class VpnDetector {
  actual fun isVpnActive(): Boolean
  — enumerate NetworkInterface, check for tun/tap interface names or PPP; return true if found
}

### build config
- Compose Desktop packaging:
  - targetFormats: Msi, Exe (Windows)
  - packageName: "FluxSync"
  - packageVersion: "1.0.0"
  - windows { dirChooser = true, perUserInstall = true, menuGroup = "FluxSync" }
  - Embed JDK 21

### Rules
- No blocking on main thread
- AdbPoller SupervisorJob survives individual poll failures
- System tray icon: embed 32x32 PNG as resource at desktop/src/main/resources/tray_icon.png (generate a placeholder reference only)

Output only code. No explanation.
```

---

---

# PROMPT 15 — Logging Infrastructure

```
You are working on FluxSync. All modules from Prompts 1–14 exist.

## Task
Generate the structured logging system across all modules.

### LogManager.kt (:core/commonMain)
- Singleton object
- fun log(channel: String, level: LogLevel, message: String, throwable: Throwable? = null)
- Formats per TR-403: "[YYYY-MM-DD HH:MM:SS TZ] [CHANNEL] [LEVEL] - Message"
- Timestamp: expect fun currentTimestampFormatted(): String (actual in androidMain + desktopMain)
- Dispatches to: (1) KermitLogSink for in-app console, (2) RollingFileLogger

### RollingFileLogger.kt (:core/commonMain)
- Writes to a log file path provided at init
- Rolling: when current file exceeds 5MB, rename to session_N.log, open new file
- Retains max 3 session files — deletes oldest on overflow
- File writes via expect fun appendToFile(path: String, text: String) — actual stubs in androidMain/desktopMain
- Thread-safe via Mutex

### Timestamp actuals
androidMain: use java.time.ZonedDateTime (API 26+, minSdk 30 so safe)
desktopMain: same java.time.ZonedDateTime

### appendToFile actuals
Both platforms: FileOutputStream(path, append=true).bufferedWriter().use { it.write(text) }

### Log path resolution
- Android: Context.filesDir / "logs" (injected at app start)
- Desktop: System.getProperty("user.home") / ".fluxsync" / "logs"

### Wire-up
- In Android ServiceLocator: LogManager.init(logDir = context.filesDir / "logs")
- In Desktop ServiceLocator: LogManager.init(logDir = homeDir / ".fluxsync/logs")
- Replace all System.out / println with LogManager.log calls throughout codebase

Output only code. No explanation.
```

---

---

# PROMPT 16 — Conflict Resolution & Batch Queue

```
You are working on FluxSync :core module. All prior prompts are complete.

## Task
Generate batch and conflict resolution logic in:
:core/src/commonMain/kotlin/com/fluxsync/core/engine/

### TransferQueue.kt
- Thread-safe queue backed by ArrayDeque + Mutex
- suspend fun enqueue(file: TransferFile)
- fun peek(): TransferFile?
- suspend fun dequeue(): TransferFile?
- val size: StateFlow<Int>
- fun snapshot(): List<TransferFile>
- fun cancel(transferId: String): Boolean

### ConflictResolver.kt
- fun resolve(desiredPath: String, existingPaths: Set<String>): String
- If desiredPath not in existingPaths: return desiredPath unchanged
- Else: append (1), (2), … until unique
- Example: "video.mp4" → "video(1).mp4" → "video(2).mp4"
- Handles paths with no extension correctly
- Pure function, no I/O

### PartFileScanner.kt
- expect class PartFileScanner
- fun scanDirectory(path: String): List<PartFileInfo>
  data class PartFileInfo(val partPath: String, val metaPath: String?, val sizeBytes: Long)
- fun totalBytes(infos: List<PartFileInfo>): Long
- fun deleteAll(infos: List<PartFileInfo>) — deletes both .part and .json files
Generate actual stubs in androidMain and desktopMain.

### CheckpointManager.kt
- fun loadMeta(partPath: String): PartFileMeta? — reads meta.json alongside .part, returns null if missing
- fun saveMeta(partPath: String, meta: PartFileMeta) — writes via writeMetaJson expect fun
- fun buildResumeRequest(meta: PartFileMeta): List<Int> — returns list of missing sequenceIds
  (totalChunks range minus receivedChunks)

Output only code. No explanation.
```

---

---

# PROMPT 17 — End-to-End Integration & Wiring

```
You are working on FluxSync. All modules from Prompts 1–16 exist. This is the final integration pass.

## Task
Wire everything together and fix any integration gaps.

### 1. ServiceLocator completeness check
Review ServiceLocator.android.kt and ServiceLocator.desktop.kt.
Ensure every class that has a dependency is properly constructed and injected.
Fix any missing wiring. Add any missing constructor parameters.

### 2. TransferSession ↔ ReceiverSession coordination
- Sender flow: DiscoveryScreen → PairingScreen → send TransferRequest → TransferSession.start()
- Receiver flow: ControlServer receives TransferRequest → ReceiverSession.start() → notify UI
- Ensure ControlChannelManager routes incoming TransferRequest to ReceiverSession and outgoing to ControlServer correctly

### 3. Chunk data channel
- Implement DataChannelServer.kt and DataChannelClient.kt in :core/commonMain using Ktor TCP raw socket (not WebSocket) on ProtocolConstants.DATA_PORT
- Sender: ChunkDispatcher serializes ChunkPayload as: [4 bytes transferId length][transferId UTF-8][4 bytes sequenceId][4 bytes data length][data bytes]
- Receiver: DataChannelServer reads this framing, reconstructs ChunkPayload, passes to ReceiverSession.writeChunk()

### 4. Speed metric implementation
In TransferViewModel:
- Every 500ms (or 2s depending on setting): calculate (totalAckedBytes - previousAckedBytes) / intervalSeconds
- Expose as speedBytesPerSec StateFlow
- ETA: (totalBytes - ackedBytes) / speedBytesPerSec

### 5. Empty Trash wiring
- TransferViewModel.emptyTrash(): calls PartFileScanner.scanDirectory on the transfers directory, then PartFileScanner.deleteAll
- Refreshes partFilesBytes StateFlow after deletion

### 6. Settings persistence
Verify all Settings keys use unique string constants. Define them in a SettingsKeys object in :core/commonMain.

### 7. Compile check instructions
After generating all files, output a checklist:
- List every expect declaration and confirm its actual exists in both androidMain and desktopMain
- List every interface and confirm its implementation exists
- List every ViewModel and confirm its screen wiring exists

Output only code and the checklist. No other explanation.
```

---

---

# PROMPT 18 — Tests

```
You are working on FluxSync. All production code from Prompts 1–17 exists.

## Task
Generate a comprehensive test suite.

### :core/commonTest — use kotlin.test + Turbine + MockK (where mockable)

#### AckTrackerTest.kt
- Test: markAcked records correctly
- Test: isComplete returns true only when all sequenceIds acked
- Test: getUnacknowledged returns correct diff
- Test: reset clears state

#### ConflictResolverTest.kt
- Test: no conflict → unchanged
- Test: single conflict → (1) appended
- Test: multiple conflicts → increments correctly
- Test: file with no extension handled
- Test: file with multiple dots in name handled

#### ChunkDispatcherTest.kt (use fakes, not mocks)
- Test: dispatch to single link
- Test: dispatch round-robins across multiple links
- Test: link removal triggers re-queue of in-flight chunks

#### PartFileMeta serialization test
- Test: roundtrip serialize/deserialize PartFileMeta

#### PairingSessionTest.kt
- Test: generatePin returns 6-digit string
- Test: confirmPin returns true on match
- Test: confirmPin returns false on mismatch
- Test: state transitions to Expired after timeout (use TestCoroutineScheduler to advance time)

#### ControlMessageSerializerTest.kt
- Test: each ControlMessage subtype serializes and deserializes correctly
- Test: unknown type throws SerializationException

### :android/androidTest — use androidx.test

#### ForegroundTransferServiceTest.kt
- Test: service starts and acquires WakeLock
- Test: notification is posted on transfer start
- Test: WakeLock released on transfer complete

### Rules
- No real file I/O in commonTest — use in-memory fakes
- Use runTest for all coroutine tests
- 100% of public functions in AckTracker, ConflictResolver, PairingSession must be covered

Output only test code. No explanation.
```

---

---

# PROMPT 19 — Final Hardening

```
You are doing a final production hardening pass on FluxSync. All code from Prompts 1–18 exists.

## Task — apply every item below:

### 1. ProGuard / R8 rules (:android)
Generate proguard-rules.pro:
- Keep all @Serializable classes and their companion objects
- Keep Ktor internal classes
- Keep PlatformSocket actual class
- Keep ForegroundTransferService
- Keep NsdManager listener classes
- -dontobfuscate for debug builds, full obfuscation for release

### 2. Null safety audit
Search all files for any !! (non-null assertion operator).
Replace every !! with a proper null check, Elvis operator, or requireNotNull with a descriptive message.
List every replacement made.

### 3. Error boundary in all ViewModels
Confirm every scope.launch block is wrapped in runCatching or has a CoroutineExceptionHandler.
Add missing handlers. Log all caught exceptions via LogManager.

### 4. StateFlow initial values
Confirm every StateFlow in every ViewModel has a meaningful initial value (never an uninitialized sentinel like -1L unless documented).

### 5. Resource cleanup
Confirm every Closeable (sockets, file handles, JmDNS, NsdManager) is closed in a finally block or via use { }.
Add missing cleanup.

### 6. Memory leak prevention
- Android: confirm all ViewModels use lifecycleScope or a scope that is cancelled on destroy
- Desktop: confirm application scope is cancelled on window close via onCloseRequest

### 7. Missing UI strings
Audit all composables for hardcoded strings. Move every hardcoded string to Strings.kt.

### 8. Accessibility
Add contentDescription to every Icon and IconButton in the UI module.

### 9. Build variants (:android)
- debug: debuggable=true, minify=false, applicationIdSuffix=".debug"
- release: minify=true, shrinkResources=true, signingConfig=release (define placeholder signingConfig with TODO comment)

### 10. Output
For each item: output the changed files in full.
At the end: output a single "PRODUCTION READY CHECKLIST" confirming every TRD requirement ID (NTR-101 through TR-603) is implemented, with a one-line note on where each is satisfied.

Output changed files + checklist. No other explanation.
```

---

*End of FluxSync Codex Prompt Suite — 19 prompts, 0 gaps.*
