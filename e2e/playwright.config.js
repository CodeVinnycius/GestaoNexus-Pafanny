// Testes da loja PAFANNY — rodam contra o site LOCAL (nunca contra produção).
// Suba o site antes (porta 8081 por padrão) ou defina LOJA_URL.
const { defineConfig, devices } = require('@playwright/test');

const LOJA_URL = process.env.LOJA_URL || 'http://localhost:8081';
if (/pafanny\.com\.br|railway\.app/.test(LOJA_URL)) {
  throw new Error('Os testes rodam só no site local — não aponte LOJA_URL para produção.');
}

module.exports = defineConfig({
  testDir: './tests',
  timeout: 60_000,
  fullyParallel: true,
  reporter: [['list']],
  use: {
    baseURL: LOJA_URL,
    trace: 'retain-on-failure'
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox',  use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit',   use: { ...devices['Desktop Safari'] } },
    { name: 'iphone',   use: { ...devices['iPhone 13'] } },
    { name: 'edge',     use: { ...devices['Desktop Edge'], channel: 'msedge' } }
  ]
});
