package com.finai.mobile.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The mobile client's view of the backend.
 *
 * Every call returns `Response<T>` so the repository can distinguish a transport
 * failure from an application error and surface the backend's machine code rather
 * than a generic message.
 */
interface FinanceApi {

    // ------------------------------------------------------------------ auth

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // -------------------------------------------------------------- dashboard

    @GET("api/dashboard")
    suspend fun dashboard(): Response<DashboardSummary>

    // ----------------------------------------------------------- transactions

    @GET("api/transactions")
    suspend fun transactions(
        @Query("type") type: String? = null,
        @Query("categoryId") categoryId: Long? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): Response<PageResponse<TransactionView>>

    @GET("api/transactions/{id}")
    suspend fun transaction(@Path("id") id: Long): Response<TransactionView>

    @POST("api/transactions")
    suspend fun createTransaction(
        @Body request: UpsertTransactionRequest,
    ): Response<TransactionView>

    @PUT("api/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body request: UpsertTransactionRequest,
    ): Response<TransactionView>

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long): Response<Unit>

    // ------------------------------------------------------------- categories

    @GET("api/categories")
    suspend fun categories(@Query("type") type: String? = null): Response<List<CategoryView>>

    @POST("api/categories")
    suspend fun createCategory(
        @Body request: UpsertPersonalCategoryRequest,
    ): Response<CategoryView>

    @PUT("api/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Long,
        @Body request: UpsertPersonalCategoryRequest,
    ): Response<CategoryView>

    @DELETE("api/categories/{id}")
    suspend fun deactivateCategory(@Path("id") id: Long): Response<Unit>

    // ---------------------------------------------------------------- budgets

    @GET("api/budgets")
    suspend fun budgets(
        @Query("includeInactive") includeInactive: Boolean = false,
    ): Response<List<BudgetView>>

    @GET("api/budgets/{id}")
    suspend fun budget(@Path("id") id: Long): Response<BudgetView>

    @POST("api/budgets")
    suspend fun createBudget(@Body request: UpsertBudgetRequest): Response<BudgetView>

    @PUT("api/budgets/{id}")
    suspend fun updateBudget(
        @Path("id") id: Long,
        @Body request: UpsertBudgetRequest,
    ): Response<BudgetView>

    @DELETE("api/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Long): Response<Unit>

    // ------------------------------------------------------------- statistics

    @GET("api/statistics/overview")
    suspend fun overview(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Response<StatisticsOverview>

    @GET("api/statistics/by-category")
    suspend fun byCategory(
        @Query("type") type: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Response<List<CategoryBreakdown>>

    @GET("api/statistics/monthly")
    suspend fun monthly(@Query("year") year: Int? = null): Response<List<MonthlyPoint>>

    @GET("api/statistics/daily")
    suspend fun daily(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Response<List<DailyPoint>>

    // ---------------------------------------------------------- notifications

    @GET("api/notifications")
    suspend fun notifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): Response<PageResponse<NotificationView>>

    @GET("api/notifications/unread")
    suspend fun unreadNotifications(): Response<List<NotificationView>>

    @GET("api/notifications/unread-count")
    suspend fun unreadCount(): Response<Map<String, Long>>

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<NotificationView>

    @PATCH("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>

    // --------------------------------------------------------------------- ai

    @POST("api/ai/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>

    @GET("api/ai/conversations")
    suspend fun conversations(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): Response<PageResponse<ConversationView>>

    @GET("api/ai/conversations/{id}/messages")
    suspend fun conversationMessages(
        @Path("id") id: Long,
    ): Response<List<MessageView>>

    // ----------------------------------------------------------------- profile

    @GET("api/users/me")
    suspend fun profile(): Response<ProfileView>

    @PATCH("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileView>

    @POST("api/users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    // ---------------------------------------------------------------- feedback

    @POST("api/feedback")
    suspend fun submitFeedback(@Body request: FeedbackRequest): Response<FeedbackView>

    @GET("api/feedback")
    suspend fun myFeedback(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): Response<PageResponse<FeedbackView>>

    @GET("api/feedback/{id}")
    suspend fun feedback(@Path("id") id: Long): Response<FeedbackView>
}