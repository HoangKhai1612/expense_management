package com.finai.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finai_session")

/**
 * Persists the bearer token and the cached profile.
 *
 * DataStore rather than SharedPreferences because the token has to be readable on a
 * background thread while the OkHttp interceptor runs, and DataStore exposes it as
 * a Flow instead of forcing a blocking read on the network thread.
 */
class SessionStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("access_token")
    private val emailKey = stringPreferencesKey("email")
    private val usernameKey = stringPreferencesKey("username")
    private val fullNameKey = stringPreferencesKey("full_name")
    private val roleKey = stringPreferencesKey("role")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }

    suspend fun currentToken(): String? = context.dataStore.data.first()[tokenKey]

    suspend fun save(auth: AuthResponse) {
        val user = auth.user
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = auth.accessToken.orEmpty()
            prefs[emailKey] = user?.email.orEmpty()
            prefs[usernameKey] = user?.username.orEmpty()
            prefs[fullNameKey] = user?.fullName.orEmpty()
            prefs[roleKey] = user?.role.orEmpty()
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(tokenKey)
            prefs.remove(emailKey)
            prefs.remove(usernameKey)
            prefs.remove(fullNameKey)
            prefs.remove(roleKey)
        }
    }

    suspend fun cachedProfile(): UserSummary? {
        val prefs = context.dataStore.data.first()
        val token = prefs[tokenKey]
        if (token.isNullOrBlank()) return null
        return UserSummary(
            id = null,
            email = prefs[emailKey],
            username = prefs[usernameKey],
            fullName = prefs[fullNameKey],
            phone = null,
            role = prefs[roleKey],
            status = null,
            lastLoginAt = null,
            createdAt = null,
        )
    }
}