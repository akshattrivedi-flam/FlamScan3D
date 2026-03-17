package com.yourorg.objectcapture.model

enum class CaptureState {
    READY,
    CAPTURING,
    REVIEWING,
    PREPARE_RECONSTRUCTION,
    RECONSTRUCTING,
    VIEWING_MODEL,
    COMPLETED,
    FAILED
}
