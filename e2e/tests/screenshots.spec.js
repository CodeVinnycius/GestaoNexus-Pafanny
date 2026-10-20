// Screenshots antes/depois: hero, catálogo e "Essência", em mobile, tablet e desktop.
// Uso: SHOTS=antes npx playwright test screenshots --project=chromium
const { test } = require('@playwright/test');
const path = require('path');

const ROTULO = process.env.SHOTS || 'atual';
const TELAS = [
  { nome: 'mobile',  width: 375,  height: 812 },
  { nome: 'tablet',  width: 768,  height: 1024 },
  { nome: 'desktop', width: 1440, height: 900 }
];
const destino = arquivo => path.join(__dirname, '..', 'screenshots', ROTULO, arquivo);

for (const tela of TELAS) {
  test(`screenshots ${tela.nome}`, async ({ page }) => {
    await page.setViewportSize({ width: tela.width, height: tela.height });
    await page.goto('/');
    await page.waitForSelector('.produto-card');
    await page.waitForTimeout(2500); // deixa as animações de entrada terminarem
    await page.screenshot({ path: destino(`${tela.nome}-hero.png`) });
    // O header fixo cobriria o topo das seções nos recortes abaixo.
    await page.addStyleTag({ content: '.header{position:relative!important}' });

    for (const [nome, seletor] of [['catalogo', '#catalogo'], ['essencia', 'section[aria-labelledby="experiencia-title"]']]) {
      const secao = page.locator(seletor);
      await secao.scrollIntoViewIfNeeded();
      // Passa por toda a seção para disparar os reveals de cada parte dela.
      await page.evaluate(async sel => {
        const el = document.querySelector(sel);
        const topo = el.getBoundingClientRect().top + scrollY;
        for (let y = topo; y < topo + el.offsetHeight; y += innerHeight / 2) {
          scrollTo(0, y); await new Promise(r => setTimeout(r, 120));
        }
      }, seletor);
      await page.waitForTimeout(1800);
      await secao.screenshot({ path: destino(`${tela.nome}-${nome}.png`), animations: 'disabled' });
    }
  });
}
