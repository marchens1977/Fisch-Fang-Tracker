// Service Worker: sorgt dafür, dass die App auch ohne Internet startet.
// Bei Änderungen an der App die Versionsnummer erhöhen.
const VERSION = 'fischfang-v9';
const APP_DATEIEN = [
  './',
  './index.html',
  './manifest.json',
  './Startbild.jpg',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/icon-maskable-512.png',
  './icons/apple-touch-icon.png',
  './icons/favicon.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(VERSION).then((c) => c.addAll(APP_DATEIEN)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);

  // Kartenbilder nicht speichern (zu viele) – die brauchen Internet.
  if (url.hostname.endsWith('tile.openstreetmap.org')) return;

  // Eigene Seite: zuerst aus dem Internet (damit Updates ankommen), sonst gespeicherte Version.
  if (url.origin === self.location.origin) {
    event.respondWith(
      fetch(req).then((res) => {
        if (res.ok) {
          const kopie = res.clone();
          caches.open(VERSION).then((c) => c.put(req, kopie));
        }
        return res;
      }).catch(() => caches.match(req, { ignoreSearch: true }).then((r) => r || caches.match('./index.html')))
    );
    return;
  }

  // Fremde Bausteine (Design, Karte, Symbole): gespeicherte Version nutzen, sonst laden und merken.
  event.respondWith(
    caches.match(req).then((gespeichert) => gespeichert || fetch(req).then((res) => {
      if (res.ok || res.type === 'opaque') {
        const kopie = res.clone();
        caches.open(VERSION).then((c) => c.put(req, kopie));
      }
      return res;
    }))
  );
});
