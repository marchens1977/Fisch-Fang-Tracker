"""Erzeugt die App-Symbole (Icons) im Ordner icons/.
Start: python3 werkzeuge/symbole_erzeugen.py   (braucht Pillow)
Design: Mets-Blau als Hintergrund, oranger Fisch, weißes Auge, weiße Wellen."""
from pathlib import Path
from PIL import Image, ImageDraw

BLAU = (0, 45, 114)      # #002D72
ORANGE = (255, 89, 16)   # #FF5910
WEISS = (255, 255, 255)
ZIEL = Path(__file__).resolve().parent.parent / "icons"


def zeichne(groesse, rund=False):
    s = 4 * groesse  # groß zeichnen, dann verkleinern = glatte Kanten
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if rund:
        d.rounded_rectangle([0, 0, s - 1, s - 1], radius=int(s * 0.22), fill=BLAU)
    else:
        d.rectangle([0, 0, s, s], fill=BLAU)
    u = s / 100  # Einheit: 1 % der Bildbreite; Inhalt bleibt im sicheren Bereich (10-90 %)
    # Fischkörper
    d.ellipse([22 * u, 30 * u, 70 * u, 62 * u], fill=ORANGE)
    # Schwanzflosse
    d.polygon([(64 * u, 46 * u), (82 * u, 30 * u), (78 * u, 46 * u), (82 * u, 62 * u)], fill=ORANGE)
    # Rückenflosse
    d.polygon([(38 * u, 32 * u), (50 * u, 22 * u), (56 * u, 33 * u)], fill=ORANGE)
    # Auge
    d.ellipse([30 * u, 39 * u, 37 * u, 46 * u], fill=WEISS)
    d.ellipse([32 * u, 41 * u, 35.5 * u, 44.5 * u], fill=BLAU)
    # Kiemenbogen
    d.arc([36 * u, 36 * u, 48 * u, 56 * u], start=-60, end=60, fill=BLAU, width=int(2 * u))
    # Wellen
    for y in (70, 78):
        for x in range(18, 82, 16):
            d.arc([x * u, (y - 4) * u, (x + 16) * u, (y + 4) * u], start=200, end=340, fill=WEISS, width=int(2.4 * u))
    return img.resize((groesse, groesse), Image.LANCZOS)


def main():
    ZIEL.mkdir(exist_ok=True)
    zeichne(192).save(ZIEL / "icon-192.png")
    zeichne(512).save(ZIEL / "icon-512.png")
    zeichne(180).convert("RGB").save(ZIEL / "apple-touch-icon.png")
    zeichne(64, rund=True).save(ZIEL / "favicon.png")
    print("Symbole gespeichert in", ZIEL)


if __name__ == "__main__":
    main()
