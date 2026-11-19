"""Automatische Prüfung für den Fischfang Tracker.
Start (im Projektordner):  python3 test/pruefung.py
Prüft: App-Steckbrief (manifest.json), Symbole, Offline-Helfer (sw.js),
Verweise in index.html, Mets-Farben, und ob der Programmcode fehlerfrei lesbar ist (wenn node da ist)."""
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

print()
if fehler:
    print(f"{len(fehler)} Fehler gefunden.")
    sys.exit(1)
print("Alle Prüfungen bestanden.")
