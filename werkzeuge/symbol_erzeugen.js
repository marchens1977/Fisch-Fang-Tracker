// Erzeugt das App-Symbol und daraus:
//   icons/icon-192.png, icons/icon-512.png, icons/icon-maskable-512.png, icons/apple-touch-icon.png, icons/favicon.png
//   android/app/src/main/res/drawable/ic_launcher_foreground.xml (+ drawable-nodpi/ic_launcher_bild.png bei QUELLE 'startbild')
//   icons/symbol.svg = die gezeichnete Angelrute (bleibt immer als Vorlage erhalten)
// QUELLE wählt das Motiv:
//   'startbild'  = Ausschnitt aus Startbild.jpg (AUSSCHNITT, in Pixeln des Startbilds) – Wunsch des Nutzers ab 19.11.2026
//   'zeichnung'  = gezeichnete Angelrute mit gefangenem Fisch (MOTIV unten) – das frühere Symbol
// Start (im Projektordner): node werkzeuge/symbol_erzeugen.js     (braucht Playwright mit Chromium)
// Das Motiv steht unten in MOTIV, Koordinaten 0..100. Alles bleibt im Kreis mit Radius 40 um die Mitte,
// damit Android und Chrome es bei runden Symbolen nicht abschneiden.
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

const QUELLE = 'startbild';
// Variante A: Gesicht + Fisch. Quadrat: links/oben-Ecke und Kantenlänge im 768x1024 großen Startbild.
const AUSSCHNITT = { x: 0, y: 180, groesse: 768 };

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

// ---- Symbol aus dem Startbild ----
const bildDatenUrl = () => 'data:image/jpeg;base64,' + fs.readFileSync(path.join(ROOT, 'Startbild.jpg')).toString('base64');

// HTML: Ausschnitt des Startbilds auf einer Fläche "groesse", mit Rand "rand" (Anteil 0..0.5) in Blau.
function bildHtml(groesse, rand = 0, ecken = 0) {
  const innen = groesse * (1 - 2 * rand);
  const f = innen / AUSSCHNITT.groesse;
  return `<html><body style="margin:0;background:transparent">
<div style="width:${groesse}px;height:${groesse}px;background:${rand ? BLAU : 'transparent'};position:relative;overflow:hidden;border-radius:${ecken}%">
<div style="position:absolute;left:${groesse * rand}px;top:${groesse * rand}px;width:${innen}px;height:${innen}px;
background:url(${bildDatenUrl()}) no-repeat;background-size:${768 * f}px ${1024 * f}px;
background-position:${-AUSSCHNITT.x * f}px ${-AUSSCHNITT.y * f}px"></div></div></body></html>`;
}

function androidBitmapXml() {
  return `<?xml version="1.0" encoding="utf-8"?>
<!-- Ausschnitt aus dem Startbild (Variante A). Erzeugt von werkzeuge/symbol_erzeugen.js – nicht von Hand ändern.
     Das Bild drawable-nodpi/ic_launcher_bild.png hat 108x108-Raster: Ausschnitt in der sichtbaren Mitte (72), Rand blau. -->
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/ic_launcher_bild"
    android:filter="true" />
`;
}

async function foto(page, html, groesse, ziel) {
  await page.setViewportSize({ width: groesse, height: groesse });
  await page.setContent(html);
  await page.waitForFunction(() => document.readyState === 'complete');
  await page.screenshot({ path: ziel, omitBackground: true });
}

(async () => {
  const { chromium } = require('playwright');
  fs.mkdirSync(path.join(ROOT, 'icons'), { recursive: true });
  fs.writeFileSync(path.join(ROOT, 'icons/symbol.svg'), svg(512));
  const res = path.join(ROOT, 'android/app/src/main/res');
  const bildPng = path.join(res, 'drawable-nodpi/ic_launcher_bild.png');

  const browser = await chromium.launch();
  const page = await browser.newPage();
  if (QUELLE === 'startbild') {
    const ziele = [['icon-192.png', 192, 0, 0], ['icon-512.png', 512, 0, 0], ['apple-touch-icon.png', 180, 0, 0],
      ['favicon.png', 64, 0, 22],
      // "maskable": Android/Chrome schneidet rund zu – Bild in die sichere Mitte (80 %), Rand blau
      ['icon-maskable-512.png', 512, 0.1, 0]];
    for (const [name, g, rand, ecken] of ziele) await foto(page, bildHtml(g, rand, ecken), g, path.join(ROOT, 'icons', name));
    fs.mkdirSync(path.dirname(bildPng), { recursive: true });
    await foto(page, bildHtml(432, 1 / 6), 432, bildPng);
    fs.writeFileSync(path.join(res, 'drawable/ic_launcher_foreground.xml'), androidBitmapXml());
  } else {
    fs.writeFileSync(path.join(res, 'drawable/ic_launcher_foreground.xml'), androidXml());
    if (fs.existsSync(bildPng)) fs.unlinkSync(bildPng);
    const ziele = [['icon-192.png', 192, false], ['icon-512.png', 512, false], ['icon-maskable-512.png', 512, false],
      ['apple-touch-icon.png', 180, false], ['favicon.png', 64, true]];
    for (const [name, g, rund] of ziele)
      await foto(page, `<html><body style="margin:0;background:transparent">${svg(g, rund)}</body></html>`, g, path.join(ROOT, 'icons', name));
  }
  await browser.close();
  console.log(`Symbole erzeugt (Quelle: ${QUELLE}): icons/ und android/.../res/`);
})();
