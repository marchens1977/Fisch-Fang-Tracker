"""Zeichnet das App-Symbol der Android-App als Vektorgrafik:
android/app/src/main/res/drawable/ic_launcher_foreground.xml
Start: python3 werkzeuge/android_symbol_erzeugen.py
Gleiches Motiv wie werkzeuge/symbole_erzeugen.py (Web-App). Optional: Vorschau-SVG als Argument,
z. B.  python3 werkzeuge/android_symbol_erzeugen.py /tmp/vorschau.svg"""
import sys
from pathlib import Path

ZIEL = Path(__file__).resolve().parent.parent / "android/app/src/main/res/drawable/ic_launcher_foreground.xml"
O, W, B = '#FF5910', '#FFFFFF', '#002D72'


def m(v):  # 0..100 (Motiv) -> Mitte des Android-Bereichs 108x108
    return round(18 + v * 0.72, 2)


def ell(cx, cy, rx, ry):
    cx, cy, rx, ry = m(cx), m(cy), round(rx * 0.72, 2), round(ry * 0.72, 2)
    return f"M{cx-rx:.2f},{cy:.2f} a{rx},{ry} 0 1,0 {2*rx:.2f},0 a{rx},{ry} 0 1,0 {-2*rx:.2f},0 Z"


def poly(pts):
    return "M" + " L".join(f"{m(x)},{m(y)}" for x, y in pts) + " Z"


def main():
    paths = [
        ('fill', O, ell(46, 46, 24, 16), 'Körper'),
        ('fill', O, poly([(64, 46), (82, 30), (78, 46), (82, 62)]), 'Schwanzflosse'),
        ('fill', O, poly([(38, 32), (50, 22), (56, 33)]), 'Rückenflosse'),
        ('fill', W, ell(33.5, 42.5, 3.5, 3.5), 'Auge'),
        ('fill', B, ell(33.75, 42.75, 1.75, 1.75), 'Pupille'),
        ('stroke', B, f"M{m(45)},{m(37.34)} Q{m(51)},{m(46)} {m(45)},{m(54.66)}", 'Kiemen'),
    ]
    for y in (70, 78):
        for x in range(18, 82, 16):
            paths.append(('stroke', W, f"M{m(x+0.48)},{m(y-1.37)} Q{m(x+8)},{m(y-6.6)} {m(x+15.52)},{m(y-1.37)}", 'Welle'))
    xml = ['<?xml version="1.0" encoding="utf-8"?>',
           '<!-- Fisch in Mets-Orange mit weißen Wellen (Bereich 108x108, sichtbar ist die Mitte).',
           '     Erzeugt von werkzeuge/android_symbol_erzeugen.py -->',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    android:width="108dp" android:height="108dp"',
           '    android:viewportWidth="108" android:viewportHeight="108">']
    svg = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="432" height="432">'
           '<rect width="108" height="108" fill="#002D72"/>']
    for art, farbe, d, name in paths:
        if art == 'fill':
            xml.append(f'    <!-- {name} -->\n    <path android:fillColor="{farbe}" android:pathData="{d}" />')
            svg.append(f'<path fill="{farbe}" d="{d}"/>')
        else:
            w = 1.73 if name == 'Welle' else 1.44
            xml.append(f'    <!-- {name} -->\n    <path android:strokeColor="{farbe}" android:strokeWidth="{w}" '
                       f'android:strokeLineCap="round" android:pathData="{d}" />')
            svg.append(f'<path fill="none" stroke="{farbe}" stroke-width="{w}" stroke-linecap="round" d="{d}"/>')
    xml.append('</vector>')
    svg.append('<circle cx="54" cy="54" r="33" fill="none" stroke="#fff" stroke-width="0.3" stroke-dasharray="1 1"/></svg>')
    ZIEL.write_text("\n".join(xml) + "\n", encoding="utf-8")
    print("Gespeichert:", ZIEL)
    if len(sys.argv) > 1:
        Path(sys.argv[1]).write_text("".join(svg), encoding="utf-8")
        print("Vorschau:", sys.argv[1])


if __name__ == "__main__":
    main()
