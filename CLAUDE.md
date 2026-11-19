# Projekt-Tagebuch: Fischfang Tracker

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
| `icons/` | App-Symbole (blauer Hintergrund, oranger Fisch) |
| `werkzeuge/symbole_erzeugen.py` | Hilfsprogramm, das die Symbole neu zeichnet |
| `test/pruefung.py` | Automatische Prüfung |
| `CLAUDE.md` | Dieses Tagebuch |

- **Aussehen:** Tailwind (fertige Design-Bausteine, aus dem Internet geladen)
- **Karte:** Leaflet mit OpenStreetMap-Kartenbildern
- **Symbole:** Lucide
- **Sprache:** Spracherkennung des Browsers (funktioniert in Chrome)
- **Speicherort der Daten:** nur im Browser auf dem Handy (`localStorage`, also ein
  kleiner Speicher im Browser). Es gibt keinen Server. Wer den Browser-Speicher löscht,
  verliert die Fänge – daher die Sicherung als Datei (Export/Import).

### Bereiche der App
1. Startbildschirm mit Bild und Start-Knopf
2. Karte mit allen Fängen, Knopf „Standort zentrieren“
3. Liste aller Fänge (bearbeiten, löschen, auf Karte springen)
4. Statistik: Anzahl Fänge, größter Fisch, häufigste Arten (Balkendiagramm)
5. Roter Mikrofon-Knopf: Fang per Sprache eintragen
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
- **Installierbare Web-App (PWA)** statt „richtiger“ App aus dem Play Store: Sie bekommt ein
  eigenes Symbol, öffnet ohne Browser-Leiste und startet offline. Kein PC, keine Gebühr,
  Updates kommen automatisch über GitHub Pages.
- Wenn sich Dateien der App ändern, in `sw.js` die Versionsnummer (`fischfang-v1`) erhöhen,
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

## 4. Offene Ideen / nächste Schritte

- Fotos zu einem Fang speichern
- Wetter und Uhrzeit automatisch mitspeichern
- Filter in der Liste (z. B. nach Fischart oder Monat)
- Kartenbilder für offline speichern (derzeit braucht die Karte Internet)

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
- **`python3 werkzeuge/symbole_erzeugen.py`** – zeichnet die Symbole in `icons/` neu
  (braucht das Python-Paket Pillow). Farben/Form stehen oben in der Datei.
- Vorschau-Bilder: Claude kann die App in der Sitzung mit Playwright im Handy-Format
  fotografieren; die Design-Bausteine aus dem Internet sind dort gesperrt und werden
  dafür lokal nachgebaut (nur für die Vorschau, nicht im Projekt).

### Tests für den Nutzer (kann Claude nicht selbst prüfen)
- App auf dem Handy in Chrome öffnen und einen Fang per Sprache eintragen.
- Standort zentrieren auf dem Handy ausprobieren (braucht GPS).
- [ ] App in Chrome öffnen → Menü (⋮) → „App installieren“ bzw. „Zum Startbildschirm hinzufügen“.
- [ ] Auf dem Startbildschirm erscheint das Symbol (blauer Hintergrund, oranger Fisch).
- [ ] App über das Symbol öffnen: keine Adressleiste, Statusleiste oben blau.
- [ ] Farben: Blau/Orange überall, Mikrofon-Knopf orange.
- [ ] Flugmodus an → App öffnen: Sie startet, Liste und Statistik gehen (Karte bleibt grau).

## 8. Regel zum Tagebuch

Nach jeder größeren Änderung dieses Tagebuch aktualisieren (vor allem Verlauf und offene Ideen).
