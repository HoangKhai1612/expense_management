package com.finai.mobile.data

import android.content.Context
import com.finai.mobile.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the Retrofit client.
 *
 * The base URL comes from `BuildConfig` so a debug build can talk to a developer
 * machine and a release build can point at a real deployment without touching code.
 */
object ApiClient {

    private const val TIMEOUT_SECONDS = 30L

    @Volatile
    private var instance: FinanceApi? = null

    fun api(context: Context, session: SessionStore): FinanceApi =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext, session).also { instance = it }
        }

    private fun build(context: Context, session: SessionStore): FinanceApi {
        val gson: Gson = GsonBuilder().create()

        val authInterceptor = Interceptor { chain ->
            // runBlocking is acceptable here: OkHttp invokes interceptors on its own
            // dispatcher thread, and DataStore's read is a fast local lookup. It is
            // never called on the main thread.
            val token = runBlocking { session.currentToken() }
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            // Bodies contain financial figures, so header logging alone would be
            // safer; BODY is enabled only in debug builds.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(FinanceApi::class.java)
    }
}