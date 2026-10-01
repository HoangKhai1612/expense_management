package com.finai.mobile.ui

import com.finai.mobile.data.Result

/**
 * A screen's async state.
 *
 * Modelling "loading" explicitly, rather than as null data, is what lets the UI
 * distinguish a first load from an empty result and show a spinner rather than a
 * misleading "nothing here" message.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val value: T) : UiState<T>
    data class Error(val message: String, val code: String = "") : UiState<Nothing>
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Loaded -> Result.Loaded(transform(value))
    is Result.Failed -> this
    is Result.Offline -> this
}

fun <T> Result<T>.toUiState(): UiState<T> = when (this) {
    is Result.Loaded -> UiState.Ready(value)
    is Result.Failed -> UiState.Error(message, code)
    is Result.Offline -> UiState.Error(message)
}