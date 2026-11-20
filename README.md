# AuraFinance AI – 100 % Lokale Android-Finanz-App (Kotlin, Jetpack Compose, Material 3, Room DB)

**AuraFinance AI** ist eine minimalistische, konsequent **local-first** entwickelte Android-Finanz-App im cleanen **Material 3 Surface-Design** (Hell- & Dunkelmodus, Smaragdgrün/Teal-Akzente). Die App arbeitet vollständig ohne Abo, ohne Cloud-Zwang und ohne Demo-Daten-Ballast (**Clean Slate**).

---

## 1. Architektur & Datenschutz (In-Place-Updates)

- **Tech-Stack:** Kotlin, Single-Activity-Architektur (`FragmentActivity`), MVVM (`StateFlow`, Coroutines), Jetpack Compose, Material Design 3, Room Database (`2.6.1`), AndroidX Biometric (`1.1.0`) und Google ML Kit Text Recognition (On-Device OCR).
- **Android-Konfiguration (`app/build.gradle.kts`):**
  - `applicationId`: `de.smartfinance.app`
  - `minSdk`: `29` (Android 10)
  - `targetSdk`: `35` (Android 15)
  - `compileSdk`: `35`
  - Java/Kotlin JVM Target: `17`
- **Deterministischer Signatur-Keystore:** Der statische Keystore (`app/keystore/keystore.b64`, automatisch beim Build nach `app/keystore/smartfinance.jks` dekodiert) wird identisch für `debug` und `release` (V1/V2-Signatur) genutzt. Dadurch lassen sich neue APKs jederzeit nahtlos über bestehende Installationen aktualisieren (**In-Place-Update**).
- **GitHub-Actions-Versionierung (`.github/workflows/android-build.yml`):**
  - `versionName`: `1.1.<GITHUB_RUN_NUMBER>` (lokal Standard `1.1.1`)
  - `versionCode`: zeitstempelbasiert (`(System.currentTimeMillis() / 10000).toInt()`)
  - Automatischer Release-Build (`./gradlew assembleRelease` mit JDK 17 & Gradle `8.9`) und Artefakt-Upload (`SmartFinance-v1.1.<run_number>.apk`).
- **Room-Datenbank & verlustfreie Migrationen (`SmartFinanceDatabase.kt`):**
  - Datenbankdatei: `smartfinance_clean_user.db`
  - Aktuelle Schema-Version: **Version 8**
  - Strikte, explizite und verlustfreie Migrationen (`SAFE_MIGRATION_1_2` bis `SAFE_MIGRATION_7_8`) – **ohne** `fallbackToDestructiveMigration()`, sodass alle Nutzerdaten bei Updates vollständig erhalten bleiben.
- **Clean-Slate-Garantie:** Frische Installationen (sowie der Reset in den Einstellungen) starten komplett leer bei **`0,00 €`** – ohne künstliche Beispieldaten.

---

## 2. Aktuelle Kernfunktionen

- **Übersicht & Cockpit:**
  - Monatswähler mit direktem Sprung zurück auf den aktuellen Monat und übersichtlichem Budget-Cockpit.
  - Festgepinnte Einnahmen immer oben in **Grün**, Ausgaben klar hervorgehoben in **Rot**.
  - Sortier-Menü für Buchungen (**Nach Datum**, **Nach Summe**, **Nach Name**).
  - **Globale Buchungssuche** über alle nicht-archivierten Buchungen sämtlicher Monate & Jahre (Live-Filter nach Titel, Kategorie, Notiz, Datum `TT.MM.JJJJ` oder Betrag, Typ-Filter *Alle / Ausgaben / Einnahmen*, Trefferanzahl & Netto-Summe sowie direktes Bearbeiten per Tap).
  - **Gmail-Style Swipe-Gesten:** Nach links wischen zum Löschen (mit 1-Tap *Rückgängig*), nach rechts wischen zum Archivieren.
- **Jahres-Rückblick:**
  - Monatsweise Ausgabenübersicht (**Januar–Dezember**) mit Jahres-Umschalter, Gesamtausgaben, Monatsdurchschnitt und dynamischen Kategorie-Filtern.
- **Sparbooster:**
  - Beliebig viele individuelle Sparziele inklusive Bankname/Speicherort, visuellem Fortschrittsbalken, Gesamtersparnis-Übersicht und **1-Tap-Einzahlungen**.
- **Ratenzahlungen & Offene Posten:**
  - Strukturierung über eigene **Ordner**, übersichtliche **Monats-Checklisten** zum Abhaken gezahlter Raten, automatische Restlaufzeit-Berechnung sowie **Sondertilgungen** und Komplett-Ablösung.
- **Notizen & Änderungen (Finanz-Logbuch):**
  - Dokumentation wichtiger finanzieller Änderungen ab Stichtag mit flexiblem Zeitraum (**Von – Bis** bzw. offenem Ende `-`), optionalem **Alt → Neu**-Betragsvergleich inklusive automatischer Differenz-Berechnung sowie Live-Suche und Kategorie-Filter.
- **Archiv (Zwischenspeicher):**
  - Ausgeblendete Buchungen werden getrennt vom aktiven Monatsbudget verwahrt und lassen sich jederzeit wiederherstellen oder endgültig löschen.
- **Hybrider KI-Coach:**
  - Arbeitet zu **100 % lokal** direkt auf deinen erfassten Finanzdaten.
  - Optional erweiterbar per **BYOK** (Bring Your Own Key) mit einem eigenen kostenlosen Google Gemini API-Key im lokalen API-Key-Tresor.
- **Beleg- & PDF-Scanner:**
  - Unterstützt Kamera, Galerie und **PDF-Dokument-Upload**.
  - Optimierte lokale **Offline-OCR** (Google ML Kit) für deutsche Kassenbons und Rechnungen (inkl. Erkennung von Endsumme, Datum & Händler-Kategorie) sowie optional Gemini Vision.
- **Sicherheit & Daten-Tresor:**
  - Optionale native **App-Sperre** per Fingerabdruck, Gesichtserkennung, PIN oder Muster über Android `BiometricPrompt` (beim Fresh-Install deaktiviert, konfigurierbare Sperr-Verzögerung: *Sofort*, *30 Sek.*, *1 Min.*).
  - **1-Tap JSON-Vollbackup & Wiederherstellung** über das Android Storage Access Framework (SAF) sowie **Bank-CSV-Import**.
  - Dezente, Clean-Slate-sichere **30-Tage-Backup-Erinnerung** auf der Übersicht (inkl. 7-Tage-Snooze, Statusanzeige des letzten Backups im Daten-Tresor und Ein-/Ausschalter in den Einstellungen).

---

## 3. Navigation (Kurzüberblick)

- **Bottom-Bar (3 Tabs):**
  - `Übersicht`
  - `KI-Coach`
  - `Sparbooster`
- **Hamburger-Menü (Drawer):**
  - `Übersicht & Cockpit`
  - `Jahres-Rückblick`
  - `Ratenzahlungen & Offene Posten`
  - `Notizen & Änderungen`
  - `Archiv (Zwischenspeicher)`
  - `Daten-Tresor (Backup & Import)`
  - `Einstellungen`
  - `Erscheinungsbild (Hell/Dunkel)`
  - `Hilfe, Features & Was ist neu?`
