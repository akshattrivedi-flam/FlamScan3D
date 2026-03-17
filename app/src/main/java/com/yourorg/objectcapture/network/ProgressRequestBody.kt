package com.yourorg.objectcapture.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File

class ProgressRequestBody(
    private val file: File,
    private val contentType: String = "application/zip",
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
) : RequestBody() {

    override fun contentType() = contentType.toMediaType()

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        file.source().use { source ->
            var written = 0L
            val buffer = okio.Buffer()
            var read: Long
            while (source.read(buffer, 8 * 1024).also { read = it } != -1L) {
                sink.write(buffer, read)
                written += read
                onProgress(written, total)
            }
        }
    }
}
