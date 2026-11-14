# 📋 PFLICHT-REGELN FÜR ZUKÜNFTIGE KI- & ENTWICKLER-UPDATES (`AI_UPDATE_RULES.md`)

> **WICHTIGE ARCHITEKTUR-DIREKTIVE FÜR ALLE ZUKÜNFTIGEN UPDATES:**
> **"Jedes Mal, wenn in einem zukünftigen Update eine neue Funktion hinzugefügt oder geändert wird, MUSS zwingend gleichzeitig ein neuer `HelpEntry` sowie ein neuer `ChangelogEntry` in `AppHelpRegistry.kt` ergänzt werden!"**

---

## 1. Mitwachsende Hilfe- & Changelog-Zentrale (`AppHelpRegistry.kt`)
Die Datei `app/src/main/java/de/smartfinance/app/core/AppHelpRegistry.kt` ist die **Single Source of Truth** für die In-App-Dokumentation, die kontextsensitiven `?`-Hilfe-Dialoge, den `💎 Lite vs. Pro`-Feature-Vergleich sowie das automatische Update-Banner auf dem Cockpit-Startbildschirm.

Bei **jeder** Code-Änderung oder Funktionserweiterung gelten folgende Pflichtschritte:
1. **Hilfe-Eintrag aktualisieren oder ergänzen (`helpEntries`):**
   - Neue Features erhalten einen eigenen `HelpEntry` mit eindeutiger `id`, deutschem `title`, `category`, `shortSummary`, `stepByStepGuide` (Schritt-für-Schritt-Anleitung), `exampleTips` (Praxis-Beispiele), `requiredTier` (`AppTier.LITE_FREE` oder `AppTier.PRO_FULL`) sowie `addedInVersion`.
   - Bestehende Features, deren Verhalten erweitert wird, müssen in ihrem bestehenden `HelpEntry` aktualisiert werden.
2. **Changelog fortschreiben (`changelogEntries`):**
   - Für jedes Funktions-Update wird ganz oben in `changelogEntries` ein neuer `ChangelogEntry` mit Versionsnummer, Build-Nummer, Datum und einer prägnanten deutschen Liste aller Neuerungen (`newFeatures`) eingetragen.

---

## 2. Nahtlose In-Place-Update-Garantie (Kein Datenverlust!)
Damit jede über GitHub Actions gebaute APK direkt über eine bestehende Installation auf dem Smartphone installiert werden kann:
1. **Unveränderliche Application-ID:** `applicationId = "de.smartfinance.app"` darf niemals geändert werden.
2. **Fester Signatur-Keystore:** `app/keystore/smartfinance.jks` muss im Repository verbleiben und sowohl für `debug` als auch `release` identisch genutzt werden.
3. **Automatische Inkrementierung:** `versionCode = 100 + runNumber` und `versionName = "1.0.$runNumber"` steigen mit jedem GitHub-Actions-Lauf automatisch an.
4. **Strikter Room-Datenschutz:**
   - Der Aufruf einer destruktiven Datenbank-Migration ist **strikt verboten**.
   - Bei Schema-Erweiterungen in `SmartFinanceDatabase.kt` muss immer eine explizite `Migration(startVersion, endVersion)` mit `ALTER TABLE` oder `CREATE TABLE` geschrieben werden, sodass bestehende Nutzerdaten zu 100 % erhalten bleiben.

---

## 3. Lite vs. Pro Feature-Gate (`FeatureGate.kt`)
- Neue Premium-Funktionen müssen über `FeatureGate.kt` geprüft werden.
- Der Standardwert in `FeatureGate` bleibt für die Entwickler-Vollversion auf `AppTier.PRO_FULL`.
- Im Testmodus (`AppTier.LITE_FREE`) müssen Pro-Funktionen mit dem `🔒 PRO`-Badge gekennzeichnet sein und beim Antippen den Info-/Freischalt-Dialog öffnen.
