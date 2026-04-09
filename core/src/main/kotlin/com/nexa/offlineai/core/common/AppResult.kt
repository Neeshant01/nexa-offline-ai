package com.nexa.offlineai.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    val message: String

    data class Validation(override val message: String) : AppError
    data class Database(override val message: String) : AppError
    data class Provider(override val message: String) : AppError
    data class Model(override val message: String) : AppError
    data class Permission(override val message: String) : AppError
    data class Voice(override val message: String) : AppError
    data class Network(override val message: String) : AppError
    data class Unknown(override val message: String) : AppError
}

inline fun <T> appResultOf(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (exception: IllegalArgumentException) {
    AppResult.Error(AppError.Validation(exception.message ?: "Validation failed"))
} catch (exception: Exception) {
    AppResult.Error(AppError.Unknown(exception.message ?: "Unexpected error"))
}
