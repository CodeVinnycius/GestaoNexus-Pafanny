// O fluxo do WhatsApp precisa continuar EXATAMENTE como na versão em produção.
// A mensagem esperada é montada aqui de forma independente do loja.js.
const { test, expect } = require('@playwright/test');

const moeda = v => (+v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

function mensagemEsperada(p, nomeLoja) {
  const linhas = ['Olá! 💛', `Vi este pijama no site da ${nomeLoja} e gostaria de saber se ele está disponível.`, '', `Produto: ${p.nome}`];
  if (p.categoria) linhas.push(`Categoria: ${p.categoria}`);
  if (p.estampa) linhas.push(`Estampa: ${p.estampa}`);
  linhas.push(`Preço: ${p.preco != null ? moeda(p.preco) : 'Consulte o valor'}`);
  if (p.codigo) linhas.push(`Código: ${p.codigo}`);
  linhas.push('', 'Quais tamanhos estão disponíveis?');
  return linhas.join('\n');
}

async function prepararPagina(page) {
  await page.addInitScript(() => {
    window.__abertos = [];
    window.open = (url) => { window.__abertos.push(url); return null; };
  });
  await page.goto('/');
  await page.waitForSelector('.produto-card');
}

test('links gerais de WhatsApp e Instagram', async ({ page, request }) => {
  const config = await (await request.get('/api/loja/config')).json();
  await prepararPagina(page);
  const numero = config.whatsapp.replace(/\D/g, '');
  const generica = `https://wa.me/${numero}?text=${encodeURIComponent(`Olá! 💛 Gostaria de saber mais sobre os pijamas da ${config.nome}.`)}`;
  for (const id of ['cta-header-whatsapp', 'cta-mobile-whatsapp', 'cta-hero-whatsapp', 'fab-whatsapp', 'footer-whatsapp']) {
    await expect(page.locator(`#${id}`)).toHaveAttribute('href', generica);
  }
  await expect(page.locator('#cta-tamanhos-whatsapp')).toHaveAttribute('href',
    `https://wa.me/${numero}?text=${encodeURIComponent(`Olá! 💛 Gostaria de ajuda para escolher o tamanho ideal de um pijama ${config.nome}.`)}`);
  for (const id of ['link-instagram', 'footer-instagram']) {
    await expect(page.locator(`#${id}`)).toHaveAttribute('href', `https://instagram.com/${config.instagram}`);
  }
});

test('"Quero esse" e o botão do card em TODAS as peças', async ({ page, request }) => {
  test.setTimeout(180_000);
  const config = await (await request.get('/api/loja/config')).json();
  const produtos = await (await request.get('/api/loja/produtos')).json();
  const numero = config.whatsapp.replace(/\D/g, '');
  await prepararPagina(page);

  // Mostra todas as peças no grid e clica no botão de WhatsApp de cada card.
  await page.evaluate(() => { limiteGridAtual = 100000; renderGrid(); });
  const botoes = page.locator('.produto-card__wpp');
  await expect(botoes).toHaveCount(produtos.length);
  for (let i = 0; i < produtos.length; i++) await botoes.nth(i).dispatchEvent('click');

  // Abre o detalhe de cada peça e clica em "Quero esse pijama".
  for (const p of produtos) {
    await page.evaluate(id => abrirProduto(id), p.id);
    await page.locator('#btn-quero-esse').click();
    await page.evaluate(() => fecharProduto());
  }

  const abertos = await page.evaluate(() => window.__abertos);
  const ordemGrid = await page.evaluate(() => produtos.map(p => p.id));
  const porId = Object.fromEntries(produtos.map(p => [p.id, p]));
  const esperados = [
    ...ordemGrid.map(id => porId[id]),
    ...produtos
  ].map(p => `https://wa.me/${numero}?text=${encodeURIComponent(mensagemEsperada(p, config.nome))}`);
  expect(abertos.length).toBe(produtos.length * 2);
  expect(abertos).toEqual(esperados);
});

test.describe('sem JavaScript', () => {
  test.use({ javaScriptEnabled: false });

  test('peças principais em HTML com o mesmo link de WhatsApp', async ({ page, request }) => {
    const config = await (await request.get('/api/loja/config')).json();
    const produtos = await (await request.get('/api/loja/produtos')).json();
    const numero = config.whatsapp.replace(/\D/g, '');
    await page.goto('/');
    const cards = page.locator('#grid-produtos .produto-card');
    await expect(cards).toHaveCount(8);
    await expect(cards.first()).toBeVisible();
    await expect(page.locator('#grid-produtos .loja-msg')).toBeHidden();

    const porNome = Object.fromEntries(produtos.map(p => [p.nome, p]));
    for (let i = 0; i < 8; i++) {
      const nome = await cards.nth(i).locator('.produto-card__nome').textContent();
      const p = porNome[nome];
      expect(p, `peça "${nome}" existe na API`).toBeTruthy();
      await expect(cards.nth(i).locator('.produto-card__wpp')).toHaveAttribute('href',
        `https://wa.me/${numero}?text=${encodeURIComponent(mensagemEsperada(p, config.nome))}`);
    }
    const generica = `https://wa.me/${numero}?text=${encodeURIComponent(`Olá! 💛 Gostaria de saber mais sobre os pijamas da ${config.nome}.`)}`;
    for (const id of ['cta-header-whatsapp', 'cta-hero-whatsapp', 'fab-whatsapp', 'footer-whatsapp']) {
      await expect(page.locator(`#${id}`)).toHaveAttribute('href', generica);
    }
    await expect(page.locator('#link-instagram')).toHaveAttribute('href', `https://instagram.com/${config.instagram}`);
  });
});
