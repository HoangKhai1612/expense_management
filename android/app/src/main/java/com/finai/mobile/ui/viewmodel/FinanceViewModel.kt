package com.finai.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finai.mobile.data.BudgetView
import com.finai.mobile.data.CategoryView
import com.finai.mobile.data.ChangePasswordRequest
import com.finai.mobile.data.ChatRequest
import com.finai.mobile.data.ChatResponse
import com.finai.mobile.data.DashboardSummary
import com.finai.mobile.data.FinanceRepository
import com.finai.mobile.data.MessageView
import com.finai.mobile.data.NotificationView
import com.finai.mobile.data.Result
import com.finai.mobile.data.TransactionView
import com.finai.mobile.data.UpdateProfileRequest
import com.finai.mobile.data.UpsertBudgetRequest
import com.finai.mobile.data.UpsertTransactionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class FinanceUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val dashboard: DashboardSummary? = null,
    val transactions: List<TransactionView> = emptyList(),
    val transactionPage: Int = 0,
    val transactionPages: Int = 1,
    val budgets: List<BudgetView> = emptyList(),
    val categories: List<CategoryView> = emptyList(),
    val notifications: List<NotificationView> = emptyList(),
    val messages: List<MessageView> = emptyList(),
    val conversationId: Long? = null,
    val aiThinking: Boolean = false,
    val typeFilter: String? = null,
    val error: String? = null,
)

/**
 * Backs every signed-in screen.
 *
 * One view model covers the whole app because the screens share the same
 * categories, budgets and dashboard payload: splitting it would mean re-fetching
 * the same rows on every tab change.
 */
class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _state = MutableStateFlow(FinanceUiState())
    val state: StateFlow<FinanceUiState> = _state.asStateFlow()

    fun loadInitial() {
        if (_state.value.dashboard != null) return
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(refreshing = true, loading = _state.value.dashboard == null)
        viewModelScope.launch {
            val dashboard = repository.dashboard()
            val transactions = repository.transactions(
                type = _state.value.typeFilter,
                page = 0,
                size = 20,
            )
            val budgets = repository.budgets()
            val categories = repository.categories()
            val notifications = repository.unreadNotifications()

            var conversationId = _state.value.conversationId
            var messages = _state.value.messages
            if (conversationId != null) {
                val loaded = repository.conversationMessages(conversationId)
                if (loaded is Result.Loaded) messages = loaded.value
            }

            val failure = listOf(dashboard, transactions, budgets, categories, notifications)
                .filterIsInstance<Result.Failed>()
                .firstOrNull()

            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                dashboard = (dashboard as? Result.Loaded)?.value ?: _state.value.dashboard,
                transactions = (transactions as? Result.Loaded)?.value?.content ?: emptyList(),
                transactionPage = (transactions as? Result.Loaded)?.value?.page ?: 0,
                transactionPages = (transactions as? Result.Loaded)?.value?.totalPages ?: 1,
                budgets = (budgets as? Result.Loaded)?.value ?: emptyList(),
                categories = (categories as? Result.Loaded)?.value ?: emptyList(),
                notifications = (notifications as? Result.Loaded)?.value ?: emptyList(),
                conversationId = conversationId,
                messages = messages,
                error = failure?.message,
            )
        }
    }

    fun setTypeFilter(type: String?) {
        _state.value = _state.value.copy(typeFilter = type)
        loadTransactions(0)
    }

    fun loadTransactions(page: Int) {
        viewModelScope.launch {
            when (val result = repository.transactions(
                type = _state.value.typeFilter,
                page = page,
                size = 20,
            )) {
                is Result.Loaded -> _state.value = _state.value.copy(
                    transactions = result.value.content ?: emptyList(),
                    transactionPage = result.value.page ?: page,
                    transactionPages = result.value.totalPages ?: 1,
                    error = null,
                )
                is Result.Failed -> _state.value = _state.value.copy(error = result.message)
                is Result.Offline -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun saveTransaction(
        existingId: Long?,
        categoryId: Long,
        type: String,
        amount: Double,
        note: String?,
        date: String,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val request = UpsertTransactionRequest(
                categoryId = categoryId,
                type = type,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                transactionDate = date,
            )
            val result = if (existingId == null) {
                repository.createTransaction(request)
            } else {
                repository.updateTransaction(existingId, request)
            }
            when (result) {
                is Result.Loaded -> {
                    _state.value = _state.value.copy(error = null)
                    refresh()
                    onDone(null)
                }
                is Result.Failed -> onDone(
                    when (result.code) {
                        "AMOUNT_MUST_BE_POSITIVE" -> "Amount must be greater than zero."
                        "AMOUNT_TOO_PRECISE" -> "Amount supports at most 2 decimal places."
                        "DATE_IN_FUTURE" -> "The date cannot be in the future."
                        "CATEGORY_TYPE_MISMATCH" -> "That category is for the other kind of entry."
                        "TRANSACTION_NOT_FOUND" -> "That transaction no longer exists."
                        else -> result.message
                    },
                )
                is Result.Offline -> onDone(result.message)
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            when (val result = repository.deleteTransaction(id)) {
                is Result.Loaded -> {
                    _state.value = _state.value.copy(error = null)
                    refresh()
                }
                is Result.Failed -> _state.value = _state.value.copy(error = result.message)
                is Result.Offline -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun saveBudget(
        existingId: Long?,
        categoryId: Long,
        amount: Double,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val request = UpsertBudgetRequest(
                categoryId = categoryId,
                amount = amount,
                periodType = "MONTHLY",
                periodStart = YearMonth.now().atDay(1).toString(),
            )
            val result = if (existingId == null) {
                repository.createBudget(request)
            } else {
                repository.updateBudget(existingId, request)
            }
            when (result) {
                is Result.Loaded -> {
                    _state.value = _state.value.copy(error = null)
                    refresh()
                    onDone(null)
                }
                is Result.Failed -> onDone(
                    when (result.code) {
                        "BUDGET_ALREADY_EXISTS" -> "A budget already covers that category this month."
                        "CATEGORY_TYPE_MISMATCH" -> "A budget can only be set on an expense category."
                        "AMOUNT_MUST_BE_POSITIVE" -> "Amount must be greater than zero."
                        else -> result.message
                    },
                )
                is Result.Offline -> onDone(result.message)
            }
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            when (val result = repository.deleteBudget(id)) {
                is Result.Loaded -> refresh()
                is Result.Failed -> _state.value = _state.value.copy(error = result.message)
                is Result.Offline -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun askAi(question: String, onDone: (ChatResponse?) -> Unit) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(aiThinking = true, error = null)
            val request = ChatRequest(conversationId = _state.value.conversationId, message = question)
            when (val result = repository.chat(request)) {
                is Result.Loaded -> {
                    val response = result.value
                    _state.value = _state.value.copy(
                        aiThinking = false,
                        conversationId = response.conversationId,
                    )
                    val transcript = response.conversationId?.let { id ->
                        repository.conversationMessages(id)
                    }
                    if (transcript is Result.Loaded) {
                        _state.value = _state.value.copy(messages = transcript.value)
                    }
                    onDone(response)
                }
                is Result.Failed -> {
                    _state.value = _state.value.copy(aiThinking = false, error = result.message)
                    onDone(null)
                }
                is Result.Offline -> {
                    _state.value = _state.value.copy(aiThinking = false, error = result.message)
                    onDone(null)
                }
            }
        }
    }

    fun startNewConversation() {
        _state.value = _state.value.copy(conversationId = null, messages = emptyList())
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            if (repository.markAllNotificationsRead() is Result.Loaded) {
                refresh()
            }
        }
    }

    /**
     * Saves the profile. Returns null on success, or a message the caller should
     * show; keeping the outcome in the callback avoids a second state channel for
     * a one-off form save.
     */
    fun saveProfile(fullName: String, phone: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val request = UpdateProfileRequest(
                fullName = fullName.trim().takeIf { it.isNotEmpty() },
                phone = phone.trim().takeIf { it.isNotEmpty() },
            )
            when (val result = repository.updateProfile(request)) {
                is Result.Loaded -> onDone(null)
                is Result.Failed -> onDone(
                    when (result.code) {
                        "VALIDATION_ERROR" -> result.message
                        else -> result.message
                    },
                )
                is Result.Offline -> onDone(result.message)
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val request = ChangePasswordRequest(currentPassword, newPassword)
            when (val result = repository.changePassword(request)) {
                is Result.Loaded -> onDone(null)
                is Result.Failed -> onDone(
                    when (result.code) {
                        "INVALID_CURRENT_PASSWORD" -> "That is not your current password."
                        "VALIDATION_ERROR" ->
                            "The new password needs an uppercase letter, a lowercase letter and a digit."
                        else -> result.message
                    },
                )
                is Result.Offline -> onDone(result.message)
            }
        }
    }

    fun today(): String = LocalDate.now().toString()

    companion object {
        fun factory(repository: FinanceRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FinanceViewModel(repository) as T
        }
    }
}