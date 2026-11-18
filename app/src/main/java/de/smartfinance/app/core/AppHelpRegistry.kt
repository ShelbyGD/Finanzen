package de.smartfinance.app.core

import de.smartfinance.app.BuildConfig

/**
 * Zentrale Hilfe-, Feature- & Changelog-Datenbank ("Single Source of Truth") für AuraFinance AI.
 */
data class HelpEntry(
    val id: String,
    val moduleNumber: Int,
    val title: String,
    val category: String,
    val shortSummary: String,
    val stepByStepGuide: List<String>,
    val exampleTips: List<String>,
    val addedInVersion: String
)

data class ChangelogEntry(
    val versionName: String,
    val buildNumber: Int,
    val date: String,
    val newFeatures: List<String>
)

/**
 * Single Source of Truth für Hilfe-Karten und das Update-Changelog.
 */
object AppHelpRegistry {

    const val ID_BOOKINGS_SWIPE = "bookings_swipe"
    const val ID_LOCAL_AI_COACH = "local_ai_coach"
    const val ID_SAVINGS_BOOSTER = "savings_booster"

    // Abwärtskompatibilitäts-Aliase für bestehende Aufrufe
    const val ID_GEMINI_CLOUD_HYBRID = ID_LOCAL_AI_COACH
    const val ID_ARCHIVE_SWIPE = ID_BOOKINGS_SWIPE
    const val ID_SAVINGS_GOAL_TRACKER = ID_SAVINGS_BOOSTER
    const val ID_TIMELINE_HEADER = ID_BOOKINGS_SWIPE
    const val ID_DAILY_BUDGET_ROLLOVER = ID_BOOKINGS_SWIPE
    const val ID_DEBIT_GUARDIAN_WAVE = ID_BOOKINGS_SWIPE
    const val ID_DYNAMIC_TABS_CONTRACTS = ID_BOOKINGS_SWIPE
    const val ID_CREEPING_COST_RADAR = ID_BOOKINGS_SWIPE
    const val ID_ANNUAL_COST_SMOOTHER = ID_BOOKINGS_SWIPE
    const val ID_HYBRID_AI_CHAT = ID_LOCAL_AI_COACH
    const val ID_WHAT_IF_WORK_HOURS = ID_LOCAL_AI_COACH
    const val ID_ONE_TAP_RECURRING = ID_BOOKINGS_SWIPE
    const val ID_OFFLINE_OCR_SCANNER = ID_BOOKINGS_SWIPE
    const val ID_DATA_VAULT_BACKUP = ID_BOOKINGS_SWIPE
    const val ID_DEMO_SHORTCUTS_SIMULATOR = ID_BOOKINGS_SWIPE
    const val ID_TIME_RANGE_SWITCHER = ID_BOOKINGS_SWIPE
    const val ID_INSTALLMENTS_TRACKER = ID_BOOKINGS_SWIPE

    val helpEntries: List<HelpEntry> = listOf(
        HelpEntry(
            id = ID_BOOKINGS_SWIPE,
            moduleNumber = 1,
            title = "1. Buchungen verwalten (Swipe-Gesten)",
            category = "Buchungen & Übersicht",
            shortSummary = "Tippe auf eine Buchung, um das Icon oder den Betrag zu ändern. Wische eine Buchung nach links, um sie sofort zu löschen, oder nach rechts, um sie ins Archiv zu verschieben.",
            stepByStepGuide = listOf(
                "Tippe auf eine Buchung in der Liste 'Letzte Buchungen', um Betrag, Name, Kategorie oder Icon flexibel anzupassen.",
                "Wische eine Buchung nach links, um sie sofort zu löschen (mit 1-Tap Rückgängig-Möglichkeit).",
                "Wische eine Buchung nach rechts, um sie ins Zwischenarchiv auszulagern (beeinflusst das aktuelle Budget nicht mehr)."
            ),
            exampleTips = listOf(
                "Einnahmen stehen immer fest oben in leuchtendem Grün.",
                "Ausgaben werden mit rotem Betrag unmissverständlich dargestellt."
            ),
            addedInVersion = "1.2.0"
        ),
        HelpEntry(
            id = ID_LOCAL_AI_COACH,
            moduleNumber = 2,
            title = "2. Der lokale KI-Coach",
            category = "KI-Assistent",
            shortSummary = "Der KI-Coach arbeitet zu 100 % lokal mit deinen eingegebenen Zahlen. Frage ihn einfach: \"Wie viel Budget habe ich noch frei?\" oder \"Reicht mein Geld bis Monatsende?\".",
            stepByStepGuide = listOf(
                "Öffne den Tab 'KI-Coach' in der Navigationsleiste.",
                "Frage einfach: 'Wie viel Budget habe ich noch frei?' oder 'Reicht mein Geld bis Monatsende?'.",
                "Nutze 'Check: 85 €', um Beträge in deine persönliche Arbeitszeit umzurechnen.",
                "Über das Mikrofon-Symbol kannst du Buchungen direkt und schnell per Sprache erfassen."
            ),
            exampleTips = listOf(
                "100 % Datenschutz: Deine Finanzzahlen bleiben vollständig auf deinem Gerät.",
                "Optional kannst du im Tresor einen eigenen Google Gemini API-Key hinterlegen."
            ),
            addedInVersion = "1.2.0"
        ),
        HelpEntry(
            id = ID_SAVINGS_BOOSTER,
            moduleNumber = 3,
            title = "3. Flexible Sparziele (Sparbooster)",
            category = "Sparbooster",
            shortSummary = "Lege für jedes deiner Konten (z. B. Tagesgeld, Festgeld, Notgroschen) eine eigene Spar-Karte an und verbuche Einzahlungen mit nur einem Tap.",
            stepByStepGuide = listOf(
                "Tippe im Tab 'Sparbooster' auf '+ Neues Sparziel anlegen'.",
                "Gib Titel, Bank/Speicherort (z. B. Tagesgeld, Notgroschen), Zielsumme und monatliche Sparrate ein.",
                "Nutze die 1-Tap Schnell-Buttons (+25 €, +50 €, +100 €), um Sonderzahlungen direkt zu verbuchen.",
                "Über die Icons auf der Karte kannst du Sparziele jederzeit bearbeiten oder löschen."
            ),
            exampleTips = listOf(
                "Die Summen-Karte ganz oben zeigt dir jederzeit deine Gesamtersparnis und monatliche Sparrate über alle Ziele hinweg."
            ),
            addedInVersion = "1.2.0"
        )
    )

    val changelogEntries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            versionName = "1.2.0 (AuraFinance AI & Clean M3 Update)",
            buildNumber = 24,
            date = "18.11.2026",
            newFeatures = listOf(
                "Komplettes Redesign im edlen Google Material 3 Surface-Look (ohne lila Farbblöcke).",
                "Smarte Swipe-Gesten (Gmail-Style): Wische nach links zum Löschen, nach rechts fürs Archiv.",
                "Neuer Sparbooster: Lege beliebig viele individuelle Sparziele (inklusive Banknamen) an.",
                "100 % Fokus auf das Wesentliche: Direkte Einnahmen-Ausgaben-Rechnung ohne verwirrende Vertrags-Dopplungen."
            )
        )
    )

    fun findById(id: String): HelpEntry? {
        val clean = id.trim().lowercase()
        return helpEntries.find { it.id == clean }
            ?: when {
                clean.contains("saving") -> helpEntries.find { it.id == ID_SAVINGS_BOOSTER }
                clean.contains("ai") || clean.contains("coach") || clean.contains("gemini") -> helpEntries.find { it.id == ID_LOCAL_AI_COACH }
                else -> helpEntries.firstOrNull()
            }
    }

    fun search(query: String): List<HelpEntry> {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return helpEntries
        return helpEntries.filter { entry ->
            entry.title.lowercase().contains(clean) ||
                entry.category.lowercase().contains(clean) ||
                entry.shortSummary.lowercase().contains(clean) ||
                entry.stepByStepGuide.any { it.lowercase().contains(clean) } ||
                entry.exampleTips.any { it.lowercase().contains(clean) }
        }
    }
}
