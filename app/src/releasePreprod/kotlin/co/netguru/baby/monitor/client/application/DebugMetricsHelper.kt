package co.netguru.baby.monitor.client.application

import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Helper class that initializes a set of debugging tools
 * for the debug build type and register crash manager for release type.
 * ## Debug type tools:
 * - AndroidDevMetrics
 * - Stetho
 * - StrictMode
 * - LeakCanary
 * - Timber
 *
 * ## Release type tools:
 * - CrashManager
 */
@Singleton
class DebugMetricsHelper @Inject constructor() {

    internal fun init() {
        Timber.plant(Timber.DebugTree())
    }
}
