"""Automatische Prüfung für den Fischfang Tracker.
Start (im Projektordner):  python3 test/pruefung.py
Prüft: App-Steckbrief (manifest.json), Symbole, Offline-Helfer (sw.js),
Verweise in index.html, Mets-Farben, ob der Programmcode fehlerfrei lesbar ist (wenn node da ist),
die Android-App (Ordner android/) und eine Simulation der App-Verbindung (wenn Playwright da ist)."""
import json, re, shutil, struct, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
fehler = []

def pruefe(bed, text):
    print(("OK    " if bed else "FEHLER") + "  " + text)
    if not bed:
        fehler.append(text)

def png_groesse(pfad):
    with open(pfad, "rb") as f:
        kopf = f.read(24)
    if kopf[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", kopf[16:24])

html = (ROOT / "index.html").read_text(encoding="utf-8")

# 1. Manifest
try:
    m = json.loads((ROOT / "manifest.json").read_text(encoding="utf-8"))
    pruefe(True, "manifest.json ist gültig")
except Exception as e:
    m = {}
    pruefe(False, f"manifest.json lesbar ({e})")
for feld in ("name", "short_name", "start_url", "display", "icons", "theme_color"):
    pruefe(feld in m, f"manifest.json hat '{feld}'")
pruefe(m.get("display") == "standalone", "App öffnet ohne Browser-Leiste (standalone)")
pruefe(m.get("start_url") == "./" and m.get("scope") == "./", "Start-Adresse passt zu GitHub Pages (relativ)")

# 2. Symbole
groessen = set()
for icon in m.get("icons", []):
    p = ROOT / icon["src"]
    pruefe(p.exists(), f"Symbol vorhanden: {icon['src']}")
    if p.exists():
        g = png_groesse(p)
        erwartet = tuple(int(x) for x in icon["sizes"].split("x"))
        pruefe(g == erwartet, f"{icon['src']} hat Größe {icon['sizes']} (ist {g})")
        groessen.add(icon["sizes"])
pruefe({"192x192", "512x512"} <= groessen, "Symbole in 192 und 512 Pixel (nötig für Installation in Chrome)")
pruefe((ROOT / "icons/symbol.svg").exists(), "Vorlage des Symbols vorhanden (icons/symbol.svg)")
fg = (ROOT / "android/app/src/main/res/drawable/ic_launcher_foreground.xml").read_text(encoding="utf-8")
pruefe("symbol_erzeugen.js" in fg, "Android-Symbol stammt aus derselben Vorlage wie die Web-Symbole")
pruefe(any("maskable" in i.get("purpose", "") for i in m.get("icons", [])), "Symbol für runde Android-Form (maskable)")

# 3. index.html verweist auf alles
for muster, text in [
    (r'<link rel="manifest" href="manifest.json">', "index.html verweist auf manifest.json"),
    (r'rel="apple-touch-icon"', "index.html hat Symbol für iPhone/Startbildschirm"),
    (r'rel="icon"', "index.html hat Browser-Tab-Symbol"),
    (r"serviceWorker\.register\('sw\.js'\)", "index.html meldet den Offline-Helfer an"),
    (r'<meta name="theme-color" content="#002D72">', "Leistenfarbe ist Mets-Blau"),
]:
    pruefe(re.search(muster, html) is not None, text)
for datei in re.findall(r'(?:href|src)="((?:icons/)[^"]+)"', html):
    pruefe((ROOT / datei).exists(), f"Datei vorhanden: {datei}")

# 4. Offline-Helfer
sw = (ROOT / "sw.js").read_text(encoding="utf-8")
for eintrag in re.findall(r"'\./([^']+)'", sw):
    pruefe((ROOT / eintrag).exists(), f"sw.js: Datei vorhanden: {eintrag}")

# 5. Farben
pruefe("#FF5910" in html and "#002D72" in html, "Mets-Farben (Orange #FF5910, Blau #002D72) sind eingebaut")
for alt in ("#3b82f6", "#0ea5e9", "bg-red-600", "text-blue-400"):
    pruefe(alt not in html, f"alte Farbe entfernt: {alt}")

# 6. Programmcode lesbar (Syntax)
node = shutil.which("node")
if node:
    skripte = re.findall(r"<script>(.*?)</script>", html, re.S)
    for nr, code in enumerate(skripte + [sw]):
        with tempfile.NamedTemporaryFile("w", suffix=".js", delete=False) as f:
            f.write(code)
        r = subprocess.run([node, "--check", f.name], capture_output=True, text=True)
        pruefe(r.returncode == 0, f"Programmcode-Block {nr + 1} fehlerfrei lesbar" + ("" if r.returncode == 0 else ": " + r.stderr[:300]))
else:
    print("HINWEIS  node nicht installiert – Syntax-Prüfung übersprungen")

# 7. Android-App (Ordner android/)
A = ROOT / "android"
for datei in ["build.gradle", "settings.gradle", "gradlew", "gradle/wrapper/gradle-wrapper.jar",
              "gradle/wrapper/gradle-wrapper.properties", "app/build.gradle", "app/app-signing.keystore",
              "app/src/main/AndroidManifest.xml", "app/src/main/java/de/fischfang/app/MainActivity.java",
              "app/src/main/res/drawable/ic_launcher_foreground.xml",
              "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"]:
    pruefe((A / datei).exists(), f"Android: Datei vorhanden: {datei}")
am = (A / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
pruefe("ACCESS_FINE_LOCATION" in am, "Android: GPS-Erlaubnis eingetragen")
pruefe("RECORD_AUDIO" in am and "android.speech.RecognitionService" in am, "Android: Mikrofon-Erlaubnis und Spracherkennung fürs Dauer-Diktat eingetragen")
pruefe("RECOGNIZE_SPEECH" in am, "Android: Spracherkennung auffindbar (queries)")
java = (A / "app/src/main/java/de/fischfang/app/MainActivity.java").read_text(encoding="utf-8")
pruefe("https://marchens1977.github.io/Fisch-Fang-Tracker/" in java, "Android: App lädt die richtige Seite")
for f in ("saveFile", "startSpeech", "startDictation", "stopDictation", "SpeechRecognizer",
          "onGeolocationPermissionsShowPrompt", "onShowFileChooser"):
    pruefe(f in java, f"Android: Funktion vorhanden: {f}")
pruefe("FischNativeApp.onSpeech" in java and "window.FischNativeApp = {" in html,
       "Webseite und App sprechen dieselbe Sprache (FischNativeApp)")
pruefe("window.AndroidApp.startSpeech()" in html and "window.AndroidApp.saveFile(" in html,
       "Webseite nutzt in der App Sprache und Sicherung des Handys")
import xml.dom.minidom
for x in (A / "app/src/main").rglob("*.xml"):
    try:
        xml.dom.minidom.parse(str(x)); ok = True
    except Exception:
        ok = False
    pruefe(ok, f"Android: XML lesbar: {x.relative_to(A)}")
upd = (A / "app/src/main/java/de/fischfang/app/UpdateChecker.java")
pruefe(upd.exists(), "Android: Update-Funktion vorhanden")
if upd.exists():
    u = upd.read_text(encoding="utf-8")
    wf = (ROOT / ".github/workflows/android-app.yml").read_text(encoding="utf-8")
    apk = re.search(r'APK_NAME = "([^"]+)"', u).group(1)
    pruefe(apk in wf, f"Update sucht dieselbe Datei, die GitHub veröffentlicht ({apk})")
    pruefe('TAG="app-v1.0.${GITHUB_RUN_NUMBER}"' in wf and "GITHUB_RUN_NUMBER" in (A / "app/build.gradle").read_text(),
           "Versionsnummer der App und des Downloads passen zusammen")
    pruefe('REPO = "marchens1977/Fisch-Fang-Tracker"' in u, "Update fragt das richtige GitHub-Projekt")
    pruefe("updates.autoCheck()" in java, "App sucht beim Start von selbst nach Updates")
pruefe("REQUEST_INSTALL_PACKAGES" not in am,
       "Android: KEINE Erlaubnis „andere Apps installieren“ (sonst blockiert Google Play Protect die App)")
pruefe('<string name="app_name">Fischfang Tracker</string>' in (A / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8"),
       "Android: Name unter dem Symbol ist „Fischfang Tracker“")
pruefe(m.get("short_name") == "Fischfang Tracker", "Web-App: Name unter dem Symbol ist „Fischfang Tracker“")
pruefe((ROOT / ".github/workflows/android-app.yml").exists(), "Automatischer App-Bau bei GitHub eingerichtet")

# 8. Programm-Ablauf im Browser (wenn Playwright da ist): App-Verbindung simulieren
if node and (ROOT / "test/app_simulation.js").exists():
    r = subprocess.run([node, str(ROOT / "test/app_simulation.js")], capture_output=True, text=True, cwd=ROOT)
    print(r.stdout.strip())
    pruefe(r.returncode == 0, "Simulation der Android-App: Sprache, Sicherung" + ("" if r.returncode == 0 else ": " + r.stderr[-500:]))

print()
if fehler:
    print(f"{len(fehler)} Fehler gefunden.")
    sys.exit(1)
print("Alle Prüfungen bestanden.")
