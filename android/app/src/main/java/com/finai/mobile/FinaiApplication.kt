package com.finai.mobile

import android.app.Application
import com.finai.mobile.data.ApiClient
import com.finai.mobile.data.FinanceRepository
import com.finai.mobile.data.SessionStore

/**
 * Manual dependency container.
 *
 * The graph is three objects deep, so a DI framework would add more ceremony than
 * it removes. Everything here is process-scoped and stateless apart from the
 * session, which is exactly what the screens need.
 */
class FinaiApplication : Application() {

    val session: SessionStore by lazy { SessionStore(this) }
    val repository: FinanceRepository by lazy {
        FinanceRepository(ApiClient.api(this, session))
    }
}