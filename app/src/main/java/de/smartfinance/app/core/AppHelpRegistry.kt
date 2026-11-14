package de.smartfinance.app.core

import de.smartfinance.app.BuildConfig

/**
 * Zentrale Hilfe-, Feature- & Changelog-Datenbank ("Single Source of Truth") für SmartFinance 2026.
 *
 * WICHTIGE PFLICHT-REGEL FÜR ZUKÜNFTIGE UPDATES:
 * "Jedes Mal, wenn in einem zukünftigen Update eine neue Funktion hinzugefügt oder geändert wird,
 * MUSS zwingend gleichzeitig ein neuer `HelpEntry` sowie ein neuer `ChangelogEntry` in `AppHelpRegistry.kt` ergänzt werden!"
 */
data class HelpEntry(
    val id: String,
    val moduleNumber: Int,
    val title: String,
    val category: String,
    val shortSummary: String,
    val stepByStepGuide: List<String>,
    val exampleTips: List<String>,
    val requiredTier: AppTier,
    val addedInVersion: String
)

data class ChangelogEntry(
    val versionName: String,
    val buildNumber: Int,
    val date: String,
    val newFeatures: List<String>
)

/**
 * Single Source of Truth für alle 13 Kernmodule, kontextsensitive Hilfe-Karten,
 * den Lite-vs.-Pro-Vergleich und das automatische In-App-Update-Changelog.
 *
 * PFLICHT-REGEL:
 * "Jedes Mal, wenn in einem zukünftigen Update eine neue Funktion hinzugefügt oder geändert wird,
 * MUSS zwingend gleichzeitig ein neuer `HelpEntry` sowie ein neuer `ChangelogEntry` in `AppHelpRegistry.kt` ergänzt werden!"
 */
object AppHelpRegistry {

    const val ID_TIMELINE_HEADER = "timeline_header"
    const val ID_DAILY_BUDGET_ROLLOVER = "daily_budget_rollover"
    const val ID_DEBIT_GUARDIAN_WAVE = "debit_guardian_wave"
    const val ID_DYNAMIC_TABS_CONTRACTS = "dynamic_tabs_contracts"
    const val ID_CREEPING_COST_RADAR = "creeping_cost_radar"
    const val ID_ANNUAL_COST_SMOOTHER = "annual_cost_smoother"
    const val ID_SAVINGS_GOAL_TRACKER = "savings_goal_tracker"
    const val ID_HYBRID_AI_CHAT = "hybrid_ai_chat"
    const val ID_WHAT_IF_WORK_HOURS = "what_if_work_hours"
    const val ID_ONE_TAP_RECURRING = "one_tap_recurring"
    const val ID_OFFLINE_OCR_SCANNER = "offline_ocr_scanner"
    const val ID_DATA_VAULT_BACKUP = "data_vault_backup"
    const val ID_DEMO_SHORTCUTS_SIMULATOR = "demo_shortcuts_simulator"

    val helpEntries: List<HelpEntry> = listOf(
        HelpEntry(
            id = ID_TIMELINE_HEADER,
            moduleNumber = 1,
            title = "1. Monats-Zeitstrahl & Violette Hero-Budget-Karte",
            category = "Übersicht & Analysen",
            shortSummary = "Wischbarer Monats-Zeitstrahl in der Top-Bar, violette Hero-Karte mit verfügbarem Monatsbudget und türkisfarbenem Fortschrittsbalken sowie detaillierter Soll/Ist- & 3/6/12M-Durchschnitt im Analysen-Tab.",
            stepByStepGuide = listOf(
                "Wische in der Übersicht horizontal nach links (vergangene Monate) oder rechts (aktueller Monat & Prognose-Monate) oder nutze oben unter 'Übersicht' den Monats-Umschalter '< November 2026 >'.",
                "Die violette Hero-Karte zeigt sofort das noch verfügbare Budget, die verbleibenden Tage bis Monatsende und deinen aktuellen Tagessatz ('Heute frei: XX,XX € / Tag').",
                "Tippe auf die violette Hero-Karte oder unten in der 5er-Navigationsleiste auf 'Analysen', um Soll/Ist-Vergleiche und den historischen 3-, 6- oder 12-Monats-Durchschnitt einzusehen."
            ),
            exampleTips = listOf(
                "Tipp: Kommt dein Gehalt am 28. des Vormonats, stelle den Abrechnungstag unter 'Einstellungen' auf den 28. ein.",
                "Über den Link 'Heute' neben dem Monatsnamen springst du aus jedem Monat sofort zurück in den aktuellen Monat."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_DAILY_BUDGET_ROLLOVER,
            moduleNumber = 2,
            title = "2. Dynamisches Tagesbudget & Rollover-Formel",
            category = "Budgetlogik",
            shortSummary = "Berechnet jeden Tag neu, wie viel Geld du heute bedenkenlos ausgeben kannst – inklusive automatischem Rollover nicht verbrauchter Beträge auf den Restmonat.",
            stepByStepGuide = listOf(
                "Vom monatlichen Gesamteinkommen werden zuerst alle Fixkosten (inkl. noch kommender Abbuchungen), deine Dauer-Sparrate sowie 1/12-Jahresrücklagen abgezogen.",
                "Das verbleibende freie Monatsbudget wird nach Abzug deiner bisherigen variablen Ausgaben durch die verbleibenden Kalendertage des Monats (inkl. heute) geteilt.",
                "Gibst du heute weniger aus als dein Tagesbudget, steigt dein Tagessatz für alle restlichen Tage des Monats automatisch an (positiver Rollover – im Tab 'Analysen' aufgeschlüsselt)."
            ),
            exampleTips = listOf(
                "Formel: Freies Tagesbudget = (Einnahmen - Fixkosten gesamt - 1/12-Rücklagen - Sparrate - Variable Ausgaben bisher) / Resttage im Monat.",
                "Beispiel: Bei 600 € freiem Monatsbudget und 20 Resttagen hast du 30,00 €/Tag. Gibst du heute 0 € aus, stehen dir ab morgen 600 € / 19 Tage = 31,58 €/Tag zur Verfügung."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_DEBIT_GUARDIAN_WAVE,
            moduleNumber = 3,
            title = "3. Abbuchungs-Wächter & Grafische Monats-Belastungswelle",
            category = "Cashflow & Analysen",
            shortSummary = "Gruppierte Karte 'Nächste Abbuchungen (Wächter)' auf der Startseite plus große interaktive 31-Tage-Belastungswelle im Tab 'Analysen'.",
            stepByStepGuide = listOf(
                "Auf der Startseite ('Übersicht') zeigt die Sektion 'Nächste Abbuchungen (Wächter)' die nächsten anstehenden Fixkosten-Lastschriften mit Countdown und Betrag.",
                "Tippe auf 'Belastungswelle öffnen >' oder wechsle unten in den Tab 'Analysen', um das große Canvas-Wellendiagramm (Tag 1 bis 31) zu öffnen.",
                "Im Diagramm trennt die vertikale 'Heute'-Linie bereits abgebuchte Posten (links) von noch offenen Abbuchungsspitzen (rechts)."
            ),
            exampleTips = listOf(
                "Beispiel: 'Handyvertrag – In 1 Tag (15.11.) – 29,99 € | Noch offen: 119,89 €'.",
                "Tippe im Analysen-Tab direkt auf einen Balken im Diagramm, um alle Buchungen dieses Kalendertages zu inspizieren."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_DYNAMIC_TABS_CONTRACTS,
            moduleNumber = 4,
            title = "4. Runde Kreis-Budgets & Vertrags-Wecker",
            category = "Budgets & Verträge",
            shortSummary = "Runde Fortschrittsringe (Circular Progress Rings) für alle Kategorien in der Budgets-Karte sowie eigene Vertrags-Verwaltung mit 30-Tage-Vorwarnung im Tab 'Verträge'.",
            stepByStepGuide = listOf(
                "In der Karte 'Budgets' siehst du für jede Kategorie (Lebensmittel, Kfz, Handy, Versicherung, Freizeit) einen runden Fortschrittsring: Grün (<75 %), Orange (>75 %) oder Rot (Limit überschritten).",
                "Tippe auf einen Budget-Kreis zum Filtern der Buchungen oder ganz rechts auf den runden '+'-Kreis-Button, um eine neue Kategorie anzulegen.",
                "Im unteren Tab 'Verträge' verwaltest du alle Fixkosten, Fälligkeitstage (1–31) und Kündigungs-Stichtage (inkl. 30-Tage-Vorwarnung über die Benachrichtigungs-Glocke oben rechts)."
            ),
            exampleTips = listOf(
                "Ein Tap auf 'Alle anzeigen >' in der Budgets-Karte hebt aktive Kategorien-Filter sofort wieder auf.",
                "Alle aktiven Vertrags-Wecker werden gebündelt über das Glocken-Symbol oben rechts in der Top-Bar angezeigt."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_CREEPING_COST_RADAR,
            moduleNumber = 5,
            title = "5. Schleichende-Kosten-Radar & Benachrichtigungs-Glocke",
            category = "KI-Anomalie-Erkennung",
            shortSummary = "Automatische Erkennung bei >20 % Abweichung vom 3-Monats-Schnitt oder überschrittenem Budgetlimit – dezent gebündelt im Benachrichtigungs-Center der Top-Bar.",
            stepByStepGuide = listOf(
                "Die lokale Finanz-Engine vergleicht laufend die aktuellen Monatsausgaben jeder Kategorie mit dem Durchschnitt der letzten 3 Monate.",
                "Liegt eine Kategorie mehr als 20 % über dem 3-Monats-Schnitt oder über ihrem Limit, zeigt die Benachrichtigungs-Glocke oben rechts in der Top-Bar einen Badge-Zähler an.",
                "Tippe auf die Glocke, um alle Warnungen im Benachrichtigungs-Sheet zu prüfen und direkt in die betroffene Kategorie zu springen."
            ),
            exampleTips = listOf(
                "Beispiel: 'Schleichende Kosten erkannt: Lebensmittel liegt aktuell +28 % über deinem 3-Monats-Schnitt.'",
                "Durch die Bündelung in der Top-Bar-Glocke bleibt deine Startseite frei von störenden Warn-Bannern."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_ANNUAL_COST_SMOOTHER,
            moduleNumber = 6,
            title = "6. 1/12-Jahreskosten-Glätter",
            category = "Verträge & Analysen",
            shortSummary = "Legt jährliche oder vierteljährliche Großrechnungen (z. B. Kfz-Versicherung) virtuell auf 12 Monate um – einsehbar in den Tabs 'Verträge' und 'Analysen'.",
            stepByStepGuide = listOf(
                "Aktiviere beim Anlegen einer Buchung oder eines Vertrags den Schalter 'Auf 12 Monate glätten (1/12-Rücklage)'.",
                "SmartFinance zieht jeden Monat automatisch exakt 1/12 (bei Jahreszahlung) bzw. 1/3 (bei Quartalszahlung) als virtuelle Rücklage von deinem freien Budget ab.",
                "In den Tabs 'Verträge' und 'Analysen' siehst du jederzeit transparent die monatlich reservierte 1/12-Rücklage."
            ),
            exampleTips = listOf(
                "Beispiel: Deine Kfz-Versicherung kostet 660,00 € im Jahr. Mit aktivem 1/12-Glätter werden jeden Monat sanfte 55,00 € vom freien Budget reserviert."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_SAVINGS_GOAL_TRACKER,
            moduleNumber = 7,
            title = "7. Sparbooster: Etappen-Sparziel-Tracker & Dauer-Sparraten",
            category = "Sparen & Vermögensaufbau",
            shortSummary = "Eigener 'Sparbooster'-Tab in der unteren Navigationsleiste für dein individuelles Etappen-Sparziel (Start bei 0,00 €) inklusive KI-Berechnung des Zieldatums.",
            stepByStepGuide = listOf(
                "Wechsle über die untere 5er-Navigationsleiste in den Tab 'Sparbooster'.",
                "Tippe auf 'Sparziel anpassen', um deinen eigenen Zielbetrag, deinen aktuellen Startbestand und deine monatliche Dauer-Sparrate einzutragen.",
                "Nutze die 1-Tap-Sparbooster-Buttons (+25 €, +50 €, +100 €), um spontan zusätzliches Geld auf dein Etappen-Ziel einzuzahlen."
            ),
            exampleTips = listOf(
                "Alle Sparziel-Werte starten standardmäßig sauber bei 0,00 € und werden sofort nach deiner Eingabe berechnet."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_HYBRID_AI_CHAT,
            moduleNumber = 8,
            title = "8. KI-Coach: Lokale Smart Finance Hybrid-KI & Q&A-Chat",
            category = "Lokale KI & Prognose",
            shortSummary = "Eigener 'KI-Coach'-Tab mit 100 % lokalem deutschen Finanz-Assistenten, Werktags-/Wochenend-Burn-Rate und Echtzeit-Q&A-Chat.",
            stepByStepGuide = listOf(
                "Tippe unten in der Navigationsleiste auf den Tab 'KI-Coach'.",
                "Tippe auf einen der Schnell-Frage-Chips oder gib deine Frage auf Deutsch ein.",
                "Die Engine analysiert in Millisekunden deine lokale Room-Datenbank ohne Internetverbindung und liefert präzise Antworten in klarem Text."
            ),
            exampleTips = listOf(
                "Unterstützte deutsche Beispiel-Fragen im KI-Coach:",
                "- 'Wie viel habe ich heute/diesen Monat noch frei?'",
                "- 'Wann geht die nächste Zahlung ab?' / 'Was wird diese Woche noch abgebucht?'",
                "- 'Wann geht im Monat am meisten ab?'",
                "- 'Was gebe ich im Schnitt für Versicherungen / Handyvertrag aus?'",
                "- 'Reicht mein Geld bis Monatsende?' / 'Wann erreiche ich mein Sparziel?'",
                "- 'Was kannst du alles?' oder 'Hilfe'"
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_WHAT_IF_WORK_HOURS,
            moduleNumber = 9,
            title = "9. \"Was-wäre-wenn\"-Simulator & Arbeitsstunden-Umrechner",
            category = "Lokale KI & Kaufcheck",
            shortSummary = "Gib im KI-Coach einfach 'Check: 85 €' ein: Die KI prüft dein Restbudget + offene Fixkosten mit Grün/Gelb/Rot-Ampel und rechnet den Preis in deine Arbeitszeit um.",
            stepByStepGuide = listOf(
                "Öffne den Tab 'KI-Coach' und tippe z. B. 'Check: 85 €' oder 'Kann ich mir 120 € leisten?' ein.",
                "Der Simulator prüft, ob nach Abzug dieses Spontankaufs UND aller noch offenen Fixkosten-Abbuchungen des Monats genug Tagesbudget übrig bleibt.",
                "Trage unter 'Einstellungen' deinen eigenen Netto-Stundenlohn ein, damit der Simulator jeden Kaufbetrag exakt in deine persönliche Arbeitszeit umrechnet."
            ),
            exampleTips = listOf(
                "Der Kauf-Simulator berücksichtigt automatisch alle noch offenen Fixkosten-Verträge bis zum Monatsende."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_ONE_TAP_RECURRING,
            moduleNumber = 10,
            title = "10. FloatingActionButton (+), Direkteingabe & Daueraufträge",
            category = "Alltag & Automatisierung",
            shortSummary = "Runder '+'-FloatingActionButton rechts unten öffnet das Einhand-BottomSheet mit dem großen Taschenrechner-Numpad. Zudem sind Grundgehalt, Budgets, Verträge und Sparziel direkt antippen- und editierbar.",
            stepByStepGuide = listOf(
                "Tippe in der Übersicht rechts unten auf den runden '+'-Button (FloatingActionButton) oder auf einen leeren Listen-Platzhalter.",
                "Über den Taschenrechner-Zahlenblock gibst du eigene Einnahmen, Ausgaben oder Sparraten (inkl. + und - Rechnung) ein.",
                "Tippe in der violetten Hero-Karte direkt auf 'Einnahmen: 0,00 €', um dein monatliches Grundgehalt einzutragen, oder im Tab 'Verträge' auf eine Kategorie, um eigene Budgets und Fixkosten festzulegen."
            ),
            exampleTips = listOf(
                "Wiederkehrende Fixkosten, Gehalt und Dauer-Sparraten werden im laufenden Monat bis zum heutigen Kalendertag automatisch am jeweiligen Fälligkeitstag (1–31) gebucht."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_OFFLINE_OCR_SCANNER,
            moduleNumber = 11,
            title = "11. Offline Beleg-Scanner (Google ML Kit OCR)",
            category = "Kamera & Belegerkennung",
            shortSummary = "Direkt über das Kamera-Icon oben rechts in der Top-Bar erreichbar: Scanne echte Kassenbons offline per Kamera/Galerie mit automatischer Betrags- und Händlererkennung.",
            stepByStepGuide = listOf(
                "Tippe oben rechts in der Top-Bar auf das Kamera-Icon oder nutze den Homescreen-Shortcut.",
                "Wähle ein Foto deines Kassenbons aus der Galerie aus.",
                "Die On-Device-Texterkennung (Google ML Kit) extrahiert per Regex den höchsten plausiblen Euro-Gesamtbetrag, das Belegdatum und ordnet Händler automatisch der passenden Kategorie zu."
            ),
            exampleTips = listOf(
                "100 % Datenschutz: Das Foto verlässt niemals dein Smartphone, die OCR-Erkennung läuft komplett lokal auf dem Gerät."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_DATA_VAULT_BACKUP,
            moduleNumber = 12,
            title = "12. Daten-Tresor (1-Tap JSON-Backup/Restore & Bank-CSV-Import)",
            category = "Datensicherung & Import",
            shortSummary = "Volle Datenhoheit ohne Cloud-Zwang: 1-Tap Komplett-Backup & Wiederherstellung als JSON-Datei sowie intelligenter Bank-CSV-Kontoauszug-Import.",
            stepByStepGuide = listOf(
                "Öffne über das linke Hamburger-Menü den Bereich 'Daten-Tresor (Backup & Import)'.",
                "Mit 'Backup speichern' sicherst du alle Buchungen, Kategorien, Verträge, Sparziele und Einstellungen als JSON-Datei.",
                "Über 'Bank-CSV-Datei auswählen & importieren' liest du echte Kontoauszüge deiner Hausbank inkl. automatischer Kategorisierung ein."
            ),
            exampleTips = listOf(
                "Vor einem Smartphone-Wechsel kannst du deine gesamte Datenbank mit einem Tap als JSON-Datei exportieren und auf dem neuen Gerät wieder einspielen."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_DEMO_SHORTCUTS_SIMULATOR,
            moduleNumber = 13,
            title = "13. 100 % Null-Start (0,00 €), Direkteingabe & In-Place-Updates",
            category = "System, Datenschutz & Komfort",
            shortSummary = "Garantierter 0,00-€-Start ohne Demo-Daten, deaktiviertes Android Cloud-AutoBackup, direktes Eintragen aller eigenen Werte per Tap sowie Base64-Keystore für nahtlose In-Place-Updates.",
            stepByStepGuide = listOf(
                "Nach der Installation stehen alle Einnahmen, Ausgaben, Tagesbudgets, Verträge und Sparziele exakt bei 0,00 €.",
                "Tippe in der violetten Hero-Karte auf 'Einnahmen: 0,00 €' (oder öffne 'Einstellungen'), um dein eigenes monatliches Grundgehalt einzutragen.",
                "Tippe im Tab 'Verträge' auf eine beliebige Kategorie, um dein persönliches Monatsbudget oder einen Fixkosten-Vertrag festzulegen, oder nutze den '+'-Button für Einzelbuchungen."
            ),
            exampleTips = listOf(
                "Wechsle über das Sonnen-/Mond-Symbol oben rechts in der Top-Bar jederzeit live zwischen dem hellen Clean-Banking-Look und dem Dark Mode.",
                "Unter 'Lite vs. Pro Übersicht' im Hamburger-Menü kannst du jederzeit live den Lite-Modus simulieren."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = BuildConfig.VERSION_NAME
        )
    )

    val changelogEntries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            versionName = BuildConfig.VERSION_NAME,
            buildNumber = BuildConfig.BUILD_NUMBER,
            date = "14.11.2026",
            newFeatures = listOf(
                "100 % echter Null-Start (0,00 €): Sämtliche Demo-Daten, Beispiel-Fallbacks und der Demo-Generator wurden restlos aus dem Code entfernt.",
                "Frische Datenbank ('smartfinance_clean_user.db') und deaktiviertes Android AutoBackup (allowBackup=false), damit keine gecachten Alt-Daten wiederhergestellt werden.",
                "Direkteingabe überall per Tap: Grundgehalt direkt auf der violetten Hero-Karte ('Einnahmen: 0,00 €') oder in den Einstellungen eintragen; Kategorien, Budgets, Verträge und Sparziel direkt per Tap bearbeiten.",
                "Klickbare Leer-Platzhalter ('Noch keine Einträge vorhanden – tippe hier oder auf +, um deine eigenen Werte einzutragen') in 'Nächste Abbuchungen' und 'Letzte Buchungen'.",
                "Garantiertes In-Place-Update ('Drüber-Installieren'): Base64-Keystore (app/keystore/keystore.b64) mit automatischer Gradle-Dekodierung für Debug & Release sowie streng monoton steigender Zeitstempel-versionCode."
            )
        ),
        ChangelogEntry(
            versionName = "1.0.4",
            buildNumber = 4,
            date = "14.11.2026",
            newFeatures = listOf(
                "UI/UX-Komplett-Redesign: Moderner 5-Tab Clean-Banking-Look ('Übersicht', 'Verträge', 'Analysen', 'KI-Coach', 'Sparbooster') mit Material-Vektor-Icons.",
                "Violette Hero-Budget-Karte mit PRO-Statistik-Badge, Resttagen, großem verfügbaren Budget, türkisfarbenem Fortschrittsbalken und Tagessatz.",
                "Gruppierte Listen-Container ('Nächste Abbuchungen' & 'Letzte Buchungen') mit 0.5dp Trennlinien und Detail-/Löschen-BottomSheet."
            )
        ),
        ChangelogEntry(
            versionName = "1.0.2",
            buildNumber = 2,
            date = "14.11.2026",
            newFeatures = listOf(
                "UI/UX-Update (Build #2): Aufgeräumtes Cockpit mit Progressive Disclosure und aufklappbaren Detailbereichen."
            )
        ),
        ChangelogEntry(
            versionName = "1.0.1",
            buildNumber = 1,
            date = "14.11.2026",
            newFeatures = listOf(
                "Initiales Voll-Release von SmartFinance 2026 (v1.0.1, Build #1) mit Room-Datenschutz, lokaler Hybrid-KI und ML Kit OCR."
            )
        )
    )

    fun findById(id: String): HelpEntry? = helpEntries.find { it.id == id }

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
