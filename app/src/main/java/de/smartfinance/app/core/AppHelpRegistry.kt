package de.smartfinance.app.core

import de.smartfinance.app.BuildConfig

/**
 * Zentrale Hilfe-, Feature- & Changelog-Datenbank ("Single Source of Truth") für NovaFinance.
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
    const val ID_GEMINI_CLOUD_HYBRID = "gemini_cloud_hybrid"
    const val ID_TIME_RANGE_SWITCHER = "time_range_switcher"
    const val ID_INSTALLMENTS_TRACKER = "installments_tracker"

    val helpEntries: List<HelpEntry> = listOf(
        HelpEntry(
            id = ID_INSTALLMENTS_TRACKER,
            moduleNumber = 15,
            title = "15. Ratenzahlungen & Offene Posten (Finanzierungs-Tracker)",
            category = "Verbindlichkeiten & Tilgung",
            shortSummary = "Vollautomatischer Tracker für Ratenkäufe, Kredite, Finanzierungen und offene Posten mit flexibler Ordner-Verwaltung, Monats-Register zum Abhaken, Gesamtzeitraum-Anzeige und einklappbaren Summenkarten.",
            stepByStepGuide = listOf(
                "Öffne das Hamburger-Menü und tippe auf 'Ratenzahlungen & Offene Posten'.",
                "Über '+ Ordner' kannst du übersichtliche Themen-Ordner anlegen (z. B. 'Technik', 'Haus & Nebenkosten', 'Auto', 'Sonstiges').",
                "Über '+ Posten' trägst du neue Ratenkäufe oder Verbindlichkeiten ein und weist sie direkt einem Ordner zu.",
                "Tippe auf das Monats-Register eines Postens, um alle Einzelraten anzuzeigen und einzelne Monate bei Bedarf manuell abzuhaken.",
                "Der Ordner-Header zeigt dir sofort die Gesamtlaufzeit aller enthaltenen Posten (z. B. 'Laufzeit: Mai 2026 – Jun 2027 (14 Monate gesamt)').",
                "Über den dezenten Pfeil-Button kannst du die große Summenkarte jederzeit zu einer schlanken Statusleiste einklappen."
            ),
            exampleTips = listOf(
                "Ordner lassen sich per Fingertipp auf- und zuklappen und über das Stift- bzw. Mülleimer-Icon umbenennen oder löschen.",
                "Über 'Auf Stichtags-Automatik zurücksetzen' stellt man manuell abgehakte Raten mit einem Tap auf die automatische Kalender-Erkennung zurück.",
                "Rate 0 € = Offener Posten (Ratenstart noch offen). Mit \"Ratenzahlung planen / später starten\" setzt du Rate und zukünftiges Startdatum; vor dem Startmonat wird nichts vom Budget abgezogen. Maximal 120 Raten werden berechnet.",
                "Vereinfachung: Unten gibt es nur noch 3 Reiter (Übersicht, KI-Coach, Sparbooster). Verfügbar = Einnahmen - Ausgaben (alle Buchungen in 'Letzte Buchungen' plus aktive Raten). Im '+'-Menü legst du mit '+ Eigene Kategorie' eigene Kategorien an und löschst oder benennst sie über 'Verwalten' um.",
                "Berechnung: Verfügbar = Einnahmen - Fixkosten/Verträge - Ausgaben (Hero-Karte zeigt alle drei Zeilen). Verträge werden pro Monat nur 1x abgezogen, auch wenn eine passende Buchung existiert. Der Abrechnungs-Starttag ist entfernt; es zählt immer der volle Kalendermonat.",
                "Navigation verschlankt: 4 Reiter unten (Übersicht, Verträge, KI-Coach, Sparbooster); der tote Menüpunkt 'Kategorien, Sparziele & Verträge' und der Tab 'Analysen' sind entfernt. Jeder Vertrag hat ein Papierkorb-Icon mit Löschbestätigung.",
                "Auf der Startseite ('Übersicht') ist die Raten-Summenkarte ebenfalls einklappbar.",
                "Die Kopfzeile zeigt jederzeit deine offene Gesamtschuld, die laufende Monatsbelastung und den Tilgungsfortschritt in Prozent."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.3.5"
        ),
        HelpEntry(
            id = ID_TIME_RANGE_SWITCHER,
            moduleNumber = 14,
            title = "14. Aufgeräumtes Monats-Cockpit & Strikte Finanz-Mathematik",
            category = "Navigation & Übersicht",
            shortSummary = "Schlankes, klares Monats-Cockpit mit wischbarem Monats-Umschalter (< November 2026 >), großer violetter Hero-Karte und direkter Liste 'Letzte Buchungen' ohne ablenkende Zwischenkarten.",
            stepByStepGuide = listOf(
                "Navigiere mit den Pfeilen (< und >) oder per horizontalem Wischen durch die Monate.",
                "Auf der Startseite ('Übersicht') siehst du ganz oben die große violette Hero-Karte mit deinem verfügbaren Monatsbudget, Einnahmen und Tagessatz.",
                "Direkt darunter stehen ohne überflüssige Zwischenkarten alle deine letzten Buchungen im Detail.",
                "Über den Link '• Heute' springst du aus jedem beliebigen Monat sofort in den aktuellen Monat zurück.",
                "Im Reiter 'Verträge' sind feste Einnahmen (+) und Verträge/Fixkosten (-) sauber getrennt."
            ),
            exampleTips = listOf(
                "Formel: Verfügbares Budget = Gesamteinnahmen - Fixkosten/Verträge - Variable Ausgaben.",
                "Komma-Toleranz: Bei Beträgen kannst du sowohl '12,50' als auch '12.50' eingeben."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.3.1"
        ),
        HelpEntry(
            id = ID_TIMELINE_HEADER,
            moduleNumber = 1,
            title = "1. Monats-Zeitstrahl & Violette Hero-Budget-Karte",
            category = "Übersicht & Analysen",
            shortSummary = "Wischbarer Monats-Zeitstrahl in der Top-Bar, violette Hero-Karte mit verfügbarem Monatsbudget und türkisfarbenem Fortschrittsbalken sowie detaillierter Soll/Ist- & 3/6/12M-Durchschnitt im Analysen-Tab.",
            stepByStepGuide = listOf(
                "Wische in der Übersicht horizontal nach links (vergangene Monate) oder rechts (aktueller Monat & Prognose-Monate) oder nutze oben unter 'Übersicht' den Monats-Umschalter '< November 2026 >'.",
                "Die violette Hero-Karte zeigt sofort das noch verfügbare Budget, die verbleibenden Tage bis Monatsende und deinen aktuellen Tagessatz ('Heute frei: XX,XX € / Tag').",
                "Die untere Navigationsleiste hat 4 Reiter (Übersicht, Verträge, KI-Coach, Sparbooster); der Analysen-Tab wurde entfernt. Auf der Übersicht siehst du Soll/Ist-Vergleiche und den historischen 3-, 6- oder 12-Monats-Durchschnitt einzusehen."
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
                "Tippe auf 'Belastungswelle öffnen >', um das große Canvas-Wellendiagramm (Tag 1 bis 31) zu öffnen.",
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
            shortSummary = "Runde Fortschrittsringe (Circular Progress Rings) für variable Monatsbudgets im Cockpit sowie saubere Vertrags- & Fixkosten-Verwaltung im Tab 'Verträge'.",
            stepByStepGuide = listOf(
                "Im Cockpit ('Übersicht') verwaltest du über die Karte 'Budgets' deine variablen Alltagsbudgets (Lebensmittel, Freizeit etc.) mit runden Fortschrittsringen: Grün (<75 %), Orange (>75 %) oder Rot (Limit überschritten). Über '+ Neu' oder 'Alle anzeigen >' legst du Monatslimits fest.",
                "Im Tab 'Verträge' sind feste monatliche Einnahmen (Gehalt) und feste Ausgaben (Kfz-Versicherung, Handyvertrag, Miete) sauber getrennt. Verträge werden ausschließlich als echte Ausgaben-Abzüge mit Fälligkeitstag und 1/12-Glättung gelistet.",
                "Kündigungsfristen werden mit 30-Tage-Vorwarnung automatisch über die Benachrichtigungs-Glocke oben rechts in der Top-Bar signalisiert."
            ),
            exampleTips = listOf(
                "Tippe auf einen Budget-Ring zum Filtern der letzten Buchungen oder nutze 'Alle anzeigen >' zum Verwalten aller Limits.",
                "Variable Monatslimits sind keine Verträge, sondern Obergrenzen für die Kreis-Ringe. Das verfügbare Monatsbudget berechnet sich strikt: Einnahmen minus Fixkosten minus variable Ausgaben."
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
                "NovaFinance zieht jeden Monat automatisch exakt 1/12 (bei Jahreszahlung) bzw. 1/3 (bei Quartalszahlung) als virtuelle Rücklage von deinem freien Budget ab.",
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
            title = "7. Sparbooster: Mehrere flexible Sparziele, Bank-Optionen & Sofort-Einzahlungen",
            category = "Sparen & Vermögensaufbau",
            shortSummary = "Vollflexibler 'Sparbooster'-Tab mit Summen-Kopfzeile, beliebig vielen individuellen Sparzielen inkl. Bank/Speicherort, Bearbeiten, Löschen und zielgenauen 1-Tap Einzahlungen.",
            stepByStepGuide = listOf(
                "Wechsle über die untere Navigationsleiste in den Tab 'Sparbooster'.",
                "Tippe oben auf '+ Neues Sparziel', um ein individuelles Ziel mit Name, Bank/Speicherort (z. B. BW-Bank, WeltSparen), Zielbetrag, Startkapital, monatlicher Sparrate und Wunschfarbe anzulegen.",
                "Jede Spar-Karte kann direkt per Stift-Icon bearbeitet und per Papierkorb gelöscht werden.",
                "Nutze die 1-Tap Schnell-Einzahlungen (+25 €, +50 €, +100 € oder individueller Betrag) direkt auf der jeweiligen Karte, um genau diesem Ziel Geld gutzuschreiben.",
                "Die Summen-Kopfzeile ganz oben zeigt stets dein gesamtes Erspartes sowie deine monatliche Gesamt-Sparrate über alle Konten hinweg."
            ),
            exampleTips = listOf(
                "Dank automatischer Monatswachstums-Berechnung wächst dein Erspartes monatlich passend zum gewählten Spar-Tag ohne manuellen Aufwand.",
                "Alle Sparziele werden dauerhaft lokal in deiner sicheren Room-Datenbank gespeichert."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.2.2"
        ),
        HelpEntry(
            id = ID_HYBRID_AI_CHAT,
            moduleNumber = 8,
            title = "8. KI-Coach: Autonomer App-Agent (Room-Vollzugriff, PDF-Upload & Rückgängig-Netz)",
            category = "Autonomer KI-Agent & Prognose",
            shortSummary = "Vollwertiger autonomer KI-Agent mit Lese- und Schreibzugriff auf alle Room-Tabellen: Trägt Buchungen, Verträge, Budgets, Sparziele und Einstellungen selbstständig ein – inkl. PDF-/Bild-Upload und 1-Tap-Rückgängig-Kachel.",
            stepByStepGuide = listOf(
                "Tippe unten in der Navigationsleiste auf den Tab 'KI-Coach'.",
                "Sprich oder schreibe deine Wünsche (z. B. 'Trage 50 € für Tanken ein', 'Neuer Stromvertrag 65 € am 1. des Monats' oder 'Setze mein Sparziel auf 5.000 €').",
                "Tippe links neben der Eingabe auf das Büroklammer-Icon (📎), um Kassenbons, Rechnungen, Gehaltsabrechnungen oder Verträge als PDF oder Foto direkt im Chat hochzuladen. Die App konvertiert PDFs lokal mit PdfRenderer und lässt die KI alle Daten automatisch buchen.",
                "Sicherheits-Netz: Jede Aktion wird transparent mit einer grünen Kachel ('✅ Automatisch eingetragen: ...') bestätigt und kann mit einem Tap auf 'Rückgängig' sofort revidiert werden."
            ),
            exampleTips = listOf(
                "Unterstützte Sprach-/Text-Aktionen des KI-Agenten:",
                "- 'Trage 45 € für Tanken bei Shell ein' (ADD_TRANSACTION)",
                "- 'Neuer Vertrag: Fitnessstudio 29,90 € monatlich am 15.' (UPSERT_CONTRACT)",
                "- 'Erhöhe mein Monatsbudget für Lebensmittel auf 500 €' (UPDATE_CATEGORY_BUDGET)",
                "- 'Mein neues Sparziel ist Notgroschen mit 3.000 €' (UPDATE_SAVINGS_GOAL)",
                "- 'Setze meinen Stundenlohn auf 19,50 €' (UPDATE_SETTINGS)",
                "- 'Lösche die Buchung Tanken' (DELETE_ENTRY)"
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
            addedInVersion = "1.1.8"
        ),
        HelpEntry(
            id = ID_GEMINI_CLOUD_HYBRID,
            moduleNumber = 14,
            title = "14. Google Gemini Cloud-KI, Vision-Split & Sprach-Buchung",
            category = "Hybrid Cloud-KI & Vision (PRO)",
            shortSummary = "BYOK API-Key-Tresor (Standard: gemini-3.8-flash + Live-Modell-Abfrage & Auto-Switch), 10-Zeilen-Kontext-Finanzcoach, Vision-Kassenbon-Splitting & Vertragsbrief-Scan sowie 1-Tap Sprach-Buchung per Mikrofon.",
            stepByStepGuide = listOf(
                "Schritt 1 (Kostenlosen API-Key auf aistudio.google.com erstellen): Öffne im Browser https://aistudio.google.com/apikey, melde dich mit deinem Google-Konto an, tippe auf 'Create API Key' und kopiere den Schlüssel in die Zwischenablage.",
                "Schritt 2 (Key eintragen, Modell wählen & Verbindung testen): Tippe im Tab 'KI-Coach' oben auf das Status-Badge oder öffne 'Einstellungen'. Tippe auf 'Aus Zwischenablage einfügen'. Standardmäßig ist 'gemini-3.8-flash (Standard)' aktiv (alternativ 'gemini-3.8-pro', 'gemini-flash-latest', 'gemini-2.5-flash', freie Eingabe im Feld 'Aktiver Modell-Name' oder Live-Abruf über 'Verfügbare Modelle laden').",
                "Schritt 3 (Selbstheilender Auto-Switch): Empfiehlt der Google-Server ein neueres Modell (z. B. 'Please update your code to use models/...'), erkennt die App den Modellnamen automatisch per Regex, speichert ihn im Tresor und wiederholt die Anfrage sofort im Hintergrund.",
                "Schritt 4 (Hybrid KI-Finanzcoach & Vision-Scanner): Stelle Fragen im 'KI-Coach' (mit anonymisiertem 10-Zeilen-Kontext aus deiner Room-DB) oder fotografiere Kassenbons (inkl. Pfand-Abzug & Split-Buchung) und Versicherungs-/Vertragsbriefe.",
                "Schritt 5 (1-Tap Sprach-Buchung per Mikrofon): Tippe im '+'-Buchungs-Sheet oder im 'KI-Coach' auf das Mikrofon-Icon und sprich z. B.: 'Ich habe heute für 64 Euro getankt und beim Bäcker 4,80 Euro ausgegeben'. Mit einem Tap auf 'Alle buchen' werden beide Buchungen sofort gespeichert."
            ),
            exampleTips = listOf(
                "Zukunftssicher ohne App-Neubau: Über den Button 'Verfügbare Modelle laden' ruft die App live GET /v1beta/models ab und wählt automatisch das neueste freigeschaltete Flash-Modell für deinen API-Key aus.",
                "Ausfallsicherer Hybrid-Betrieb: Selbst ohne API-Key oder ohne Internet funktionieren KI-Coach, Beleg-Scanner (per Google ML Kit) und der Mehrfach-Sprachbuchungs-Parser nahtlos weiter."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = BuildConfig.VERSION_NAME
        )
    )

    val changelogEntries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            versionName = "1.3.8",
            buildNumber = 20,
            date = "17.11.2026",
            newFeatures = listOf(
                "Sparbooster UI-Entschlackung: Redundante leere Platzhalter-Karte entfernt – der Screen bleibt unter dem '+ Neues Sparziel anlegen'-Button absolut sauber.",
                "Sortier-Menü in 'Letzte Buchungen': Neues dezentes Sortier-Icon mit DropdownMenu ('Nach Datum', 'Nach Summe', 'Nach Name').",
                "Festgepinnte Einnahmen (Priorität 1): Einnahmen (wie das grüne Gehalt) bleiben unabhängig vom gewählten Sortiermodus stets unverrückbar ganz oben an Position 1.",
                "Farb-Klarheit in der Buchungsliste: Ausgaben-Beträge werden nun unmissverständlich in Rot dargestellt, während Einnahmen leuchtend grün bleiben."
            )
        ),
        ChangelogEntry(
            versionName = "1.3.7",
            buildNumber = 19,
            date = "17.11.2026",
            newFeatures = listOf(
                "Offizielles Rebranding in 'NovaFinance': Zeitloser, moderner Markenauftritt ohne Jahreszahl in der gesamten App, im Android-Manifest und im Menü.",
                "Neues Material 3 Expressive Icon: Dynamische Schild-Form mit Violett/Smaragdgrün-Elementen und Sicherheits-Grid im modernen M3-Squircle-Design.",
                "UI-Cleanup im Sparbooster: Untere Historien-Liste restlos entfernt – der Tab endet nun clean nach der letzten Sparziel-Karte.",
                "Bereinigter Sparziel-Button: Überflüssiges doppeltes Plus entfernt für einen sauberen Button '+ Neues Sparziel anlegen'."
            )
        ),
        ChangelogEntry(
            versionName = "1.3.6",
            buildNumber = 18,
            date = "17.11.2026",
            newFeatures = listOf(
                "Vollflexibler Sparbooster mit Mehr-Karten-Verwaltung: Lege beliebig viele individuelle Sparziele mit Titel, Bank/Speicherort (z. B. BW-Bank, WeltSparen), Zielbetrag, Startkapital, monatlicher Sparrate und Wunschfarbe an.",
                "Karten-Aktionen & Zielgenaue Einzahlungen: Jede Karte verfügt über Bearbeiten- und Löschen-Aktionen sowie direkte Sofort-Einzahlungs-Buttons (+25 €, +50 €, +100 € oder freier Betrag), die dem Ziel live gutgeschrieben werden.",
                "Summen-Kopfzeile: Sofortiger Überblick über die Gesamtsumme aller Ersparnisse und die monatliche Gesamtrate über alle Sparziele hinweg.",
                "Automatischer Monatszuwachs: Fällige monatliche Sparraten fließen stichtagsgenau in den Fortschrittsbalken und die Berechnung ein."
            )
        ),
        ChangelogEntry(
            versionName = "1.3.5",
            buildNumber = 17,
            date = "16.11.2026",
            newFeatures = listOf(
                "Monats-Register & Raten abhaken: Aufklappbare Einzelraten-Übersicht je Posten mit individuellen Monats-Checkboxen. Abgehakte Monate werden durchgestrichen, grün markiert und passen den Restbetrag live an.",
                "Gesamtzeitraum im Ordner-Header: Zeigt im Header jedes Ordners sofort die gesamte Laufzeit und Gesamtmonate an (z. B. 'Laufzeit: Mai 2026 – Jun 2027 (14 Monate gesamt)').",
                "Einklappbare Summen-Karten: Sowohl auf der Startseite ('Übersicht') als auch im Ratenzahlungs-Tracker lassen sich die großen Kopfkarten per dezentem Pfeil-Button einklappen, sodass nur eine schlanke Zeile bleibt.",
                "Stichtags-Automatik & Checklisten-Flexibilität: Nahtloses Zusammenspiel zwischen automatischer Stichtags-Berechnung und individuellen manuellen Checkboxen inklusive 1-Klick Rücksetz-Option."
            )
        ),
        ChangelogEntry(
            versionName = "1.3.4",
            buildNumber = 16,
            date = "16.11.2026",
            newFeatures = listOf(
                "Keine Duplikate beim Bearbeiten: Feste ID-Bindung und intelligenter Duplikat-Schutz (gleicher Name im selben Ordner überschreibt bestehenden Eintrag statt Duplikate anzulegen). Automatische Selbstbereinigung überzähliger Alt-Duplikate.",
                "Glasklare Subtraktions-Logik in der Kopf-Kachel: 'Urspr. Gesamtbetrag' minus 'Bereits bezahlt / getilgt' (grün) ergibt unmissverständlich 'Aktuell noch offen' (Restbetrag).",
                "Vollständiges Startdatum (TT.MM.JJJJ): Ersetzt das alte 'Fällig am (Tag)'-Feld durch ein vollwertiges Datumsfeld inklusive klickbarem Kalender-Icon (DatePickerDialog) und manueller Direkteingabe.",
                "Rückwirkende Automatik-Berechnung: Vergangene Fälligkeitstage seit dem Startdatum werden exakt berechnet (inkl. automatischem Tag-31-Schutz für kürzere Monate), um bereits getilgte Raten sofort abzuziehen.",
                "Live-Rechenbox im Dialog & 3-Zeilen-Karten in den Ordnern: Dynamische Tilgungsvorschau im Dialog sowie aufgeräumte Karten mit prominentem Restbetrag und Schnell-Aktionen (Bearbeiten, Ablösen, Löschen).",
                "Bereinigte Header-Buttons: Ordner-Icon vor '+ Ordner' entfernt – klare, bündige Buttons '+ Ordner' und '+ Posten'."
            )
        ),
        ChangelogEntry(
            versionName = "1.3.3",
            buildNumber = 15,
            date = "16.11.2026",
            newFeatures = listOf(
                "100 % Clean-Look: Alle störenden Karten-Icons und Emojis aus dem Menü, der TopBar und den Karten von 'Ratenzahlungen & Offene Posten' restlos entfernt.",
                "Neues Ordner-System für Ratenzahlungen: Beliebig viele Ordner erstellen (+ Ordner), umbenennen, löschen und Posten übersichtlich gruppieren.",
                "Aufklappbare Ordner-Karten mit Live-Zwischensummen (Anzahl Posten, offene Restschuld und monatliche Ratenbelastung je Ordner).",
                "Schnell-Aktionen im Ordner-Header: Direkt per '+' neue Posten in den jeweiligen Ordner einfügen, Ordner umbenennen oder löschen.",
                "Neue STIL-GESETZ Pflichtregel in QA_AUTOPILOT.md: Strikte Typografie ohne bunte Emojis in Menüs, Titeln und Headern."
            )
        ),
        ChangelogEntry(
            versionName = "1.3.2",
            buildNumber = 14,
            date = "16.11.2026",
            newFeatures = listOf(
                "Neues Modul 'Ratenzahlungen & Offene Posten' im Hamburger-Menü: Vollautomatisches Tracking von Finanzierungen, Ratenkäufen und offenen Posten.",
                "Vollautomatische monatliche Reduzierung: Zählt abgelaufene Fälligkeitstage seit Startdatum und tilgt Restbetrag sowie Gesamtschuld ohne manuelles Abhaken.",
                "Echtzeit-Summenkachel & Tilgungsbalken: Offene Gesamtschuld, monatliche Ratenbelastung und prozentualer Tilgungsfortschritt auf einen Blick.",
                "1-Tap Schnell-Ablösung & Sonderzahlungen: Teilzahlungen flexibel eintragen oder mit 1 Klick auf 'Komplett abbezahlt setzen'.",
                "Automatische Monatsbudget-Kopplung: Laufende Raten werden als Fixkosten abgezogen und entfallen nach vollständiger Tilgung automatisch."
            )
        ),
        ChangelogEntry(
            versionName = "1.3.1",
            buildNumber = 13,
            date = "16.11.2026",
            newFeatures = listOf(
                "Radikale Entschlackung der Übersicht: Überflüssigen Woche/Monat/Jahr-Umschalter im Header komplett entfernt – die App rechnet konsistent und geradlinig im Monats-Modus (< November 2026 >).",
                "Fokussierte Startseite ('Übersicht'): 'Budgets'-Karte mit Kreisringen und 'Nächste Abbuchungen (Wächter)' komplett entfernt.",
                "Zwei Kernelemente auf einen Blick: Oben die große violette Hero-Karte ('vom Gehalt verfügbar', 'Einnahmen', 'Heute frei') und direkt darunter die Liste 'Letzte Buchungen' mit viel Platz für alle Einträge."
            )
        ),
        ChangelogEntry(
            versionName = "1.3.0",
            buildNumber = 12,
            date = "16.11.2026",
            newFeatures = listOf(
                "Strikte Finanz-Mathematik & Verträge-Bereinigung: Eindeutige Formel 'Verfügbar = Einnahmen - Fixkosten/Verträge - Variable Ausgaben'. Reiter 'Verträge' aufgeteilt in feste Einnahmen (+) und echte Verträge/Fixkosten (-). Im Buchungsdialog klarer Umschalter '➖ Ausgabe' vs. '➕ Einnahme' mit voller Komma- & Punkttoleranz.",
                "Strenger Duplikat-Wächter & Debounce: Beseitigung redundanter Buttons, intelligenter Duplikat-Schutz bei Kategorien (DISTINCT / Case-Insensitive) und Doppelklick-Sperre (Debounce) in allen Dialogen.",
                "UI-Politur: Einheitliches 104dp Content-Padding für alle LazyColumns gegen verdeckte Einträge und bündiges Tastatur-Padding im KI-Coach.",
                "Autonomes Auto-QA-System: Feste Pflicht-Regeldatei QA_AUTOPILOT.md und automatische JUnit-Test-Suite SmartFinanceSelfTest.kt (Mathe-Check, Zeitraum-Umrechnung, Duplikat-Schutz, 0,00-€-Null-Start)."
            )
        ),
        ChangelogEntry(
            versionName = "1.2.2",
            buildNumber = 11,
            date = "15.11.2026",
            newFeatures = listOf(
                "Struktur-Update im Reiter 'Verträge': Klare Trennung in Sektion A 'Feste monatliche Einnahmen (+)' (Gehalt als positive Budget-Basis) und Sektion B 'Verträge & Fixkosten (Feste Ausgaben -)' mit Gesamtsumme ('Fixkosten gesamt: -XXX,XX € / Monat').",
                "Keine Vermischung von variablen Budgets mehr: 'Lebensmittel', 'Freizeit' etc. und der Text 'Monatslimit: 0,00 €' wurden vollständig aus den Verträgen entfernt.",
                "Variable Monatsbudgets direkt in den Budget-Ringen verwalten: Limits für Alltagsausgaben werden direkt im Cockpit über die Kreis-Ringe sowie die Dialoge ('Alle anzeigen >' & '+ Neu') gepflegt.",
                "Strikte Ausgaben-Mathematik in der violetten Hero-Karte ('vom Gehalt verfügbar'): Einnahmen abzüglich Fixkosten und variabler Ausgaben reduzieren das freie Budget verlässlich."
            )
        ),
        ChangelogEntry(
            versionName = BuildConfig.VERSION_NAME,
            buildNumber = BuildConfig.BUILD_NUMBER,
            date = "15.11.2026",
            newFeatures = listOf(
                "Neues Standard-Modell 'gemini-3.8-flash' & 4 Schnellwahl-Chips ('gemini-3.8-flash (Standard)', 'gemini-3.8-pro', 'gemini-flash-latest', 'gemini-2.5-flash') im API-Key-Tresor.",
                "Zukunftssicheres editierbares Modell-Textfeld ('Aktiver Modell-Name') sowie Live-Abfrage 'Verfügbare Modelle laden' (GET /v1beta/models) mit automatischer Auswahl des neuesten freigeschalteten Flash-Modells.",
                "Selbstheilender Auto-Fallback im GeminiApiService.kt: Erkennt Hinweise wie 'Please update your code to use models/...' automatisch per Regex, speichert das empfohlene Modell im ApiKeyManager und wiederholt den Request sofort nahtlos im Hintergrund.",
                "Hybrid KI-Finanzcoach (10-Zeilen-Room-Kontext), multimodaler Vision-Scanner (Kassenbon-Splitting & Vertragsbrief-Erkennung) und 1-Tap Sprach-Buchung ('Smart Voice Entry')."
            )
        ),
        ChangelogEntry(
            versionName = "1.2.1",
            buildNumber = 10,
            date = "15.11.2026",
            newFeatures = listOf(
                "Strikte Trennung von Einnahmen (+) & Ausgaben (-) im Buchungs-Sheet: Neuer großer Segmented-Toggle ('➖ Ausgabe' vs. '➕ Einnahme') mit dynamischer Kategorien- und Schnellauswahl (Gehalt, Rückerstattung, Bonus).",
                "Korrekte Finanz-Mathematik: Gesamteinnahmen (gebuchte Einnahmen + optionales geplantes Grundgehalt) abzüglich Gesamtausgaben und offener Fixkosten reduzieren das verfügbare Budget zwingend korrekt.",
                "Eindeutige visuelle Kennzeichnung: Ausgaben mit Minus ('-45,20 €'), Einnahmen mit Plus ('+2.450,00 €') in kräftigem Smaragdgrün und grünem Icon-Hintergrund in allen Listen & Analysen.",
                "Keyboard-Fix & bündige Eingabeleiste im KI-Coach: Kein doppelter Abstand über der Tastatur mehr und automatisches Einklappen des Headers beim Schreiben."
            )
        ),
        ChangelogEntry(
            versionName = "1.2.0",
            buildNumber = 9,
            date = "15.11.2026",
            newFeatures = listOf(
                "Vollautomatischer KI-Agent im Chat (JSON Action-Engine): Vollzugriff auf alle Room-Tabellen. Trägt Buchungen, Fixkosten-Verträge, Budgets, Sparziele und Einstellungen autonom direkt in die Datenbank ein.",
                "Sicherheits-Netz im Chat mit 1-Tap 'Rückgängig': Jede automatisch ausgeführte KI-Aktion wird transparent mit grüner Bestätigungs-Kachel und 'Rückgängig'-Button unter der Chat-Antwort angezeigt.",
                "Dokumenten-, PDF- & Screenshot-Upload direkt im Chat (📎-Button): Wähle PDF-Dateien oder Fotos aus; der native Android PdfRenderer rendert PDFs lokal und die KI erfasst alle Buchungen, Gehälter oder Verträge vollautomatisch.",
                "Keyboard-Fix & Auto-Collapse Header: Kein doppelter Abstand über Gboard mehr (bündig sitzende Eingabeleiste) und automatisches Einklappen der oberen Info-Karte beim Tippen für maximalen Chat-Fokus."
            )
        ),
        ChangelogEntry(
            versionName = "1.1.8",
            buildNumber = 8,
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
