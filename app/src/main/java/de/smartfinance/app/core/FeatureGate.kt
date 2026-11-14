package de.smartfinance.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lizenz-Stufen für SmartFinance 2026 (Lite-Basisversion vs. Pro-Vollversion).
 */
enum class AppTier(val displayName: String, val badgeLabel: String) {
    LITE_FREE(
        displayName = "SmartFinance Lite (Basisversion)",
        badgeLabel = "🔓 LITE-Testmodus aktiv"
    ),
    PRO_FULL(
        displayName = "SmartFinance PRO (Vollversion)",
        badgeLabel = "💎 PRO-Vollversion aktiv"
    )
}

/**
 * Zentrale Lizenz- & Feature-Schnittstelle für SmartFinance 2026.
 *
 * Standardwert ist fest auf [AppTier.PRO_FULL] gesetzt (Entwickler-Vollversion ab Tag 1 aktiv).
 * Über den Live-Testschalter im Screen "Lite vs. Pro Übersicht" kann jederzeit zwischen
 * [AppTier.PRO_FULL] und [AppTier.LITE_FREE] umgeschaltet werden, um das Verhalten der
 * 🔒 PRO-Badges und Freischalt-Dialoge live zu testen.
 */
object FeatureGate {

    // === GEMINI ARCHITECTURE NOTE: LITE VS PRO BILLING HOOK ===
    // Späterer Einstiegspunkt für die Google Play Billing Library (com.android.billingclient:billing-ktx):
    // 1. Beim App-Start in SmartFinanceApp.onCreate() BillingClient initialisieren und queryPurchasesAsync()
    //    für INAPP-Produkt "smartfinance_pro_lifetime" bzw. SUBS "smartfinance_pro_yearly" aufrufen.
    // 2. Falls ein verifizierter Kauf (PurchaseState.PURCHASED) vorliegt, setTier(AppTier.PRO_FULL) setzen,
    //    andernfalls im Play-Store-Release auf AppTier.LITE_FREE zurückfallen.
    // 3. Bis zur Play-Store-Monetarisierung bleibt der Standardwert fest auf AppTier.PRO_FULL!
    private val _currentTier = MutableStateFlow(AppTier.PRO_FULL)
    val currentTier: StateFlow<AppTier> = _currentTier.asStateFlow()

    /**
     * Prüft, ob ein Feature mit der angeforderten Lizenzstufe aktuell freigeschaltet ist.
     */
    fun isUnlocked(requiredTier: AppTier, activeTier: AppTier = _currentTier.value): Boolean {
        return when (requiredTier) {
            AppTier.LITE_FREE -> true
            AppTier.PRO_FULL -> activeTier == AppTier.PRO_FULL
        }
    }

    /**
     * Setzt die aktive Lizenzstufe (für Entwickler-Simulator oder zukünftigen Billing-Callback).
     */
    fun setTier(tier: AppTier) {
        _currentTier.value = tier
    }

    /**
     * Schaltet live zwischen PRO_FULL und LITE_FREE um (Entwickler-Testschalter).
     */
    fun toggleSimulatedTier() {
        _currentTier.value = if (_currentTier.value == AppTier.PRO_FULL) {
            AppTier.LITE_FREE
        } else {
            AppTier.PRO_FULL
        }
    }
}
