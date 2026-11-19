package de.smartfinance.app.core

import android.content.Context
import android.content.SharedPreferences
import de.smartfinance.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettingsState(
    val billingStartDay: Int = 1,
    val hourlyWage: Double = 0.0,
    val averageMonthsWindow: Int = 3,
    val lastSeenVersionCode: Int = 0,
    val showUpdateBanner: Boolean = false,
    val isDarkMode: Boolean = false,
    val showBookingCategoryIcons: Boolean = true,
    val isAppLockEnabled: Boolean = false,
    val appLockTimeoutSeconds: Int = 30
)

/**
 * Verwaltet persistente Nutzereinstellungen, den Light/Dark-Theme-Umschalter
 * sowie die Versionserkennung für den Update-Hinweis in der Benachrichtigungs-Glocke.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smartfinance_clean_user_prefs", Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadState())
    val settingsState: StateFlow<UserSettingsState> = _settingsState.asStateFlow()

    private fun loadState(): UserSettingsState {
        val billingDay = 1 // fest: voller Kalendermonat
        val wage = prefs.getFloat(KEY_HOURLY_WAGE, 0.0f).toDouble().coerceAtLeast(0.0)
        val avgWindow = prefs.getInt(KEY_AVG_WINDOW, 3)
        val lastSeenVersion = prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, -1)
        val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
        val showIcons = prefs.getBoolean(KEY_SHOW_BOOKING_CATEGORY_ICONS, true)
        val appLockEnabled = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        val rawTimeout = prefs.getInt(KEY_APP_LOCK_TIMEOUT_SECONDS, 30)
        val appLockTimeout = if (rawTimeout in VALID_APP_LOCK_TIMEOUTS) rawTimeout else 30
        val currentVersion = BuildConfig.VERSION_CODE

        val shouldShowUpdateBanner = lastSeenVersion < currentVersion

        return UserSettingsState(
            billingStartDay = billingDay,
            hourlyWage = wage,
            averageMonthsWindow = avgWindow,
            lastSeenVersionCode = if (lastSeenVersion == -1) 0 else lastSeenVersion,
            showUpdateBanner = shouldShowUpdateBanner,
            isDarkMode = isDark,
            showBookingCategoryIcons = showIcons,
            isAppLockEnabled = appLockEnabled,
            appLockTimeoutSeconds = appLockTimeout
        )
    }

    fun setBillingStartDay(day: Int) {
        val validDay = 1 // Abrechnungs-Starttag entfernt: immer voller Kalendermonat
        prefs.edit().putInt(KEY_BILLING_START_DAY, validDay).apply()
        _settingsState.value = _settingsState.value.copy(billingStartDay = validDay)
    }

    fun setHourlyWage(wage: Double) {
        val validWage = wage.coerceIn(0.0, 1000.0)
        prefs.edit().putFloat(KEY_HOURLY_WAGE, validWage.toFloat()).apply()
        _settingsState.value = _settingsState.value.copy(hourlyWage = validWage)
    }

    fun setAverageMonthsWindow(months: Int) {
        val validMonths = if (months in listOf(3, 6, 12)) months else 3
        prefs.edit().putInt(KEY_AVG_WINDOW, validMonths).apply()
        _settingsState.value = _settingsState.value.copy(averageMonthsWindow = validMonths)
    }

    fun toggleDarkMode() {
        val next = !_settingsState.value.isDarkMode
        prefs.edit().putBoolean(KEY_DARK_MODE, next).apply()
        _settingsState.value = _settingsState.value.copy(isDarkMode = next)
    }

    fun setShowBookingCategoryIcons(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BOOKING_CATEGORY_ICONS, show).apply()
        _settingsState.value = _settingsState.value.copy(showBookingCategoryIcons = show)
    }

    fun toggleShowBookingCategoryIcons() {
        val next = !_settingsState.value.showBookingCategoryIcons
        setShowBookingCategoryIcons(next)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
        _settingsState.value = _settingsState.value.copy(isAppLockEnabled = enabled)
    }

    fun setAppLockTimeoutSeconds(seconds: Int) {
        val validSeconds = if (seconds in VALID_APP_LOCK_TIMEOUTS) seconds else 30
        prefs.edit().putInt(KEY_APP_LOCK_TIMEOUT_SECONDS, validSeconds).apply()
        _settingsState.value = _settingsState.value.copy(appLockTimeoutSeconds = validSeconds)
    }

    fun dismissUpdateBanner() {
        val currentVersion = BuildConfig.VERSION_CODE
        prefs.edit().putInt(KEY_LAST_SEEN_VERSION_CODE, currentVersion).apply()
        _settingsState.value = _settingsState.value.copy(
            lastSeenVersionCode = currentVersion,
            showUpdateBanner = false
        )
    }

    fun exportToMap(): Map<String, Any> = mapOf(
        KEY_BILLING_START_DAY to _settingsState.value.billingStartDay,
        KEY_HOURLY_WAGE to _settingsState.value.hourlyWage,
        KEY_AVG_WINDOW to _settingsState.value.averageMonthsWindow,
        KEY_SHOW_BOOKING_CATEGORY_ICONS to _settingsState.value.showBookingCategoryIcons
    )

    fun importFromMap(billingStartDay: Int, hourlyWage: Double, averageMonthsWindow: Int, showBookingCategoryIcons: Boolean = true) {
        setBillingStartDay(billingStartDay)
        setHourlyWage(hourlyWage)
        setAverageMonthsWindow(averageMonthsWindow)
        setShowBookingCategoryIcons(showBookingCategoryIcons)
    }

    companion object {
        private const val KEY_BILLING_START_DAY = "billing_start_day"
        private const val KEY_HOURLY_WAGE = "hourly_wage"
        private const val KEY_AVG_WINDOW = "avg_months_window"
        private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
        private const val KEY_SHOW_BOOKING_CATEGORY_ICONS = "show_booking_category_icons"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_APP_LOCK_TIMEOUT_SECONDS = "app_lock_timeout_seconds"
        val VALID_APP_LOCK_TIMEOUTS = listOf(0, 30, 60)
    }
}
