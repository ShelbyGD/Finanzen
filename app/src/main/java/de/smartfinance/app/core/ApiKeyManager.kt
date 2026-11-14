package de.smartfinance.app.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GeminiVaultState(
    val apiKey: String = "",
    val selectedModel: String = ApiKeyManager.MODEL_GEMINI_FLASH,
    val lastTestMessage: String? = null,
    val lastTestSuccess: Boolean? = null
) {
    val hasApiKey: Boolean
        get() = apiKey.trim().isNotEmpty()

    val maskedApiKey: String
        get() {
            val clean = apiKey.trim()
            if (clean.isEmpty()) return "Kein API-Key hinterlegt"
            if (clean.length <= 8) return "••••••••"
            return "${clean.take(4)}••••••••••••${clean.takeLast(4)}"
        }

    val modelDisplayName: String
        get() = when (selectedModel) {
            ApiKeyManager.MODEL_GEMINI_PRO -> "Gemini 2.5 Pro"
            else -> "Gemini 2.5 Flash"
        }
}

/**
 * Sicherer lokaler API-Key-Tresor (Bring-Your-Own-Key) für das Google Gemini Hybrid-Modul.
 * Speichert den vom Nutzer eingegebenen Google AI Studio API-Key ausschließlich lokal
 * in SharedPreferences auf dem Gerät (niemals hartcodiert im Quellcode).
 */
class ApiKeyManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _vaultState = MutableStateFlow(loadState())
    val vaultState: StateFlow<GeminiVaultState> = _vaultState.asStateFlow()

    private fun loadState(): GeminiVaultState {
        val storedKey = prefs.getString(KEY_GEMINI_API_KEY, "")?.trim().orEmpty()
        val storedModel = prefs.getString(KEY_GEMINI_MODEL, MODEL_GEMINI_FLASH) ?: MODEL_GEMINI_FLASH
        return GeminiVaultState(
            apiKey = storedKey,
            selectedModel = if (storedModel == MODEL_GEMINI_PRO) MODEL_GEMINI_PRO else MODEL_GEMINI_FLASH
        )
    }

    fun saveApiKey(rawKey: String) {
        val clean = rawKey.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, clean).apply()
        _vaultState.value = _vaultState.value.copy(
            apiKey = clean,
            lastTestMessage = if (clean.isEmpty()) "API-Key entfernt – Lokal-Modus aktiv." else null,
            lastTestSuccess = null
        )
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
        _vaultState.value = _vaultState.value.copy(
            apiKey = "",
            lastTestMessage = "API-Key gelöscht. App arbeitet im 100 % lokalen Modus.",
            lastTestSuccess = null
        )
    }

    fun setSelectedModel(model: String) {
        val validModel = if (model == MODEL_GEMINI_PRO) MODEL_GEMINI_PRO else MODEL_GEMINI_FLASH
        prefs.edit().putString(KEY_GEMINI_MODEL, validModel).apply()
        _vaultState.value = _vaultState.value.copy(
            selectedModel = validModel,
            lastTestMessage = null,
            lastTestSuccess = null
        )
    }

    fun updateTestStatus(success: Boolean?, message: String?) {
        _vaultState.value = _vaultState.value.copy(
            lastTestSuccess = success,
            lastTestMessage = message
        )
    }

    companion object {
        private const val PREFS_NAME = "smartfinance_gemini_vault_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_selected_model"

        const val MODEL_GEMINI_FLASH = "gemini-2.5-flash"
        const val MODEL_GEMINI_PRO = "gemini-2.5-pro"
    }
}
