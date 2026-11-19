# Projekt-Tagebuch: Fisch-Fang-Tracker

## 1. Worum es geht

Eine Handy-App (Web-App im Browser), mit der der Nutzer seine Fischfänge festhält:
Fangort auf der Karte, Fischart, Größe, Notizen – per Hand oder per Sprache.

- **Online:** https://marchens1977.github.io/Fisch-Fang-Tracker/ (GitHub Pages, vom Nutzer am 16.11.2026 bestätigt).
  Änderungen in `main` erscheinen dort nach wenigen Minuten automatisch.
- **Code:** GitHub-Projekt `marchens1977/Fisch-Fang-Tracker`, Hauptversion = Branch `main`.

### Technischer Aufbau

| Datei | Inhalt |
|---|---|
| `index.html` | Die ganze App in einer Datei (Aussehen, Text und Programmcode) |
| `Startbild.jpg` | Bild auf dem Startbildschirm |
| `manifest.json` | „Steckbrief“ der App: Name, Symbol, Farben – damit man sie installieren kann |
| `sw.js` | Offline-Helfer (Service Worker): speichert die App im Handy, damit sie ohne Internet startet |
| `icons/` | App-Symbole: weiße Angelrute mit gefangenem orangen Fisch auf Mets-Blau |
| `werkzeuge/symbol_erzeugen.js` | Zeichnet das Symbol und erzeugt daraus alle Symbol-Dateien (Web + Android) |
| `test/pruefung.py` | Automatische Prüfung |
| `test/app_simulation.js` | Spielt die Android-App im Browser nach (Teil der Prüfung) |
| `android/` | Eigene **Android-App** (siehe „Android-App“ unten) |
| `.github/workflows/android-app.yml` | Baut die App-Datei (APK) automatisch bei GitHub |
| `CLAUDE.md` | Dieses Tagebuch |

- **Aussehen:** Tailwind (fertige Design-Bausteine, aus dem Internet geladen)
- **Karte:** Leaflet mit OpenStreetMap-Kartenbildern
- **Symbole:** Lucide
- **Sprache:** Spracherkennung des Browsers (funktioniert in Chrome)
- **Speicherort der Daten:** nur im Browser auf dem Handy (`localStorage`, also ein
  kleiner Speicher im Browser). Es gibt keinen Server. Wer den Browser-Speicher löscht,
  verliert die Fänge – daher die Sicherung als Datei (Export/Import).

### Android-App (Ordner `android/`) – gebaut wie die Fitness-App des Nutzers

- Kleine App (Java, ohne Zusatz-Bibliotheken), die die Online-Seite anzeigt
  (`MainActivity.java`). Paketname `de.fischfang.app`, Name auf dem Handy „Fisch-Fang-Tracker“.
- Was die App zusätzlich zur Webseite macht:
  - **GPS:** fragt die Standort-Erlaubnis von Android ab und gibt den Standort an die Seite.
  - **Sprache = Dauer-Diktat** (ab App 1.0.10, übernommen aus der App „Smartphone-Audio-Aufnahme“
    des Nutzers): Die Seite ruft `AndroidApp.startDictation()` auf und zeigt ihr Fenster
    „Zuhören aktiv“. Die App nutzt `SpeechRecognizer` (de-DE) und startet nach **jeder Sprechpause
    sofort neu**; der Text wird gesammelt und laufend per `FischNativeApp.onDictation(text)`
    angezeigt. Erst „Aufnahme beenden & Speichern“ → `AndroidApp.stopDictation()` → die App
    schickt den ganzen Text an `FischNativeApp.onSpeech(text)` → gleiche Auswertung wie bisher.
    Fehler (z. B. kein Internet) → `FischNativeApp.onDictationStatus(meldung)`.
    **Samsung-Besonderheit:** Die Erkennung fängt nach einer Sprechpause oft *innerhalb derselben
    Runde* wieder bei null an, und das Endergebnis enthält nur den letzten Abschnitt. Die Regeln in
    `DictationMerge.java` erkennen diesen Neubeginn und sichern das vorher Gesagte (ohne Doppelungen).
  - **Leisten:** Ab Android 15 zeichnet die App bis unter Status- und Navigationsleiste. Die
    Seite bekommt deshalb in `MainActivity` (`applyBarInsets`) Abstand zu den Leisten und zur Tastatur.
    Braucht die Mikrofon-Erlaubnis (`RECORD_AUDIO`, Android fragt beim ersten Mal).
    Ältere App-Versionen haben nur `startSpeech()` (einfaches Google-Sprachfenster, stoppt bei
    Pausen) – die Seite nutzt das automatisch, solange die App nicht aktualisiert ist.
  - **Sicherung:** `window.AndroidApp.saveFile(name, inhalt)` → Speicherort wählen. Erst wenn
    gespeichert ist, ruft die App `FischNativeApp.onBackupSaved()` („zuletzt gesichert“).
  - **Sicherung laden:** Dateiauswahl des Handys.
  - **Ohne Internet:** zeigt die gespeicherte Kopie der Seite.
  - **Updates von selbst** (`UpdateChecker.java`): Beim Start (höchstens 1x pro Stunde) fragt
    die App bei GitHub die neueste Version ab (Name `app-v1.0.N`, N = Bau-Nummer = `versionCode`).
    Ist sie neuer, erscheint sofort ein Fenster „Update verfügbar“ → „Jetzt herunterladen“ lädt
    die APK im Browser; der Nutzer tippt auf die Datei → „Aktualisieren“. Zusätzlich steht in der
    Statistik (nur in der App) die App-Version mit Knopf „Nach Update suchen“.
  - **Wichtig – keine Selbst-Installation:** Version 1.0.6 konnte sich selbst installieren
    (Erlaubnis `REQUEST_INSTALL_PACKAGES`). Google Play Protect hat sie deshalb als
    „Schädliche App“ **blockiert** (kein „Trotzdem installieren“). Diese Erlaubnis darf in
    selbst gebauten Apps des Nutzers **nicht** verwendet werden. Die Prüfung achtet darauf.
    Für andere Apps wiederverwendbar: in `UpdateChecker.java` nur `REPO` und `APK_NAME` ändern.
- Download der fertigen App:
  https://github.com/marchens1977/Fisch-Fang-Tracker/releases/latest/download/FischfangTracker.apk
  (wird bei jeder Änderung an `android/` auf `main` neu gebaut).
- Änderungen nur an `index.html` brauchen **keine neue APK** – die App lädt die Online-Seite.
- Signatur-Schlüssel `android/app/app-signing.keystore` liegt bewusst im Projekt (Passwort in
  `app/build.gradle`), wie bei der Fitness-App, damit Updates ohne Neuinstallation klappen.
  Nicht ändern! Sonst muss die App deinstalliert werden und die Fänge darin sind weg.
- Die Fänge in der App und in Chrome sind **getrennt gespeichert**. Umzug: in Chrome
  „Sichern“, in der App „Hinzufügen“.

### Bereiche der App
1. Startbildschirm mit Bild und Start-Knopf
2. Karte mit allen Fängen, Knopf „Standort zentrieren“
3. Liste aller Fänge (bearbeiten, löschen, auf Karte springen)
4. Statistik: Anzahl Fänge, größter Fisch, häufigste Arten (Balkendiagramm)
5. Oranger Mikrofon-Knopf: Fang per Sprache eintragen
6. „+ Fang“: Fang von Hand eintragen
7. Sicherung: Daten als Datei speichern und wieder laden

## 2. Wichtige Regeln und Entscheidungen

- Alles bleibt in **einer Datei** (`index.html`), damit der Nutzer sie leicht hochladen kann.
- **Import löscht nie etwas:** Beim Laden einer Sicherung werden nur neue Fänge
  hinzugefügt, mit Rückfrage vorher.
- Die App zeigt an, **wann zuletzt gesichert** wurde, und warnt, wenn das lange her ist.
- Eingaben (Namen, Notizen) werden **sicher angezeigt**, damit kein eingeschleuster Code läuft.
- Zahleneingaben (z. B. Größe) werden geprüft.
- Design: **New-York-Mets-Farben** – Blau `#002D72` (Flächen), Orange `#FF5910` (Knöpfe,
  Hervorhebungen), sehr dunkles Blau `#001433` als Hintergrund. Navigation unten, großer
  **oranger** Mikrofon-Knopf in der Mitte. Löschen bleibt rot (Warnfarbe).
- **Zwei Wege, ein Code:** Die Seite bleibt als installierbare Web-App (PWA) online, und
  zusätzlich gibt es eine **richtige Android-App** (Wunsch des Nutzers, 19.11.2026), gebaut wie
  seine Fitness-App. Kein Play Store – die APK wird direkt von GitHub geladen.
- Wenn sich Dateien der App ändern, in `sw.js` die Versionsnummer (`fischfang-v8`) erhöhen,
  damit alte gespeicherte Versionen sicher ersetzt werden.

## 3. Verlauf

| Datum | Was | PR |
|---|---|---|
| 28.10.2026 | Erste Version der App hochgeladen (vom Nutzer, über GitHub) | – |
| 12.11.2026 | Weitere Versionen und Startbild hochgeladen (vom Nutzer) | – |
| 15.11.2026 | Import sicher gemacht, Sicherungs-Hinweis, sichere Anzeige, neues Design | – (direkt in `main`) |
| 16.11.2026 | Dieses Tagebuch (`CLAUDE.md`) angelegt | #1 |
| 16.11.2026 | Online-Adresse bestätigt und eingetragen | #2 |
| 18.11.2026 | Regel „Hilfsprogramme und Tests gehören ins Projekt“ eingetragen | #3 |
| 19.11.2026 | Installierbar (eigenes Symbol, offline), Mets-Farben, erste automatische Prüfung | #4 |
| 19.11.2026 | Richtige Android-App (Ordner `android/`), automatischer APK-Bau, App-Simulation im Test | #5 |
| 19.11.2026 | Neues Symbol: Angelrute mit gefangenem Fisch (Wunsch des Nutzers, alter Fisch gefiel nicht) | #6 |
| 19.11.2026 | App sucht selbst nach Updates und bietet sie sofort an (Wunsch des Nutzers) | #7 |
| 19.11.2026 | Play Protect blockierte 1.0.6 → Selbst-Installation entfernt, Download über Browser; Name „Fischfang Tracker“ | #8 |
| 19.11.2026 | Nutzer hat 1.0.8 installiert. Dauer-Diktat: Sprachaufnahme stoppt nicht mehr bei Pausen, nur bei „Beenden“ | #9 |
| 19.11.2026 | Diktat verlor nach Pausen den Anfang (Samsung beginnt intern neu) → wird jetzt gesichert. Start-Knopf war unter der Navigationsleiste (Android 15) → Abstand zu den Leisten | #10 |
| 19.11.2026 | Name überall „Fisch-Fang-Tracker“ (unter dem Symbol, Titel, Kopfzeile) – Wunsch des Nutzers | #11 |

## 4. Offene Ideen / nächste Schritte

- Fotos zu einem Fang speichern
- Wetter und Uhrzeit automatisch mitspeichern
- Filter in der Liste (z. B. nach Fischart oder Monat)
- Kartenbilder für offline speichern (derzeit braucht die Karte Internet)
- Android-App: Startbildschirm-Widget (z. B. „letzter Fang“), wie bei der Fitness-App
- Update-Funktion auch in die Fitness-App (und weitere APK-Apps des Nutzers) übernehmen –
  OHNE `REQUEST_INSTALL_PACKAGES` (siehe oben, Play Protect)

## 5. Zusammenarbeit mit dem Nutzer

- Der Nutzer ist **Programmier-Laie**. Immer **einfaches Deutsch**, Fachbegriffe kurz erklären.
- **Kleine, nummerierte Schritte.** Klar sagen, was Claude übernimmt und was der Nutzer
  selbst tun muss.
- Claude übernimmt **alles, was möglich ist** – auch Pull Request (Änderungsvorschlag
  auf GitHub) anlegen und in `main` übernehmen (mergen).
- **Hardware des Nutzers:** nur ein Samsung-Smartphone (Chrome) und ein Raspberry Pi 400.
  Anleitungen müssen damit machbar sein.
- Nichts Persönliches oder Geheimes in dieses Tagebuch schreiben
  (keine E-Mail-Adressen, Passwörter, Schlüssel).

## 6. Regel zum Modell

Wenn du nicht auf Opus läufst (z. B. Fable) und eine Aufgabe abgeschlossen ist, erinnere
den Nutzer am Ende der Antwort:
„Tipp: Du kannst jetzt mit dem Befehl /model wieder auf Opus zurückstellen.“

## 7. Regel: Hilfsprogramme und Tests gehören ins Projekt

1. Automatische Prüfungen (Tests) und Hilfsprogramme, die über eine einzelne Aufgabe
   hinaus nützlich sind, werden im Projekt gespeichert (z. B. Ordner `werkzeuge/` oder
   `test/`) – nicht nur im Arbeitsbereich der Sitzung, der beim Archivieren verloren geht.
2. Im Tagebuch steht, wie man sie startet und was sie prüfen.
3. Nach jeder Code-Änderung die vorhandenen Prüfungen laufen lassen; bei neuen Funktionen
   passende Prüfpunkte ergänzen.
4. Gibt es noch keine automatische Prüfung, bei der nächsten Code-Änderung eine einfache anlegen.
5. Was Claude nicht selbst testen kann (z. B. das echte Handy, den Raspberry Pi), im
   Tagebuch als Test für den Nutzer vermerken.

### Vorhandene Prüfungen und Hilfsprogramme
- **`python3 test/pruefung.py`** – prüft App-Steckbrief, Symbole (Größen), Offline-Helfer,
  Verweise in `index.html`, Mets-Farben und ob der Programmcode fehlerfrei lesbar ist.
  Am Ende steht „Alle Prüfungen bestanden.“ oder die Zahl der Fehler.
  Mit Playwright läuft zusätzlich `test/app_simulation.js`: Sie spielt die Android-App nach
  (Mikrofon → Handy-Spracherkennung, „Zander 65 Zentimeter“ wird gespeichert, GPS-Ort,
  Sicherung über Handy-Speicherort). Braucht den Karten-Baustein Leaflet: aus dem Internet,
  oder in Claudes Sitzung lokal per `npm i leaflet@1.9.4` in einem Hilfsordner und
  `NODE_PATH="$NODE_PATH:<Hilfsordner>/node_modules" python3 test/pruefung.py`.
  Fehlt Leaflet, wird die Simulation mit Hinweis übersprungen.
- **`test/DiktatTest.java`** – spielt Abläufe der Spracherkennung nach (z. B. Samsung: Neubeginn
  nach Pause) und prüft, dass kein Wort verloren geht und nichts doppelt ist. Läuft automatisch in
  `pruefung.py` mit, wenn Java installiert ist.
- **Android-App:** wird bei jeder Änderung an `android/` von GitHub gebaut – ein
  erfolgreicher Bau ist die Prüfung, dass der Code übersetzbar ist.
- **`node werkzeuge/symbol_erzeugen.js`** – zeichnet das App-Symbol (Angelrute mit Fisch) und
  erzeugt daraus `icons/symbol.svg`, die PNG-Symbole der Web-App und das Android-Symbol
  (`ic_launcher_foreground.xml`). Braucht Playwright. Motiv, Farben, Drehung des Fisches und
  Größe stehen oben in der Datei – nur dort ändern, nie die erzeugten Dateien von Hand.
- Vorschau-Bilder: Claude kann die App in der Sitzung mit Playwright im Handy-Format
  fotografieren; die Design-Bausteine aus dem Internet sind dort gesperrt und werden
  dafür lokal nachgebaut (nur für die Vorschau, nicht im Projekt).

### Tests für den Nutzer (kann Claude nicht selbst prüfen)
- App auf dem Handy in Chrome öffnen und einen Fang per Sprache eintragen.
- Standort zentrieren auf dem Handy ausprobieren (braucht GPS).
- [ ] App in Chrome öffnen → Menü (⋮) → „App installieren“ bzw. „Zum Startbildschirm hinzufügen“.
- [ ] Auf dem Startbildschirm erscheint das Symbol (Angelrute mit orangem Fisch auf Blau).
- [ ] App über das Symbol öffnen: keine Adressleiste, Statusleiste oben blau.
- [ ] Farben: Blau/Orange überall, Mikrofon-Knopf orange.
- [ ] Flugmodus an → App öffnen: Sie startet, Liste und Statistik gehen (Karte bleibt grau).
- **Android-App:**
- [ ] APK herunterladen und installieren (Play Protect: „App scannen“ reicht).
- [ ] Symbol mit Namen „Fisch-Fang-Tracker“ (Angelrute mit orangem Fisch auf Blau) erscheint in der App-Liste, nichts abgeschnitten.
- [ ] App öffnen: keine Adressleiste; Frage nach Standort-Erlaubnis → erlauben → Standort auf der Karte.
- [ ] Mikrofon-Knopf: Fenster „Zuhören aktiv“, beim ersten Mal Mikrofon erlauben. Mit Pausen sprechen
      (z. B. „Zander … 65 Zentimeter … am Rhein“) → Text erscheint laufend, Aufnahme läuft weiter.
      „Aufnahme beenden & Speichern“ → Fang mit Art, Länge und ganzem Text als Notiz.
- [ ] Nach App-Update (Diktat-Korrektur): Mit langen Pausen sprechen → der Anfang bleibt im Fenster
      stehen und wird beim Speichern komplett übernommen.
- [ ] Startbildschirm: „Karte öffnen & Starten“ ist ganz zu sehen, nicht unter der Navigationsleiste.
      Auch unten die Navigation der App und oben die Kopfzeile sind nicht verdeckt.
- [ ] „Sichern“: Speicherort wählen, Datei wird gespeichert, Hinweis „zuletzt gesichert“ ist aktuell.
- [ ] „Hinzufügen“: Sicherung aus Chrome laden → Fänge erscheinen in der App.
- [ ] Statistik: unten steht „App-Version 1.0.N“ und der Knopf „Nach Update suchen“ → Meldung „neueste Version“.
- [ ] Beim nächsten App-Update: Beim Öffnen erscheint „Update verfügbar“ → „Jetzt herunterladen“ →
      Download im Browser → auf die Datei tippen → „Aktualisieren“ → Fänge sind noch da.

## 8. Regel zum Tagebuch

Nach jeder größeren Änderung dieses Tagebuch aktualisieren (vor allem Verlauf und offene Ideen).
