# QA_AUTOPILOT.md — Pflicht-Qualitätsrichtlinien für SmartFinance

Diese Datei definiert die verbindlichen Qualitäts-, Architektur- und Praxis-Regeln für alle zukünftigen Builds, Commits und Agent-Iterationen in SmartFinance. Vor jedem Git-Push MÜSSEN alle hier definierten Kriterien erfüllt sein.

---

## 1. Keine toten Buttons & UI-Aktionen (No Dead Handlers)
- Jedes klickbare Element (`Button`, `IconButton`, `Surface.clickable`, `Card`, Dialog-Buttons) MUSS eine echte, wirksame Funktion ausführen.
- Verboten: Leere Lambdas wie `onClick = {}` oder reine Platzhalter ohne State-Änderung oder Navigation.
- Alle Bearbeiten- und Löschen-Fenster müssen funktionstüchtig angebunden sein.
- Ein Dialog muss sich bei Abbruch oder erfolgreichem Speichern sauber schließen (`onDismiss()`).

---

## 2. Strikte Finanz-Mathematik & Vorzeichen-Klarheit
- **Zentrale Haushalts-Formel:**
  $$\text{Verfügbares Budget} = \text{Gesamteinnahmen} - \text{Fixkosten/Verträge} - \text{Variable Ausgaben}$$
- **Vorzeichen und Semantik:**
  - **Einnahmen (`isIncome == true` / `TransactionType.INCOME` / geplantes Grundgehalt):**
    Erhöhen das Budget (`+`, Smaragdgrün `#00E676`).
  - **Ausgaben (`TransactionType.EXPENSE`):**
    Reduzieren IMMER das Budget (`-`, Rot `#EF4444` oder Neutral).
  - **Verträge & Fixkosten (`CategoryType.FIXED_EXPENSE`):**
    Sind feste Abzüge (`-`). Darstellung stets mit Minuszeichen (z. B. `-40,00 € / Monat`).
- **Klare Buchungsauswahl:**
  - Im `+`-Buchungsdialog sitzt ganz oben der 2er-Segmented-Toggle: `➖ Ausgabe` (Standard) vs. `➕ Einnahme`.
- **Komma-Toleranz:**
  - Jedes Eingabefeld für Geldbeträge akzeptiert sowohl deutsches Komma (`12,50`) als auch Punkt (`12.50`) via `SmartFinanceEngine.parseEuroAmount()`.

---

## 3. Strenger Duplikat-Schutz & Debounce
- **Keine doppelten Buttons:**
  - Auf keinem Screen dürfen redundante Buttons für denselben Zweck existieren (z. B. kein doppelter `+`-Button im Reiter "Verträge", wenn oben rechts bereits `+ Neu` platziert ist).
- **Datenbank-Deduplizierung:**
  - Kategorien (wie Lebensmittel, Handyvertrag, Kfz-Versicherung etc.) dürfen niemals mehrfach in der Datenbank angelegt werden.
  - Case-Insensitive Prüfung beim Speichern (`deduplicateCategories()` / Namensvergleich mit `ignoreCase = true`).
- **Doppelklick-Sperre (Debounce):**
  - Alle Speichern- und Buchen-Buttons müssen gegen schnelles Doppeltippen gesichert sein (`isSubmitting`-Guard im UI sowie Zeit-Debounce im ViewModel).

---

## 4. Echter 0,00-€-Null-Start
- Eine frische App-Installation ohne Nutzereingaben MUSS exakt bei `0,00 €` freiem Budget starten.
- Es dürfen niemals ungefragt Demo-, Test- oder Fake-Transaktionen in die produktive Nutzer-Datenbank geschrieben werden.
- Platzhalter (z. B. in "Nächste Abbuchungen" oder "Letzte Buchungen") müssen klickbar sein und den Nutzer zur Direkteingabe auffordern.

---

## 5. Klare, einheitliche Monats-Architektur & fokussiertes Cockpit
- Die App rechnet überall fest und eindeutig im Monats-Modus (`< Monat Jahr >`).
- Mit `<` und `>` wechselt der Nutzer monatsweise vor oder zurück.
- Die Startseite (`Übersicht`) bleibt radikal vereinfacht und fokussiert:
  1. Oben die große violette Hero-Karte (`vom Gehalt verfügbar`, `Einnahmen`, `Heute frei`).
  2. Direkt darunter die Liste `Letzte Buchungen` mit maximalem Platz für alle Einträge.

---

## 6. UI- & Layout-Integrität
- **Kein Verdecken durch Buttons:**
  - Jede scrollbare Liste (`LazyColumn`) erhält `contentPadding = PaddingValues(..., bottom = 104.dp)`, damit der unterste Eintrag niemals durch FloatingActionButtons oder BottomNav verdeckt wird.
- **Tastatur-Bündigkeit:**
  - Im KI-Coach (`AiCoachTabScreen`) liegt das Texteingabefeld ohne störenden Zwischenraum direkt auf der Tastaturkante (`isKeyboardOpen`-Handling), und die Header-Karte klappt beim Tippen per `AnimatedVisibility(visible = !isKeyboardOpen)` automatisch ein.

---

## 7. Automatische Qualitätssicherung vor JEDEM Git-Push
Bevor ein Commit gepusht werden darf, MÜSSEN folgende Schritte erfolgreich im Terminal ausgeführt worden sein:
1. `AppHelpRegistry.kt` aktualisieren (neuer `HelpEntry` und `ChangelogEntry`).
2. `./gradlew testDebugUnitTest` — 100 % der JUnit-Tests in `SmartFinanceSelfTest.kt` müssen grün sein:
   - Test 1: Mathe-Check ($2.500 - 400 - 100 = 2.000,00\text{ €}$).
   - Test 2: Zeitraum-Umrechnung (Woche, Monat, Jahr).
   - Test 3: Duplikat-Schutz für Kategorien.
   - Test 4: 0,00-€-Null-Start bei leerer Datenbank.
3. `./gradlew assembleDebug` — Erfolgreicher fehlerfreier Build.
