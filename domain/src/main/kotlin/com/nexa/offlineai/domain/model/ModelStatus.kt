package com.nexa.offlineai.domain.model

data class ModelStatus(
    val providerType: ProviderType,
    val state: ModelState,
    val detail: String,
    val progress: Float? = null,
    val loadedModel: String? = null,
) {
    val isReady: Boolean = state == ModelState.READY
}
