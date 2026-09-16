// CareerPulse Lightweight Service Worker for Desktop App Installability & Offline Shell
const CACHE_NAME = 'careerpulse-v2.10';
const ASSETS_TO_CACHE = [
    '/',
    '/css/theme.css?v=2.10',
    '/css/app.css?v=2.10',
    '/js/api.js?v=2.10',
    '/js/app.js?v=2.10',
    '/manifest.json'
];

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return cache.addAll(ASSETS_TO_CACHE).catch(() => {
                // Ignore cache errors in offline/dev environments
            });
        })
    );
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) => {
            return Promise.all(
                keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
            );
        })
    );
    self.clients.claim();
});

self.addEventListener('fetch', (event) => {
    // Network first, fallback to cache for static shell
    if (event.request.method === 'GET' && !event.request.url.includes('/api/')) {
        event.respondWith(
            fetch(event.request).catch(() => {
                return caches.match(event.request).then((res) => {
                    return res || caches.match('/');
                });
            })
        );
    }
});
