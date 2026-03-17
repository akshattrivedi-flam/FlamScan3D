package com.yourorg.objectcapture.network

import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File

class ApiService(
    private val baseUrl: String,
    private val client: OkHttpClient,
    moshi: Moshi
) {
    private val uploadAdapter = moshi.adapter(UploadResponse::class.java)
    private val statusAdapter = moshi.adapter(JobStatusResponse::class.java)

    fun uploadZip(zipFile: File, onProgress: (Int) -> Unit): String {
        val requestBody = ProgressRequestBody(zipFile) { bytes, total ->
            val progress = if (total > 0L) ((bytes.toDouble() / total) * 100).toInt() else 0
            onProgress(progress.coerceIn(0, 100))
        }
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", zipFile.name, requestBody)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/upload")
            .post(multipart)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Upload failed: ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val parsed = uploadAdapter.fromJson(body)
                ?: throw IllegalStateException("Invalid upload response")
            return parsed.job_id
        }
    }

    fun getStatus(jobId: String): JobStatusResponse {
        val request = Request.Builder()
            .url("$baseUrl/status/$jobId")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Status failed: ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            return statusAdapter.fromJson(body)
                ?: throw IllegalStateException("Invalid status response")
        }
    }
}

data class UploadResponse(
    val job_id: String
)

data class JobStatusResponse(
    val status: String,
    val progress: Int,
    val model_url: String?
)
