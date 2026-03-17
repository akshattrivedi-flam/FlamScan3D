package com.yourorg.objectcapture.core

data class UploadUiState(
    val inProgress: Boolean = false,
    val progress: Int = 0,
    val jobId: String? = null,
    val status: String = "",
    val modelUrl: String? = null,
    val error: String? = null
) {
    val isCompleted: Boolean get() = status == "completed" && modelUrl != null
    val isFailed: Boolean get() = status == "failed"
}
