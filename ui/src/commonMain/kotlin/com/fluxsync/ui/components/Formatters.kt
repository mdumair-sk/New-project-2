package com.fluxsync.ui.components

fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit += 1
    }
    return if (unit == 0) "${bytes} ${units[unit]}" else "${round1(value)} ${units[unit]}"
}

fun formatSpeed(bytesPerSecond: Long): String = "${formatBytes(bytesPerSecond)}/s"

fun formatEta(seconds: Long): String {
    if (seconds <= 0L) return "0s"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) "${minutes}m ${remainingSeconds}s" else "${remainingSeconds}s"
}

private fun round1(value: Double): String {
    val scaled = (value * 10.0).toLong()
    return "${scaled / 10}.${scaled % 10}"
}
