package de.smartfinance.app.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GeminiVaultState(
    val apiKey: String = "",
    val selectedModel: String = ApiKeyManager.MODEL_GEMINI_3_8_FLASH,
    val availableModels: List<String> = ApiKeyManager.DEFAULT_PRESET_MODELS,
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
        get() = when (selectedModel.trim()) {
            ApiKeyManager.MODEL_GEMINI_3_8_FLASH -> "Gemini 3.8 Flash"
            ApiKeyManager.MODEL_GEMINI_3_8_PRO -> "Gemini 3.8 Pro"
            ApiKeyManager.MODEL_GEMINI_FLASH_LATEST -> "Gemini Flash Latest"
            ApiKeyManager.MODEL_GEMINI_2_5_FLASH -> "Gemini 2.5 Flash"
            else -> selectedModel.trim().ifEmpty { ApiKeyManager.MODEL_GEMINI_3_8_FLASH }
        }
}

/**
 * Sicherer lokaler API-Key-Tresor (Bring-Your-Own-Key) für das Google Gemini Hybrid-Modul.
 * Standard-Modell ist fest auf "gemini-3.8-flash" gesetzt, erlaubt beliebige manuelle Modell-Namen
 * sowie automatisches Selbstheilungs-Switching (Auto-Switch), falls Google ein neueres Modell empfiehlt.
 */
class ApiKeyManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _vaultState = MutableStateFlow(loadState())
    val vaultState: StateFlow<GeminiVaultState> = _vaultState.asStateFlow()

    private fun loadState(): GeminiVaultState {
        val storedKey = prefs.getString(KEY_GEMINI_API_KEY, "")?.trim().orEmpty()
        val migratedTo38 = prefs.getBoolean(KEY_MIGRATED_DEFAULT_3_8, false)
        val rawStoredModel = prefs.getString(KEY_GEMINI_MODEL, MODEL_GEMINI_3_8_FLASH)
            ?.trim()
            ?.removePrefix("models/")
            .orEmpty()

        val effectiveModel = if (!migratedTo38 && (rawStoredModel.isEmpty() || rawStoredModel == MODEL_GEMINI_2_5_FLASH)) {
            prefs.edit()
                .putString(KEY_GEMINI_MODEL, MODEL_GEMINI_3_8_FLASH)
                .putBoolean(KEY_MIGRATED_DEFAULT_3_8, true)
                .apply()
            MODEL_GEMINI_3_8_FLASH
        } else {
            if (!migratedTo38) {
                prefs.edit().putBoolean(KEY_MIGRATED_DEFAULT_3_8, true).apply()
            }
            rawStoredModel.ifEmpty { MODEL_GEMINI_3_8_FLASH }
        }

        return GeminiVaultState(
            apiKey = storedKey,
            selectedModel = effectiveModel,
            availableModels = DEFAULT_PRESET_MODELS
        )
    }

    fun saveApiKey(rawKey: String) {
        val clean = rawKey.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, clean).apply()
        _vaultState.value = _vaultState.value.copy(
            apiKey = clean,
            lastTestMessage = if (clean.isEmpty()) "API-Key entfernt – Lokal-Modus aktiv." else _vaultState.value.lastTestMessage,
            lastTestSuccess = if (clean.isEmpty()) null else _vaultState.value.lastTestSuccess
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
        val cleanModel = model.trim().removePrefix("models/").ifEmpty { MODEL_GEMINI_3_8_FLASH }
        prefs.edit()
            .putString(KEY_GEMINI_MODEL, cleanModel)
            .putBoolean(KEY_MIGRATED_DEFAULT_3_8, true)
            .apply()
        _vaultState.value = _vaultState.value.copy(
            selectedModel = cleanModel
        )
    }

    fun updateDiscoveredModels(models: List<String>, autoSelectedModel: String?) {
        val merged = (DEFAULT_PRESET_MODELS + models)
            .map { it.trim().removePrefix("models/") }
            .filter { it.isNotEmpty() }
            .distinct()
        val chosen = autoSelectedModel?.trim()?.removePrefix("models/")?.takeIf { it.isNotEmpty() }
            ?: _vaultState.value.selectedModel
        prefs.edit().putString(KEY_GEMINI_MODEL, chosen).apply()
        _vaultState.value = _vaultState.value.copy(
            selectedModel = chosen,
            availableModels = merged
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
        private const val KEY_MIGRATED_DEFAULT_3_8 = "migrated_default_gemini_3_8"

        const val MODEL_GEMINI_3_8_FLASH = "gemini-3.8-flash"
        const val MODEL_GEMINI_3_8_PRO = "gemini-3.8-pro"
        const val MODEL_GEMINI_FLASH_LATEST = "gemini-flash-latest"
        const val MODEL_GEMINI_2_5_FLASH = "gemini-2.5-flash"

        val DEFAULT_PRESET_MODELS = listOf(
            MODEL_GEMINI_3_8_FLASH,
            MODEL_GEMINI_3_8_PRO,
            MODEL_GEMINI_FLASH_LATEST,
            MODEL_GEMINI_2_5_FLASH
        )
    }
}
