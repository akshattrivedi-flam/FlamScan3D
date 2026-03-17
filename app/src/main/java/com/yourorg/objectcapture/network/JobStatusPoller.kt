package com.yourorg.objectcapture.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class JobStatusPoller(
    private val apiService: ApiService
) {
    fun poll(jobId: String, intervalMs: Long = 5000): Flow<JobStatusResponse> = flow {
        while (true) {
            val status = apiService.getStatus(jobId)
            emit(status)
            if (status.status == "completed" || status.status == "failed") {
                break
            }
            delay(intervalMs)
        }
    }
}
