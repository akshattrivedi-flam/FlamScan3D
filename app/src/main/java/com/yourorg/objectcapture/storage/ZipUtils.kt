package com.yourorg.objectcapture.storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {
    fun zipDirectory(sourceDir: File, outputFile: File) {
        if (!sourceDir.exists()) return
        outputFile.parentFile?.mkdirs()
        val excludePath = outputFile.canonicalFile.toPath()
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            zipFile(sourceDir, sourceDir, zos, excludePath)
        }
    }

    private fun zipFile(rootDir: File, file: File, zos: ZipOutputStream, excludePath: java.nio.file.Path) {
        if (file.canonicalFile.toPath() == excludePath) {
            return
        }
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> zipFile(rootDir, child, zos, excludePath) }
        } else {
            val entryName = rootDir.toPath().relativize(file.toPath()).toString()
            zos.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }
}
