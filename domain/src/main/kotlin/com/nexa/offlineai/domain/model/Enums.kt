package com.nexa.offlineai.domain.model

enum class ProviderType {
    LOCAL_GEMMA,
    MOCK,
    REMOTE_FALLBACK,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AccentOption {
    AURORA,
    AMBER,
    TIDAL,
    ROSEWOOD,
}

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

enum class MessageState {
    COMPLETE,
    STREAMING,
    FAILED,
}

enum class ReminderStatus {
    UPCOMING,
    COMPLETED,
    SNOOZED,
    MISSED,
}

enum class ModelState {
    UNKNOWN,
    CHECKING_FILES,
    LOADING,
    READY,
    UNAVAILABLE,
    INSUFFICIENT_MEMORY,
    INFERENCE_ERROR,
    UNSUPPORTED_DEVICE,
    REMOTE_ONLY,
}
