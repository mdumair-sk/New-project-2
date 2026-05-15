package com.fluxsync.core.engine

object ConflictResolver {
    fun resolve(desiredPath: String, existingPaths: Set<String>): String {
        if (desiredPath !in existingPaths) return desiredPath

        val slashIndex = desiredPath.lastIndexOf('/')
        val directory = if (slashIndex >= 0) desiredPath.substring(0, slashIndex + 1) else ""
        val name = desiredPath.substring(slashIndex + 1)
        val dotIndex = name.lastIndexOf('.')
        val hasExtension = dotIndex > 0
        val base = if (hasExtension) name.substring(0, dotIndex) else name
        val extension = if (hasExtension) name.substring(dotIndex) else ""

        var counter = 1
        while (true) {
            val candidate = "$directory$base($counter)$extension"
            if (candidate !in existingPaths) return candidate
            counter += 1
        }
    }
}
