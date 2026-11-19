// Simuliert die Android-App im Browser und prüft die Verbindung zur Webseite.
// Start: node test/app_simulation.js   (braucht Playwright mit Chromium)
// Die Design-Bausteine aus dem Internet werden durch Platzhalter ersetzt; die Karte (Leaflet)
// wird aus dem Internet geladen oder – falls gesperrt – aus einem lokalen "leaflet"-Paket (NODE_PATH).
const path = require('path');
const fs = require('fs');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { console.log('HINWEIS  Playwright fehlt – Simulation übersprungen'); process.exit(0); }

function lokal(datei) {
  try { return fs.readFileSync(require.resolve('leaflet/dist/' + datei)); } catch (e) { return null; }
}

(async () => {
  const browser = await chromium.launch();
  const context = await browser.newContext({ permissions: ['geolocation'], geolocation: { latitude: 49.99, longitude: 8.25 } });
  const page = await context.newPage();
  const fehler = [];
  page.on('pageerror', e => fehler.push(e.message));
  page.on('dialog', d => d.accept());

  await page.route('https://cdn.tailwindcss.com/**', r => r.fulfill({ contentType: 'application/javascript', body: 'window.tailwind={};' }));
  await page.route('https://unpkg.com/lucide@latest', r => r.fulfill({ contentType: 'application/javascript', body: 'window.lucide={createIcons(){}};' }));
  await page.route('https://unpkg.com/leaflet@1.9.4/dist/**', async r => {
    const datei = r.request().url().endsWith('.css') ? 'leaflet.css' : 'leaflet.js';
    try { const resp = await r.fetch(); if (resp.ok()) return r.fulfill({ response: resp }); } catch (e) {}
    const body = lokal(datei);
    if (body) return r.fulfill({ contentType: datei.endsWith('.css') ? 'text/css' : 'application/javascript', body });
    return r.abort();
  });
  await page.route('https://*.tile.openstreetmap.org/**', r => r.abort());

  // So sieht die Android-App für die Webseite aus:
  await page.addInitScript(() => {
    localStorage.clear();
    window.__app = { speech: 0, saved: null, updateChecks: 0 };
    window.AndroidApp = {
      startSpeech() { window.__app.speech++; },
      saveFile(name, content) { window.__app.saved = { name, content }; },
      getVersion() { return '1.0.4'; },
      checkUpdate() { window.__app.updateChecks++; },
      isApp() { return true; }
    };
  });

  await page.goto('file://' + path.resolve(__dirname, '..', 'index.html'));
  const hatKarte = await page.evaluate(() => typeof window.L !== 'undefined');
  if (!hatKarte) {
    console.log('HINWEIS  Karten-Baustein (Leaflet) nicht erreichbar – Simulation übersprungen');
    await browser.close(); process.exit(0);
  }
  await page.evaluate(() => startApp());

  let ok = true;
  const pruefe = (bed, text) => { console.log((bed ? 'OK    ' : 'FEHLER') + '  Simulation: ' + text); if (!bed) ok = false; };

  // 1. Mikrofon-Knopf ruft die Spracherkennung des Handys auf
  await page.click('button[title="Sprachfang"]');
  const st = await page.evaluate(() => ({ n: window.__app.speech, overlay: !document.getElementById('voice-overlay').classList.contains('hidden') }));
  pruefe(st.n === 1, 'Mikrofon-Knopf startet die Handy-Spracherkennung');
  pruefe(!st.overlay, 'kein Browser-Sprachfenster in der App');

  // 2. Erkannter Text kommt zurück und wird als Fang gespeichert
  await page.evaluate(() => window.FischNativeApp.onSpeech('Zander 65 Zentimeter'));
  await page.waitForFunction(() => (localStorage.getItem('fish_catches') || '[]') !== '[]', null, { timeout: 10000 }).catch(() => {});
  const fang = await page.evaluate(() => JSON.parse(localStorage.getItem('fish_catches') || '[]'));
  pruefe(fang.length === 1 && fang[0].species === 'Zander' && fang[0].length === 65, 'Sprachfang „Zander 65 Zentimeter“ gespeichert (Art + Länge)');
  pruefe(fang.length === 1 && Math.abs(fang[0].lat - 49.99) < 0.01, 'Fangort kommt vom GPS');

  // 3. Sicherung läuft über den Speicherort des Handys
  await page.evaluate(() => exportData());
  const saved = await page.evaluate(() => window.__app.saved);
  pruefe(saved && /^fischfang_sicherung_\d{4}-\d{2}-\d{2}\.json$/.test(saved.name), 'Sicherung: Dateiname richtig');
  pruefe(saved && (JSON.parse(saved.content)[0] || {}).species === 'Zander', 'Sicherung: Inhalt enthält die Fänge');
  const vorher = await page.evaluate(() => localStorage.getItem('fish_last_export'));
  pruefe(vorher === null, '„Zuletzt gesichert“ erst nach echtem Speichern');
  await page.evaluate(() => window.FischNativeApp.onBackupSaved());
  const nachher = await page.evaluate(() => localStorage.getItem('fish_last_export'));
  pruefe(nachher !== null, '„Zuletzt gesichert“ wird nach dem Speichern gesetzt');

  // 4. Update-Knopf in der Statistik
  await page.click('#nav-stats');
  const box = await page.evaluate(() => ({
    sichtbar: !document.getElementById('app-update-box').classList.contains('hidden'),
    text: document.getElementById('app-version').textContent }));
  pruefe(box.sichtbar && box.text === 'App-Version 1.0.4', 'Statistik zeigt App-Version');
  await page.click('text=Nach Update suchen');
  pruefe(await page.evaluate(() => window.__app.updateChecks) === 1, 'Knopf „Nach Update suchen“ fragt die App');

  pruefe(fehler.length === 0, 'keine Programmfehler auf der Seite' + (fehler.length ? ': ' + fehler.join(' | ') : ''));
  await browser.close();
  process.exit(ok ? 0 : 1);
})();
