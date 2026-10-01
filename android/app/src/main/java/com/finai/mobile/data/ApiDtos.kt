package com.finai.mobile.data

/**
 * Wire models mirroring the backend response bodies.
 *
 * Nullable fields are optional because the API is configured with
 * `default-property-inclusion: non_null`: a null value is omitted from the payload
 * rather than serialised, so Gson leaves these properties null.
 */

data class AuthResponse(
    val accessToken: String?,
    val tokenType: String?,
    val expiresIn: Long?,
    val user: UserSummary?,
)

data class UserSummary(
    val id: Long?,
    val email: String?,
    val username: String?,
    val fullName: String?,
    val phone: String?,
    val role: String?,
    val status: String?,
    val lastLoginAt: String?,
    val createdAt: String?,
)

data class LoginRequest(val identifier: String, val password: String)

data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val fullName: String? = null,
    val phone: String? = null,
)

data class PageResponse<T>(
    val content: List<T>?,
    val page: Int?,
    val size: Int?,
    val totalElements: Long?,
    val totalPages: Int?,
    val first: Boolean?,
    val last: Boolean?,
    val empty: Boolean?,
)

data class CategorySummary(
    val id: Long?,
    val name: String?,
    val code: String?,
    val icon: String?,
    val color: String?,
)

data class TransactionView(
    val id: Long?,
    val type: String?,
    val amount: Double?,
    val note: String?,
    val transactionDate: String?,
    val category: CategorySummary?,
    val createdAt: String?,
)

data class UpsertTransactionRequest(
    val categoryId: Long,
    val type: String,
    val amount: Double,
    val note: String? = null,
    val transactionDate: String,
)

data class CategoryView(
    val id: Long?,
    val name: String?,
    val code: String?,
    val type: String?,
    val icon: String?,
    val color: String?,
    val systemCategory: Boolean?,
    val active: Boolean?,
)

data class UpsertPersonalCategoryRequest(
    val name: String,
    val type: String,
    val icon: String? = null,
    val color: String? = null,
)

data class BudgetView(
    val id: Long?,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val amount: Double?,
    val periodType: String?,
    val periodStart: String?,
    val periodEnd: String?,
    val usedAmount: Double?,
    val remainingAmount: Double?,
    val usagePercentage: Double?,
    val status: String?,
)

data class UpsertBudgetRequest(
    val categoryId: Long,
    val amount: Double,
    val periodType: String = "MONTHLY",
    val periodStart: String? = null,
)

data class DashboardSummary(
    val periodStart: String?,
    val periodEnd: String?,
    val monthIncome: Double?,
    val monthExpense: Double?,
    val monthBalance: Double?,
    val allTimeIncome: Double?,
    val allTimeExpense: Double?,
    val allTimeBalance: Double?,
    val totalTransactionCount: Long?,
    val budgetSummary: BudgetSummary?,
    val topSpendingCategories: List<CategoryBreakdown>?,
    val recentTransactions: List<TransactionView>?,
    val unreadNotificationCount: Long?,
)

data class BudgetSummary(
    val totalBudgets: Int?,
    val warningBudgets: Int?,
    val exceededBudgets: Int?,
    val totalBudgetAmount: Double?,
    val totalUsedAmount: Double?,
)

data class StatisticsOverview(
    val from: String?,
    val to: String?,
    val totalIncome: Double?,
    val totalExpense: Double?,
    val balance: Double?,
    val incomeCount: Long?,
    val expenseCount: Long?,
)

data class CategoryBreakdown(
    val categoryId: Long?,
    val categoryName: String?,
    val categoryCode: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val total: Double?,
    val count: Long?,
    val percentage: Double?,
)

data class MonthlyPoint(
    val year: Int?,
    val month: Int?,
    val label: String?,
    val income: Double?,
    val expense: Double?,
    val balance: Double?,
)

data class DailyPoint(
    val date: String?,
    val income: Double?,
    val expense: Double?,
    val balance: Double?,
)

data class NotificationView(
    val id: Long?,
    val type: String?,
    val level: String?,
    val title: String?,
    val message: String?,
    val referenceType: String?,
    val referenceId: Long?,
    val read: Boolean?,
    val createdAt: String?,
)

data class ChatRequest(val conversationId: Long? = null, val message: String)

data class ChatResponse(
    val conversationId: Long?,
    val question: String?,
    val answer: String?,
    val facts: List<String>?,
    val grounded: Boolean?,
    val engine: String?,
    val providerAvailable: Boolean?,
    val intent: String?,
    val latencyMs: Long?,
    val createdAt: String?,
)

data class ConversationView(
    val id: Long?,
    val title: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class MessageView(
    val id: Long?,
    val role: String?,
    val content: String?,
    val hasGroundingFacts: Boolean?,
    val createdAt: String?,
)

data class FeedbackRequest(
    val title: String,
    val content: String,
    val category: String = "OTHER",
)

data class FeedbackView(
    val id: Long?,
    val title: String?,
    val content: String?,
    val category: String?,
    val status: String?,
    val adminReply: String?,
    val resolvedAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class ProfileView(
    val id: Long?,
    val email: String?,
    val username: String?,
    val fullName: String?,
    val phone: String?,
    val status: String?,
    val lastLoginAt: String?,
    val createdAt: String?,
)

data class UpdateProfileRequest(val fullName: String?, val phone: String?)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

data class UnreadCount(val count: Long?)

/** The error envelope returned by GlobalExceptionHandler. */
data class ApiErrorBody(
    val timestamp: String?,
    val status: Int?,
    val code: String?,
    val message: String?,
    val path: String?,
    val violations: List<Violation>?,
) {
    data class Violation(val field: String?, val message: String?)
}