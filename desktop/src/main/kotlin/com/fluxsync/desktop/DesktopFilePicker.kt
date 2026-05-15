package com.fluxsync.desktop

import java.awt.FileDialog
import java.awt.Frame
import javax.swing.JFileChooser

object DesktopFilePicker {
    fun selectFiles(): List<String> {
        val dialog = FileDialog(null as Frame?, "Select files", FileDialog.LOAD)
        dialog.isMultipleMode = true
        dialog.isVisible = true
        return dialog.files?.map { it.absolutePath }.orEmpty()
    }

    fun selectFolder(): String? {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else {
            null
        }
    }
}
