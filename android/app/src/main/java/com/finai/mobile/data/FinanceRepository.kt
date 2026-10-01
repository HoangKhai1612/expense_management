package com.finai.mobile.data

import com.google.gson.Gson
import retrofit2.Response

/**
 * Result of a single API call.
 *
 * The distinction matters to the UI: [Result.Loaded] means the server answered,
 * [Result.Failed] means it answered with an error the user can act on, and
 * [Result.Offline] means the request never arrived, which is a different message.
 */
sealed interface Result<out T> {
    data class Loaded<T>(val value: T) : Result<T>
    data class Failed(val code: String, val message: String, val status: Int) : Result<Nothing>
    data class Offline(val message: String) : Result<Nothing>
}

/**
 * Runs a Retrofit call and normalises everything that can go wrong.
 *
 * The backend always answers errors with the same envelope, so the machine code
 * (for example ACCOUNT_LOCKED or CATEGORY_TYPE_MISMATCH) is preserved for the UI
 * to branch on instead of matching on human-readable text.
 */
class FinanceRepository(private val api: FinanceApi) {

    private val gson = Gson()

    // ------------------------------------------------------------------ calls
    //
    // One named method per endpoint keeps the Retrofit interface out of the
    // view models and gives every screen a single place to add error handling.

    suspend fun login(request: LoginRequest) = call { api.login(request) }

    suspend fun register(request: RegisterRequest) = call { api.register(request) }

    suspend fun profile() = call { api.profile() }

    suspend fun updateProfile(request: UpdateProfileRequest) = call { api.updateProfile(request) }

    suspend fun changePassword(request: ChangePasswordRequest) = call { api.changePassword(request) }

    suspend fun dashboard() = call { api.dashboard() }

    suspend fun transactions(
        type: String? = null,
        page: Int = 0,
        size: Int = 20,
    ) = call { api.transactions(type = type, page = page, size = size) }

    suspend fun createTransaction(request: UpsertTransactionRequest) =
        call { api.createTransaction(request) }

    suspend fun updateTransaction(id: Long, request: UpsertTransactionRequest) =
        call { api.updateTransaction(id, request) }

    suspend fun deleteTransaction(id: Long) = call { api.deleteTransaction(id) }

    suspend fun categories(type: String? = null) = call { api.categories(type) }

    suspend fun budgets(includeInactive: Boolean = false) =
        call { api.budgets(includeInactive) }

    suspend fun createBudget(request: UpsertBudgetRequest) = call { api.createBudget(request) }

    suspend fun updateBudget(id: Long, request: UpsertBudgetRequest) =
        call { api.updateBudget(id, request) }

    suspend fun deleteBudget(id: Long) = call { api.deleteBudget(id) }

    suspend fun unreadNotifications() = call { api.unreadNotifications() }

    suspend fun markAllNotificationsRead() = call { api.markAllNotificationsRead() }

    suspend fun chat(request: ChatRequest) = call { api.chat(request) }

    suspend fun conversationMessages(id: Long) = call { api.conversationMessages(id) }

    suspend fun <T> call(block: suspend () -> Response<T>): Result<T> = try {
        val response = block()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            val body = response.body()
            if (body == null) {
                // A 204 with no payload is a legitimate success for delete/void routes.
                Result.Loaded(Unit as T)
            } else {
                Result.Loaded(body)
            }
        } else {
            parseError(response)
        }
    } catch (t: Throwable) {
        Result.Offline(t.message ?: "Could not reach the server.")
    }

    private fun <T> parseError(response: Response<T>): Result<T> {
        val raw = try {
            response.errorBody()?.string()
        } catch (t: Throwable) {
            null
        }
        val parsed = raw?.takeIf { it.isNotBlank() }?.let {
            try {
                gson.fromJson(it, ApiErrorBody::class.java)
            } catch (t: Throwable) {
                null
            }
        }
        return if (parsed != null) {
            Result.Failed(
                code = parsed.code ?: "UNEXPECTED_ERROR",
                message = parsed.message ?: "The request could not be completed.",
                status = response.code(),
            )
        } else {
            Result.Failed(
                code = "HTTP_${response.code()}",
                message = "The server returned ${response.code()}.",
                status = response.code(),
            )
        }
    }
}