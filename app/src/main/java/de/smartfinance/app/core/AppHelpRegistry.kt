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
 * Single Source of Truth für alle 13 Kernmodule, kontextsensitive `?`-Hilfe-Karten,
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
            title = "1. Monats-Zeitstrahl & Soll/Ist-Header",
            category = "Cockpit & Übersicht",
            shortSummary = "Horizontale Wisch-Ansicht durch vergangene und zukünftige Monate, Einnahmen/Ausgaben-Durchschnitt (3/6/12 Monate) und flexibler Abrechnungstag (1., 15. oder 28.).",
            stepByStepGuide = listOf(
                "Wische im Cockpit horizontal nach links, um vergangene Abrechnungsmonate zu analysieren, oder nach rechts für den aktuellen Monat und zukünftige Prognose-Monate.",
                "Tippe im Soll/Ist-Header oben rechts auf die Chips '3M', '6M' oder '12M', um den historischen Durchschnitt deiner Einnahmen und Ausgaben über 3, 6 oder 12 Monate direkt mit dem laufenden Monat zu vergleichen.",
                "Stelle unter 'Einstellungen & Demo-Daten' deinen persönlichen Gehalts-Abrechnungstag auf den 1., 15. oder 28. des Monats ein."
            ),
            exampleTips = listOf(
                "Tipp: Kommt dein Gehalt am 28. des Vormonats, stelle den Abrechnungstag auf den 28. ein – der Zeitstrahl berechnet dein Monatsbudget dann exakt vom 28. bis zum 27. des Folgemonats.",
                "Über den Button 'Heute' im Monats-Header springst du aus jedem Monat sofort zurück in die Gegenwart."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_DAILY_BUDGET_ROLLOVER,
            moduleNumber = 2,
            title = "2. Dynamisches Tagesbudget & Rollover-Formel",
            category = "Cockpit & Budgetlogik",
            shortSummary = "Berechnet jeden Tag neu, wie viel Geld du heute bedenkenlos ausgeben kannst – inklusive automatischem Rollover nicht verbrauchter Beträge auf den Restmonat.",
            stepByStepGuide = listOf(
                "Vom monatlichen Gesamteinkommen werden zuerst alle Fixkosten (inkl. noch kommender Abbuchungen), deine Dauer-Sparrate sowie 1/12-Jahresrücklagen abgezogen.",
                "Das verbleibende freie Monatsbudget wird nach Abzug deiner bisherigen variablen Ausgaben durch die verbleibenden Kalendertage des Monats (inkl. heute) geteilt.",
                "Gibst du heute weniger aus als dein Tagesbudget, steigt dein Tagessatz für alle restlichen Tage des Monats automatisch an (positiver Rollover)."
            ),
            exampleTips = listOf(
                "Formel: Freies Tagesbudget = (Einnahmen - Fixkosten gesamt - 1/12-Rücklagen - Sparrate - Variable Ausgaben bisher) ÷ Resttage im Monat.",
                "Beispiel: Bei 600 € freiem Monatsbudget und 20 Resttagen hast du 30,00 €/Tag. Gibst du heute 0 € aus, stehen dir ab morgen 600 € ÷ 19 Tage = 31,58 €/Tag zur Verfügung!"
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_DEBIT_GUARDIAN_WAVE,
            moduleNumber = 3,
            title = "3. Abbuchungs-Wächter & Grafische Monats-Belastungswelle",
            category = "Cashflow & Schutz",
            shortSummary = "Countdown für die nächste anstehende Abbuchung plus hochauflösendes Canvas-Wellendiagramm (Tag 1–31) mit Belastungs-Peaks und vertikaler 'Heute'-Trennlinie.",
            stepByStepGuide = listOf(
                "Jede Fixkosten-Kategorie und jeder Dauerauftrag speichert seinen genauen Abbuchungstag im Monat (Tag 1 bis 31).",
                "Das Wächter-Widget direkt unter dem Header zählt die Tage bis zur nächsten Abbuchung herunter und zeigt die Summe aller in diesem Monat noch offenen Lastschriften.",
                "Im Canvas-Diagramm darunter siehst du Tag 1 bis 31 aufgeteilt in Monatsanfang (1–10), Mitte (11–20) und Ende (21–31): Erledigte Tage links der 'Heute'-Linie sind grün, kommende Abbuchungsspitzen (Peaks) leuchten bernsteinfarben/rot."
            ),
            exampleTips = listOf(
                "Beispiel: 'Nächste Abbuchung: In 1 Tag (15.11.) – Handyvertrag (29,99 €) | Noch offen diesen Monat: 134,89 €'.",
                "So vermeidest du Rücklastschriften, weil du schon am 10. des Monats siehst, welche großen Brocken am 15. und 28. noch vom Konto abgehen."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_DYNAMIC_TABS_CONTRACTS,
            moduleNumber = 4,
            title = "4. Dynamische Kategorien-Tabs & Vertrags-Wecker",
            category = "Kategorien & Verträge",
            shortSummary = "Vorinstallierte Tabs (Versicherung, Handy, Kfz, Lebensmittel, Fixkosten, Sparen), frei erweiterbar um eigene Tabs inkl. 30-Tage-Vorwarnung vor Vertragsende.",
            stepByStepGuide = listOf(
                "Tippe im Cockpit auf die horizontal scrollbaren Filter-Chips, um Buchungen und Statistiken sofort nach einem Tab zu filtern.",
                "Tippe ganz rechts auf '+ Tab hinzufügen' oder öffne im Menü 'Kategorien, Sparziele & Verträge', um eigene Tabs mit Icon, Monatslimit und Fälligkeitstag (1–31) anzulegen.",
                "Hinterlege bei Verträgen (z. B. Handyvertrag, Strom, Kfz-Versicherung) das Vertragsende- bzw. Kündigungs-Stichtagsdatum – der Vertrags-Wecker warnt dich automatisch ab 30 Tage vorher!"
            ),
            exampleTips = listOf(
                "Beispiel: Läuft dein Handyvertrag am 30.11. aus, erscheint im Cockpit automatisch das Warn-Badge '⏰ Vertrags-Wecker: Handyvertrag endet in 16 Tagen – jetzt Tarif vergleichen!'.",
                "Du kannst jeden Tab jederzeit bearbeiten oder eigene Kategorien wie 'Haustier', 'Fitnessstudio' oder 'Kinder' ergänzen."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_CREEPING_COST_RADAR,
            moduleNumber = 5,
            title = "5. Schleichende-Kosten-Radar & Budget-Überschreitungs-Warner",
            category = "KI-Anomalie-Erkennung",
            shortSummary = "Automatische Frühwarnung im Dashboard, sobald eine Kategorie ihr Monatslimit überschreitet oder mehr als 20 % über dem historischen 3-Monats-Schnitt liegt.",
            stepByStepGuide = listOf(
                "Die lokale Finanz-Engine vergleicht bei jeder Buchung die aktuellen Monatsausgaben jeder Kategorie mit dem Durchschnitt der letzten 3 Monate.",
                "Steigt eine Kategorie um mehr als 20 % über ihren 3-Monats-Schnitt (oder über ihr festgesetztes Tab-Budgetlimit), blendet das Cockpit sofort das gelb/rote Radar-Banner ein.",
                "Tippe auf das Warn-Banner, um die betroffene Kategorie direkt zu filtern und die Preistreiber-Buchungen zu prüfen."
            ),
            exampleTips = listOf(
                "Beispiel: '⚠️ Schleichende Kosten erkannt: Lebensmittel liegt aktuell +28 % über deinem 3-Monats-Schnitt (460 € statt Ø 360 €).'",
                "Auch schleichende Preiserhöhungen bei Versicherungen oder Streaming-Abos fallen sofort auf."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_ANNUAL_COST_SMOOTHER,
            moduleNumber = 6,
            title = "6. 1/12-Jahreskosten-Glätter",
            category = "Budget-Glättung",
            shortSummary = "Legt jährliche oder vierteljährliche Großrechnungen (z. B. Kfz-Versicherung, GEZ, Jahresbeiträge) virtuell auf 12 Monate um, damit kein Monat ins Minus rutscht.",
            stepByStepGuide = listOf(
                "Aktiviere beim Anlegen oder Bearbeiten einer Buchung bzw. eines Dauerauftrags mit jährlichem/vierteljährlichem Turnus den Schalter 'Auf 12 Monate glätten (1/12-Rücklage)'.",
                "SmartFinance zieht jeden Monat automatisch exakt 1/12 (bei Jahreszahlung) bzw. 1/3 (bei Quartalszahlung) als virtuelle Rücklage von deinem freien Budget ab.",
                "Im Cockpit siehst du in der Karte '1/12-Jahreskosten-Glätter' jederzeit transparent, wie viel Euro monatlich für deine Jahresrechnungen reserviert werden."
            ),
            exampleTips = listOf(
                "Beispiel: Deine Kfz-Versicherung kostet 660,00 € einmal im Jahr. Mit aktivem 1/12-Glätter werden jeden Monat sanfte 55,00 € vom freien Budget reserviert statt eines 660-€-Schocks im November!",
                "Tippe auf das '?'-Icon direkt an der 1/12-Glätter-Karte im Cockpit, um diese Erklärung jederzeit nachzulesen."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_SAVINGS_GOAL_TRACKER,
            moduleNumber = 7,
            title = "7. Etappen-Sparziel-Tracker & Dauer-Sparraten",
            category = "Sparen & Vermögensaufbau",
            shortSummary = "Visueller Fortschrittsbalken für dein Etappen-Sparziel (z. B. 3.000-€-Notgroschen) inklusive KI-Berechnung des exakten Zieldatums.",
            stepByStepGuide = listOf(
                "Wähle im Cockpit den Tab 'Sparen' oder öffne im Menü 'Kategorien, Sparziele & Verträge'.",
                "Lege deinen Zielbetrag (z. B. 3.000 €), deinen aktuellen Startbestand und deine monatliche Dauer-Sparrate (z. B. 250 €/Monat) fest.",
                "Jede Buchung in der Kategorie 'Sparen' erhöht deinen Fortschrittsbalken automatisch, und die Engine berechnet auf den Monat genau, wann du dein Ziel erreichst."
            ),
            exampleTips = listOf(
                "Beispiel: 'Etappen-Sparziel Notgroschen: 1.750 € von 3.000 € erreicht (58 %) – Bei 250 €/Monat erreichst du dein Ziel in 5 Monaten (April 2027)!'",
                "Tipp: Arbeite mit motivierenden Etappen (1.000 € → 3.000 € → 10.000 €) statt eines einzigen riesigen Ziels."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_HYBRID_AI_CHAT,
            moduleNumber = 8,
            title = "8. Lokale Smart Finance Hybrid-KI & Q&A-Chat",
            category = "Lokale KI & Prognose",
            shortSummary = "100 % lokaler, datenschutzsicherer deutscher Finanz-Assistent mit Werktags-/Wochenend-Burn-Rate, Monatsend-Prognose und Echtzeit-Q&A-Chat.",
            stepByStepGuide = listOf(
                "Tippe unten im Cockpit auf den Button '🤖 KI-Finanzcheck & Chat' (öffnet sich daumenfreundlich als Bottom-Sheet in der unteren Bildschirmhälfte).",
                "Tippe auf einen der Schnell-Frage-Chips oder tippe deine Frage auf Deutsch ein.",
                "Die Engine analysiert in Millisekunden deine lokale Room-Datenbank ohne Internetverbindung und liefert exakte Zahlen."
            ),
            exampleTips = listOf(
                "Unterstützte deutsche Beispiel-Fragen im Chat:",
                "• 'Wie viel habe ich heute/diesen Monat noch frei?'",
                "• 'Wann geht die nächste Zahlung ab?' / 'Was wird diese Woche noch abgebucht?'",
                "• 'Wann geht im Monat am meisten ab?'",
                "• 'Was gebe ich im Schnitt für Versicherungen / Handyvertrag aus?'",
                "• 'Reicht mein Geld bis Monatsende?' / 'Wann erreiche ich mein Sparziel?'",
                "• 'Was kannst du alles?' oder 'Hilfe'"
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_WHAT_IF_WORK_HOURS,
            moduleNumber = 9,
            title = "9. \"Was-wäre-wenn\"-Simulator & Arbeitsstunden-Umrechner",
            category = "Lokale KI & Kaufcheck",
            shortSummary = "Gib im Chat einfach 'Check: 85 €' ein: Die KI prüft dein Restbudget + offene Fixkosten mit Grün/Gelb/Rot-Ampel und rechnet den Preis in deine Arbeitszeit um.",
            stepByStepGuide = listOf(
                "Öffne den KI-Chat und tippe z. B. 'Check: 85 €' oder 'Kann ich mir 120 € leisten?' ein.",
                "Der Simulator prüft, ob nach Abzug dieses Spontankaufs UND aller noch offenen Fixkosten-Abbuchungen des Monats genug Tagesbudget übrig bleibt.",
                "Du erhältst sofort eine Ampel-Bewertung (🟢 Grün / 🟡 Gelb / 🔴 Rot), dein neues Tagesbudget nach dem Kauf sowie die Umrechnung in Netto-Arbeitsstunden anhand deines Stundenlohns (Standard: 15,75 €/h)."
            ),
            exampleTips = listOf(
                "Beispiel für 'Check: 85 €' bei 15,75 €/Std.: '🟢 GRÜN – Kauf problemlos möglich! Entspricht 5 Std. 24 Min. deiner Arbeitszeit. Dein Tagesbudget sinkt für die restlichen 16 Tage um 5,31 €/Tag.'",
                "Deinen persönlichen Netto-Stundenlohn kannst du jederzeit unter 'Einstellungen & Demo-Daten' anpassen."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_ONE_TAP_RECURRING,
            moduleNumber = 10,
            title = "10. 1-Tap-Schnelleingabe & Automatische Daueraufträge",
            category = "Alltag & Automatisierung",
            shortSummary = "Daumenfreundliches Taschenrechner-Numpad, 1-Tap-Schnellvorlagen für Alltagsausgaben und vollautomatische Buchung von Daueraufträgen am Fälligkeitstag.",
            stepByStepGuide = listOf(
                "Tippe im Cockpit auf eine der 1-Tap-Schnellvorlagen (z. B. '☕ Bäcker 4,50 €', '🛒 Einkauf 35,00 €', '⛽ Tanken 65,00 €'), um eine Ausgabe mit einem einzigen Fingertipp sofort zu buchen.",
                "Für individuelle Beträge tippe auf '+ Buchung' – es öffnet sich in der unteren Bildschirmhälfte das große Custom-Numpad mit integrierter Plus/Minus-Rechenfunktion.",
                "Wiederkehrende Fixkosten, Gehalt und Dauer-Sparraten werden im laufenden Monat bis zum heutigen Kalendertag automatisch am jeweiligen Fälligkeitstag (1–31) gebucht."
            ),
            exampleTips = listOf(
                "Tipp: Im großen Taschenrechner-Numpad kannst du mehrere Posten direkt addieren (z. B. '12,50 + 4,80 =') und sofort speichern!",
                "Du kannst beliebige eigene 1-Tap-Vorlagen anlegen."
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_OFFLINE_OCR_SCANNER,
            moduleNumber = 11,
            title = "11. Offline Beleg-Scanner (Google ML Kit OCR)",
            category = "Kamera & Belegerkennung",
            shortSummary = "Scanne Kassenbons komplett offline per Kamera oder Galerie: Automatische Erkennung von Gesamtbetrag, Datum und passender Kategorie anhand des Händlers.",
            stepByStepGuide = listOf(
                "Tippe im Cockpit auf das Kamera-Symbol ('📷 Beleg scannen') oder nutze den Homescreen-Shortcut.",
                "Fotografiere den Kassenbon, wähle ein Bild aus der Galerie oder teste den integrierten 'Beispiel-Kassenbon (OCR-Demo)'.",
                "Die On-Device-Texterkennung (Google ML Kit) extrahiert per Regex den höchsten plausiblen Euro-Gesamtbetrag (z. B. neben 'SUMME' / 'ZU ZAHLEN'), das Belegdatum und ordnet Händler wie REWE, ALDI, ARAL oder TELEKOM automatisch dem passenden Tab zu."
            ),
            exampleTips = listOf(
                "100 % Datenschutz: Das Foto verlässt niemals dein Smartphone, die OCR-Erkennung läuft komplett lokal auf dem Gerät.",
                "Vor dem Speichern kannst du Betrag, Titel und Kategorie noch mit einem Tipp anpassen."
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
                "Öffne über das linke Hamburger-Menü den Bereich '🔐 Daten-Tresor (Backup & Import)'.",
                "Mit '1-Tap JSON-Komplett-Backup exportieren' sicherst du alle Buchungen, Kategorien, Verträge, Sparziele und Einstellungen über das Android-Dateisystem.",
                "Mit 'JSON-Backup wiederherstellen' spielst du eine Sicherung jederzeit verlustfrei wieder ein, oder importierst über 'Bank-CSV importieren' Umsätze deiner Hausbank (inkl. automatischer Kategorisierung)."
            ),
            exampleTips = listOf(
                "Unterstützt deutsche Bank-CSV-Formate (Semikolon-getrennt, deutsche Kommazahlen wie '-45,90' und Datumsformate TT.MM.JJJJ).",
                "Im Daten-Tresor findest du außerdem einen 1-Tap-Testbutton 'Beispiel-Bank-CSV testen', um den CSV-Parser sofort auszuprobieren."
            ),
            requiredTier = AppTier.PRO_FULL,
            addedInVersion = "1.0.1"
        ),
        HelpEntry(
            id = ID_DEMO_SHORTCUTS_SIMULATOR,
            moduleNumber = 13,
            title = "13. Demo-Daten-Generator, App-Shortcuts & Lite/Pro-Simulator",
            category = "System, Test & Komfort",
            shortSummary = "6 Monate realistische Testdaten mit 1 Tap erzeugen/zurücksetzen, Homescreen-Shortcuts durch langes Drücken aufs App-Icon und Live-Umschalter zwischen Lite & Pro.",
            stepByStepGuide = listOf(
                "Öffne '⚙️ Einstellungen & Demo-Daten' und tippe auf '6 Monate Demo-Daten generieren', um sofort realistische Einnahmen, Ausgaben und verteilte Abbuchungstage (1., 5., 15., 20. und 28.) für alle Diagramme und KI-Prognosen zu erzeugen.",
                "Über 'Alle Daten zurücksetzen (Reset)' löschst du die Demo-Daten jederzeit mit einem Klick, um mit deinen echten Finanzen zu starten.",
                "Halte das SmartFinance-Icon auf deinem Android-Homescreen gedrückt, um direkt in 'Schnell-Ausgabe', 'Beleg scannen' oder 'KI-Finanzcheck' zu springen. Unter '💎 Lite vs. Pro Übersicht' kannst du zudem live den Lite-Modus simulieren."
            ),
            exampleTips = listOf(
                "Die 6-Monate-Demo-Daten erzeugen bewusst verteilte Abbuchungsspitzen am 1., 5., 15., 20. und 28. sowie eine Kosten-Anomalie und einen bald auslaufenden Handyvertrag, damit du jedes einzelne Feature sofort live erleben kannst!"
            ),
            requiredTier = AppTier.LITE_FREE,
            addedInVersion = "1.0.1"
        )
    )

    val changelogEntries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            versionName = BuildConfig.VERSION_NAME,
            buildNumber = BuildConfig.BUILD_NUMBER,
            date = "14.11.2026",
            newFeatures = listOf(
                "Vollständiges Release von SmartFinance 2026 (v${BuildConfig.VERSION_NAME}, Build #${BuildConfig.BUILD_NUMBER}) mit deterministischer In-Place-Update-Signatur und striktem Room-Datenschutz.",
                "Monats-Zeitstrahl (HorizontalPager) mit Soll/Ist-Kompakt-Header, 3/6/12-Monats-Durchschnitt und dynamischem Tagesbudget-Rollover.",
                "Abbuchungs-Wächter mit Countdown und grafischer Monats-Belastungswelle (Canvas Tag 1–31 inkl. Peaks und 'Heute'-Markierungslinie).",
                "Dynamische Kategorien-Tabs inkl. Vertrags-Wecker (30-Tage-Vorwarnung), Schleichende-Kosten-Radar (>20 % Abweichung) und 1/12-Jahreskosten-Glätter.",
                "Etappen-Sparziel-Tracker (3.000-€-Etappe) mit automatischer KI-Zieldatum-Prognose.",
                "Lokale Smart Finance Hybrid-KI mit deutschem Q&A-Chat, 'Was-wäre-wenn'-Simulator (Check: 85 € mit Grün/Gelb/Rot-Ampel) und Arbeitsstunden-Umrechner (15,75 €/h Standard).",
                "Einhand-Daumen-Ergonomie mit Taschenrechner-Numpad, 1-Tap-Schnellvorlagen, Offline Beleg-Scanner (Google ML Kit OCR) und Daten-Tresor (JSON-Backup & Bank-CSV-Import).",
                "6-Monate-Demo-Daten-Generator (Abbuchungstage 1., 5., 15., 20., 28.), Android Homescreen-Shortcuts und Lite-vs.-Pro-Live-Simulator."
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
