package com.yourorg.objectcapture.colmap

import java.io.File

class ImageListWriter {
    fun write(file: File, images: List<ColmapImage>) {
        val lines = images.sortedBy { it.id }.joinToString("\n") { it.name }
        file.writeText(lines + "\n")
    }
}
