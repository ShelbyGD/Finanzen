package de.smartfinance.app.core

import android.content.Context
import android.content.SharedPreferences
import de.smartfinance.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettingsState(
    val billingStartDay: Int = 1,
    val hourlyWage: Double = 15.75,
    val averageMonthsWindow: Int = 3,
    val lastSeenVersionCode: Int = 0,
    val showUpdateBanner: Boolean = false,
    val isDarkMode: Boolean = false,
    val isDemoDataActive: Boolean = false
)

/**
 * Verwaltet persistente Nutzereinstellungen, den Light/Dark-Theme-Umschalter,
 * den Status des Test-Labors (Echte Daten vs. Beispieldaten) sowie die Versionserkennung
 * für den Update-Hinweis in der Benachrichtigungs-Glocke.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smartfinance_prefs", Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadState())
    val settingsState: StateFlow<UserSettingsState> = _settingsState.asStateFlow()

    private fun loadState(): UserSettingsState {
        val billingDay = prefs.getInt(KEY_BILLING_START_DAY, 1).coerceIn(1, 28)
        val wage = prefs.getFloat(KEY_HOURLY_WAGE, 15.75f).toDouble()
        val avgWindow = prefs.getInt(KEY_AVG_WINDOW, 3)
        val lastSeenVersion = prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, -1)
        val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
        val isDemo = prefs.getBoolean(KEY_DEMO_DATA_ACTIVE, false)
        val currentVersion = BuildConfig.VERSION_CODE

        val shouldShowUpdateBanner = lastSeenVersion < currentVersion

        return UserSettingsState(
            billingStartDay = billingDay,
            hourlyWage = wage,
            averageMonthsWindow = avgWindow,
            lastSeenVersionCode = if (lastSeenVersion == -1) 0 else lastSeenVersion,
            showUpdateBanner = shouldShowUpdateBanner,
            isDarkMode = isDark,
            isDemoDataActive = isDemo
        )
    }

    fun setBillingStartDay(day: Int) {
        val validDay = day.coerceIn(1, 28)
        prefs.edit().putInt(KEY_BILLING_START_DAY, validDay).apply()
        _settingsState.value = _settingsState.value.copy(billingStartDay = validDay)
    }

    fun setHourlyWage(wage: Double) {
        val validWage = wage.coerceIn(1.0, 500.0)
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

    fun setDemoDataActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_DATA_ACTIVE, active).apply()
        _settingsState.value = _settingsState.value.copy(isDemoDataActive = active)
    }

    fun dismissUpdateBanner() {
        val currentVersion = BuildConfig.VERSION_CODE
        prefs.edit().putInt(KEY_LAST_SEEN_VERSION_CODE, currentVersion).apply()
        _settingsState.value = _settingsState.value.copy(
            lastSeenVersionCode = currentVersion,
            showUpdateBanner = false
        )
    }

    fun isLegacyAutoSeedCleaned(): Boolean = prefs.getBoolean(KEY_LEGACY_AUTOSEED_CLEANED, false)

    fun markLegacyAutoSeedCleaned() {
        prefs.edit().putBoolean(KEY_LEGACY_AUTOSEED_CLEANED, true).apply()
    }

    fun exportToMap(): Map<String, Any> = mapOf(
        KEY_BILLING_START_DAY to _settingsState.value.billingStartDay,
        KEY_HOURLY_WAGE to _settingsState.value.hourlyWage,
        KEY_AVG_WINDOW to _settingsState.value.averageMonthsWindow
    )

    fun importFromMap(billingStartDay: Int, hourlyWage: Double, averageMonthsWindow: Int) {
        setBillingStartDay(billingStartDay)
        setHourlyWage(hourlyWage)
        setAverageMonthsWindow(averageMonthsWindow)
    }

    companion object {
        private const val KEY_BILLING_START_DAY = "billing_start_day"
        private const val KEY_HOURLY_WAGE = "hourly_wage"
        private const val KEY_AVG_WINDOW = "avg_months_window"
        private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
        private const val KEY_DEMO_DATA_ACTIVE = "demo_data_active"
        private const val KEY_LEGACY_AUTOSEED_CLEANED = "legacy_autoseed_cleaned_v5"
    }
}
