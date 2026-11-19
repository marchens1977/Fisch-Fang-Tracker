// Zeichnet das App-Symbol (Angelrute mit gefangenem Fisch) und erzeugt daraus:
//   icons/symbol.svg, icons/icon-192.png, icons/icon-512.png, icons/apple-touch-icon.png, icons/favicon.png
//   android/app/src/main/res/drawable/ic_launcher_foreground.xml
// Start (im Projektordner): node werkzeuge/symbol_erzeugen.js     (braucht Playwright mit Chromium)
// Das Motiv steht unten in MOTIV, Koordinaten 0..100. Alles bleibt im Kreis mit Radius 40 um die Mitte,
// damit Android und Chrome es bei runden Symbolen nicht abschneiden.
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

const BLAU = '#002D72', ORANGE = '#FF5910', ORANGE_DUNKEL = '#D9440A', WEISS = '#FFFFFF';

const kreis = (cx, cy, r) => `M${cx - r},${cy} a${r},${r} 0 1,0 ${2 * r},0 a${r},${r} 0 1,0 ${-2 * r},0 Z`;

// Der Fisch wird um sein Maul (66,41) gedreht, als würde er an der Schnur zappeln.
// Ganzes Motiv etwas verkleinern, damit nichts über den Rand ragt.
const MASSSTAB = 0.9;

const FISCH_DREHUNG = { winkel: 12, x: 66, y: 41 };

const MOTIV = [
  // Schnur von der Rutenspitze zum Maul des Fisches
  { stroke: WEISS, breite: 1.1, d: 'M66,21 L66,41.5' },
  // Angelrute (wird zur Spitze hin dünner)
  { fill: WEISS, d: 'M18.07,74.41 L65.38,19.49 L66.62,20.51 L21.93,77.59 Z' },
  // Griff
  { stroke: ORANGE, breite: 6.5, d: 'M21.5,74 L28.5,65.5' },
  // Rolle
  { fill: WEISS, d: kreis(35.5, 65, 4.2) },
  { fill: BLAU, d: kreis(35.5, 65, 1.6) },
  // ---- ab hier: Fisch (gedreht) ----
  // Schwanzflosse
  { fisch: true, fill: ORANGE_DUNKEL, d: 'M66,74 L58,87 Q66,82.5 74,87 Z' },
  // Rückenflosse und Brustflosse
  { fisch: true, fill: ORANGE_DUNKEL, d: 'M74,51 L82,56.5 L74.5,64 Z' },
  { fisch: true, fill: ORANGE_DUNKEL, d: 'M58,56 L52,61 L58.3,62.5 Z' },
  // Körper (hängt mit dem Maul nach oben)
  { fisch: true, fill: ORANGE, d: 'M66,41 C72.5,41 75.5,50 75,59 C74.5,67 70.5,74 66,77 C61.5,74 57.5,67 57,59 C56.5,50 59.5,41 66,41 Z' },
  // Schuppen-Andeutung
  { fisch: true, stroke: ORANGE_DUNKEL, breite: 1.2, d: 'M62,60 L66,63.5 L70,60' },
  { fisch: true, stroke: ORANGE_DUNKEL, breite: 1.2, d: 'M63,66 L66,68.8 L69,66' },
  // Auge
  { fisch: true, fill: WEISS, d: kreis(62.3, 48, 2.6) },
  { fisch: true, fill: BLAU, d: kreis(62, 48.1, 1.25) },
];

function svgTeil(t) {
  return t.fill
    ? `<path fill="${t.fill}" d="${t.d}"/>`
    : `<path fill="none" stroke="${t.stroke}" stroke-width="${t.breite}" stroke-linecap="round" stroke-linejoin="round" d="${t.d}"/>`;
}

function svg(groesse, rund = false) {
  const { winkel, x, y } = FISCH_DREHUNG;
  const teile = [
    ...MOTIV.filter(t => !t.fisch).map(svgTeil),
    `<g transform="rotate(${winkel} ${x} ${y})">`, ...MOTIV.filter(t => t.fisch).map(svgTeil), '</g>'];
  const verschiebung = 50 * (1 - MASSSTAB);
  const hg = rund ? `<rect width="100" height="100" rx="22" fill="${BLAU}"/>` : `<rect width="100" height="100" fill="${BLAU}"/>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="${groesse}" height="${groesse}">${hg}<g transform="translate(${verschiebung} ${verschiebung}) scale(${MASSSTAB})">${teile.join('')}</g></svg>`;
}

function androidTeil(t, einzug) {
  return einzug + (t.fill
    ? `<path android:fillColor="${t.fill}" android:pathData="${t.d}" />`
    : `<path android:strokeColor="${t.stroke}" android:strokeWidth="${t.breite}" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="${t.d}" />`);
}

function androidXml() {
  const { winkel, x, y } = FISCH_DREHUNG;
  const teile = [
    ...MOTIV.filter(t => !t.fisch).map(t => androidTeil(t, '        ')),
    `        <group android:rotation="${winkel}" android:pivotX="${x}" android:pivotY="${y}">`,
    ...MOTIV.filter(t => t.fisch).map(t => androidTeil(t, '            ')),
    '        </group>'];
  return `<?xml version="1.0" encoding="utf-8"?>
<!-- Angelrute mit gefangenem Fisch (Mets-Farben). Erzeugt von werkzeuge/symbol_erzeugen.js – nicht von Hand ändern.
     Motiv 0..100 wird in die sichtbare Mitte des Android-Bereichs 108x108 gesetzt. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:translateX="${(18 + 0.72 * 50 * (1 - MASSSTAB)).toFixed(3)}" android:translateY="${(18 + 0.72 * 50 * (1 - MASSSTAB)).toFixed(3)}" android:scaleX="${(0.72 * MASSSTAB).toFixed(4)}" android:scaleY="${(0.72 * MASSSTAB).toFixed(4)}">
${teile.join('\n')}
    </group>
</vector>
`;
}

(async () => {
  const { chromium } = require('playwright');
  fs.mkdirSync(path.join(ROOT, 'icons'), { recursive: true });
  fs.writeFileSync(path.join(ROOT, 'icons/symbol.svg'), svg(512));
  fs.writeFileSync(path.join(ROOT, 'android/app/src/main/res/drawable/ic_launcher_foreground.xml'), androidXml());

  const browser = await chromium.launch();
  const page = await browser.newPage();
  const ziele = [['icon-192.png', 192, false], ['icon-512.png', 512, false], ['apple-touch-icon.png', 180, false], ['favicon.png', 64, true]];
  for (const [name, g, rund] of ziele) {
    await page.setViewportSize({ width: g, height: g });
    await page.setContent(`<html><body style="margin:0;background:transparent">${svg(g, rund)}</body></html>`);
    await page.screenshot({ path: path.join(ROOT, 'icons', name), omitBackground: true });
  }
  await browser.close();
  console.log('Symbole erzeugt: icons/ und android/.../ic_launcher_foreground.xml');
})();
