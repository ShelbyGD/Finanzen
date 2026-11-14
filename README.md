# SmartFinance 2026 – 100 % Lokale Android-Finanz-App (Kotlin, Jetpack Compose, Material 3, Room DB)

Eine produktionsreife, zu 100 % lokal arbeitende Android-Finanz-App mit moderner Architektur (**MVVM, Single-Activity, Kotlin Coroutines, StateFlow, Jetpack Compose, Material Design 3 inkl. Dynamic Colors & Dark Mode, Room DB & Google ML Kit OCR**).

---

## 🚀 1. Nahtlose In-Place-Update-Architektur (Drüber-Installieren ohne Datenverlust!)
- **Einheitliche Application-ID:** `de.smartfinance.app` (`minSdk = 29`, `targetSdk = 35`, `compileSdk = 35`).
- **Deterministischer Signatur-Keystore:** `app/keystore/smartfinance.jks` wird für `debug` und `release` identisch genutzt (mit automatischem `keytool`-Fallback in `app/build.gradle.kts` und `.github/workflows/android-build.yml`).
- **Automatische Versionierung in GitHub Actions:**
  - `versionCode = 100 + runNumber`
  - `versionName = "1.0.$runNumber"`
  - Sichtbar als `v1.0.X (Build #Y)` in der TopAppBar, im linken Hamburger-Menü und in der Hilfe-Zentrale.
- **Strikter Room-Datenschutz:** Kein `.fallbackToDestructiveMigration()` – bestehende Nutzerdaten bleiben bei jedem App-Update zu 100 % erhalten.

---

## 📚 2. Die 13 Kernmodule (Dokumentiert in `AppHelpRegistry.kt`)
1. **Monats-Zeitstrahl & Soll/Ist-Header** (`HorizontalPager`, 3/6/12-Monats-Schnitt, Abrechnungstag 1./15./28.)
2. **Dynamisches Tagesbudget & Rollover-Formel** (Automatischer Rollover nicht verbrauchter Tagesbudgets auf den Restmonat)
3. **Abbuchungs-Wächter & Grafische Monats-Belastungswelle** (Countdown + Compose `Canvas` Diagramm Tag 1–31 mit Peaks & vertikaler „Heute“-Linie)
4. **Dynamische Kategorien-Tabs & Vertrags-Wecker** (`Versicherung`, `Handyvertrag`, `Kfz-Versicherung`, `Lebensmittel`, `Fixkosten`, `Sparen` + eigene Tabs & 30-Tage-Vorwarnung)
5. **Schleichende-Kosten-Radar & Budget-Überschreitungs-Warner** (Automatische Warnung bei >20 % Abweichung vom 3-Monats-Schnitt oder überschrittenem Tab-Limit)
6. **1/12-Jahreskosten-Glätter** (Jährliche/vierteljährliche Kosten wie Kfz-Versicherung virtuell auf 12 Monate umlegen)
7. **Etappen-Sparziel-Tracker & Dauer-Sparraten** (Fortschrittsbalken z. B. für 3.000-€-Etappe inkl. KI-Zieldatum-Prognose)
8. **Lokale Smart Finance Hybrid-KI & Q&A-Chat** (Werktags-/Wochenend-Burn-Rate, Monatsend-Prognose & deutscher Echtzeit-Chat)
9. **„Was-wäre-wenn“-Simulator & Arbeitsstunden-Umrechner** (`Check: 85 €` im Chat mit Grün/Gelb/Rot-Ampel + Arbeitszeit-Umrechnung bei 15,75 €/h Standard)
10. **1-Tap-Schnelleingabe & Automatische Daueraufträge** (Daumen-Numpad mit Taschenrechner, 1-Tap-Vorlagen & automatische Buchung am Fälligkeitstag)
11. **Offline Beleg-Scanner (Google ML Kit OCR)** (Lokale Kassenbon-Texterkennung für Gesamtbetrag, Datum & Händler-Kategorie)
12. **Daten-Tresor (1-Tap JSON-Backup/Restore & Bank-CSV-Import)** (Volle lokale Datenhoheit über das Android Storage Access Framework)
13. **Demo-Daten-Generator, App-Shortcuts & Lite/Pro-Simulator** (6 Monate Demo-Daten mit Abbuchungstagen am 1., 5., 15., 20. und 28., Homescreen-Shortcuts & Live-Testschalter `PRO_FULL` vs. `LITE_FREE`)
