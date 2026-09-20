# Location Peek 1.0.0

Kleine Android-App zum Setzen eines simulierten Teststandorts über Androids offizielle Entwickleroption **„App für simulierte Standortdaten auswählen“**.

## Funktionsumfang
- Koordinaten manuell eingeben
- Presets für Graz, Wien und Berlin
- Start/Stop
- Foreground-Service aktualisiert GPS + Netzwerk-Testprovider jede Sekunde
- Keine Root-Rechte
- Keine Lime-spezifische Manipulation oder Umgehung von Erkennungsmechanismen

## Build in Android Studio
1. ZIP entpacken.
2. Android Studio öffnen → **Open** → Ordner `LocationPeek` wählen.
3. Falls Android Studio den Gradle Wrapper nicht automatisch verwenden kann, Android Studio schließen und `prepare-project.bat` einmal ausführen. Danach den Projektordner erneut öffnen.
4. Falls Android Studio SDK 35 oder weitere Build-Komponenten nachinstallieren möchte, bestätigen.
5. Nach erfolgreichem Gradle Sync: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
6. Die Debug-APK liegt anschließend unter `app\build\outputs\apk\debug\app-debug.apk`.

## Einrichtung auf dem HONOR Pad
1. APK installieren.
2. **Einstellungen → Entwickleroptionen → App für simulierte Standortdaten auswählen**.
3. **Location Peek** auswählen.
4. Location Peek öffnen.
5. Koordinaten wählen und **Standort simulieren** drücken.
6. Danach die gewünschte Karten-/Standort-App öffnen.
7. Zum Beenden in Location Peek **Simulation stoppen** drücken.

Android kennzeichnet diese Positionen als Mock Location. Andere Apps können dies erkennen oder ignorieren.
