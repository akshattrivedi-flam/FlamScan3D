package com.yourorg.objectcapture.storage

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class MetadataReader {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(FrameMetadata::class.java)

    fun read(file: File): FrameMetadata? {
        return try {
            adapter.fromJson(file.readText())
        } catch (t: Throwable) {
            null
        }
    }

    fun readAll(dir: File): List<FrameMetadata> {
        val files = dir.listFiles { file -> file.extension.lowercase() == "json" } ?: return emptyList()
        return files.sortedBy { it.name }.mapNotNull { read(it) }
    }
}
