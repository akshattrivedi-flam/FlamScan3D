package com.yourorg.objectcapture.storage

import com.google.ar.core.Pose
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class DraftManager {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(DraftState::class.java)

    fun saveDraft(sessionDir: File, state: DraftState) {
        val file = File(sessionDir, "draft.json")
        file.writeText(adapter.indent("  ").toJson(state))
    }

    fun loadDraft(sessionDir: File): DraftState? {
        val file = File(sessionDir, "draft.json")
        if (!file.exists()) return null
        return adapter.fromJson(file.readText())
    }

    fun findLatestDraft(rootDir: File): DraftState? {
        val dirs = rootDir.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.name } ?: return null
        for (dir in dirs) {
            val draft = loadDraft(dir)
            if (draft != null) return draft
        }
        return null
    }
}

data class DraftState(
    val sessionId: String,
    val sessionPath: String,
    val imageCount: Int,
    val coverageBins: List<Int>,
    val lastPoseTranslation: List<Float>? = null,
    val lastPoseRotation: List<Float>? = null
) {
    fun toPose(): Pose? {
        return if (lastPoseTranslation != null && lastPoseRotation != null &&
            lastPoseTranslation.size == 3 && lastPoseRotation.size == 4
        ) {
            Pose.makePose(lastPoseTranslation.toFloatArray(), lastPoseRotation.toFloatArray())
        } else {
            null
        }
    }
}
