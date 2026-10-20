/**
 * GestãoNexus — Service Worker
 * Estratégia: Network First para API, Cache First para assets estáticos
 */

const CACHE_NAME    = 'estoque-v7';
const CACHE_STATIC  = 'estoque-static-v7';

// A loja pública não passa pelo cache: quem abriu o sistema no mesmo navegador
// via a versão antiga da loja depois de cada publicação.
const LOJA_SEM_CACHE = /^\/($|loja\.html|css\/(loja|efeitos)\.css|js\/(loja|efeitos)\.js|img\/|uploads\/)/;

// Assets que ficam em cache para funcionar offline
const ASSETS_STATIC = [
  '/index.html',
  '/login.html',
  '/admin.html',
  '/css/style.css',
  '/js/script.js',
  '/manifest.json',
  '/icons/icon-192x192.svg',
  '/icons/icon-512x512.svg',
  'https://fonts.googleapis.com/css2?family=DM+Sans:wght@300;400;500;600&family=DM+Mono:wght@400;500&display=swap'
];

// ── INSTALL: pré-cacheia os assets estáticos ──────────────────────────────────
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_STATIC)
      .then(cache => cache.addAll(ASSETS_STATIC))
      .then(() => self.skipWaiting())
  );
});

// ── ACTIVATE: remove caches antigos ──────────────────────────────────────────
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys
          .filter(k => k !== CACHE_NAME && k !== CACHE_STATIC)
          .map(k => caches.delete(k))
      )
    ).then(() => self.clients.claim())
  );
});

// ── FETCH: estratégia híbrida ─────────────────────────────────────────────────
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  if (url.origin === self.location.origin && LOJA_SEM_CACHE.test(url.pathname)) return;

  // API calls: Network First (sempre tenta a rede, sem cache)
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request).catch(() =>
        new Response(JSON.stringify({ erro: 'Sem conexão com o servidor.' }), {
          status: 503,
          headers: { 'Content-Type': 'application/json' }
        })
      )
    );
    return;
  }

  // Assets estáticos: Cache First (carrega do cache, atualiza em background)
  event.respondWith(
    caches.match(event.request).then(cached => {
      const networkFetch = fetch(event.request).then(response => {
        if (response && response.status === 200 && response.type === 'basic') {
          const clone = response.clone();
          caches.open(CACHE_STATIC).then(cache => cache.put(event.request, clone));
        }
        return response;
      });
      return cached || networkFetch;
    })
  );
});
