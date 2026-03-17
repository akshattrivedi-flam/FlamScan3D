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
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            zipFile(sourceDir, sourceDir, zos)
        }
    }

    private fun zipFile(rootDir: File, file: File, zos: ZipOutputStream) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> zipFile(rootDir, child, zos) }
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
