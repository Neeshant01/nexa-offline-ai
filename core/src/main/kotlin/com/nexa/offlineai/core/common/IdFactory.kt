package com.nexa.offlineai.core.common

import java.util.UUID

object IdFactory {
    fun newId(): String = UUID.randomUUID().toString()
}
