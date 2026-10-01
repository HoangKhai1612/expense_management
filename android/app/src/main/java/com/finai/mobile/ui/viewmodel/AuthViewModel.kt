package com.finai.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finai.mobile.data.FinanceRepository
import com.finai.mobile.data.LoginRequest
import com.finai.mobile.data.RegisterRequest
import com.finai.mobile.data.Result
import com.finai.mobile.data.SessionStore
import com.finai.mobile.data.UserSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val checkingSession: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val user: UserSummary? = null,
)

/**
 * Owns the session.
 *
 * On start the stored token is validated against a real endpoint rather than
 * trusted: a token can be expired or the account can have been locked since it was
 * issued, and discovering that on the first screen would look like a random failure.
 */
class AuthViewModel(
    private val repository: FinanceRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val token = session.currentToken()
            if (token.isNullOrBlank()) {
                _state.value = AuthUiState(checkingSession = false)
                return@launch
            }
            val cached = session.cachedProfile()
            when (val result = repository.profile()) {
                is Result.Loaded -> _state.value = AuthUiState(
                    checkingSession = false,
                    user = UserSummary(
                        id = result.value.id,
                        email = result.value.email,
                        username = result.value.username,
                        fullName = result.value.fullName,
                        phone = result.value.phone,
                        role = cached?.role,
                        status = result.value.status,
                        lastLoginAt = result.value.lastLoginAt,
                        createdAt = result.value.createdAt,
                    ),
                )
                is Result.Failed -> {
                    // The token no longer works, so the local copy must go too.
                    session.clear()
                    _state.value = AuthUiState(checkingSession = false, error = null)
                }
                is Result.Offline -> _state.value = AuthUiState(
                    checkingSession = false,
                    user = cached,
                    error = "You appear to be offline. Some data may be out of date.",
                )
            }
        }
    }

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Enter your email or username and password.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, error = null)
            when (val result = repository.login(LoginRequest(identifier.trim(), password))) {
                is Result.Loaded -> {
                    session.save(result.value)
                    _state.value = AuthUiState(checkingSession = false, user = result.value.user)
                }
                is Result.Failed -> _state.value = _state.value.copy(
                    submitting = false,
                    error = when (result.code) {
                        "ACCOUNT_LOCKED" -> "This account is locked. Please contact support."
                        "ACCOUNT_DEACTIVATED" -> "This account has been deactivated."
                        "INVALID_CREDENTIALS" -> "Email/username or password is incorrect."
                        else -> result.message
                    },
                )
                is Result.Offline -> _state.value = _state.value.copy(
                    submitting = false,
                    error = "Could not reach the server.",
                )
            }
        }
    }

    fun register(email: String, username: String, password: String, fullName: String?) {
        if (email.isBlank() || username.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "All three fields are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, error = null)
            val request = RegisterRequest(
                email = email.trim(),
                username = username.trim(),
                password = password,
                fullName = fullName?.trim()?.takeIf { it.isNotEmpty() },
            )
            when (val result = repository.register(request)) {
                is Result.Loaded -> {
                    session.save(result.value)
                    _state.value = AuthUiState(checkingSession = false, user = result.value.user)
                }
                is Result.Failed -> _state.value = _state.value.copy(
                    submitting = false,
                    error = when (result.code) {
                        "EMAIL_ALREADY_EXISTS" -> "An account already exists for that email."
                        "USERNAME_ALREADY_EXISTS" -> "That username is already taken."
                        "VALIDATION_ERROR" -> result.message
                        else -> result.message
                    },
                )
                is Result.Offline -> _state.value = _state.value.copy(
                    submitting = false,
                    error = "Could not reach the server.",
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            session.clear()
            _state.value = AuthUiState(checkingSession = false)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    companion object {
        fun factory(repository: FinanceRepository, session: SessionStore) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(repository, session) as T
            }
    }
}