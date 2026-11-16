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
- Design: dunkel, Navigation unten, großer roter Mikrofon-Knopf in der Mitte.

## 3. Verlauf

| Datum | Was | PR |
|---|---|---|
| 28.10.2026 | Erste Version der App hochgeladen (vom Nutzer, über GitHub) | – |
| 12.11.2026 | Weitere Versionen und Startbild hochgeladen (vom Nutzer) | – |
| 15.11.2026 | Import sicher gemacht, Sicherungs-Hinweis, sichere Anzeige, neues Design | – (direkt in `main`) |
| 16.11.2026 | Dieses Tagebuch (`CLAUDE.md`) angelegt | #1 |
| 16.11.2026 | Online-Adresse bestätigt und eingetragen | #2 |

## 4. Offene Ideen / nächste Schritte

- Fotos zu einem Fang speichern
- Wetter und Uhrzeit automatisch mitspeichern
- Filter in der Liste (z. B. nach Fischart oder Monat)
- App auf dem Startbildschirm des Handys installierbar machen (funktioniert dann auch ohne Internet)

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

## 7. Regel zum Tagebuch

Nach jeder größeren Änderung dieses Tagebuch aktualisieren (vor allem Verlauf und offene Ideen).
