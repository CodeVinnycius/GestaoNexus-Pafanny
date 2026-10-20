// Teste com bloqueador de anúncios de verdade, contra o site LOCAL.
//   node ferramentas/teste-bloqueador.js            (site em http://localhost:8081)
// 1) Rede: abre a loja num Chromium com o uBlock Origin Lite instalado e lista o que ele barrou.
// 2) Elementos escondidos: aplica as regras genéricas de esconder elementos da EasyList e da
//    EasyPrivacy (as listas usadas por uBlock, Brave e Vivaldi) e lista o que seria escondido.
// Na primeira vez baixa a extensão e as listas para e2e/.bloqueador (fora do git).
const { chromium } = require('@playwright/test');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const LOJA_URL = process.env.LOJA_URL || 'http://localhost:8081';
if (/pafanny\.com\.br|railway\.app/.test(LOJA_URL)) throw new Error('Só contra o site local.');
const PASTA = path.join(__dirname, '..', '.bloqueador');

async function baixar(url, destino) {
  const r = await fetch(url);
  if (!r.ok) throw new Error(`Falha ao baixar ${url}: ${r.status}`);
  fs.writeFileSync(destino, Buffer.from(await r.arrayBuffer()));
}

async function prepararArquivos() {
  fs.mkdirSync(PASTA, { recursive: true });
  const ubol = path.join(PASTA, 'ubol');
  if (!fs.existsSync(path.join(ubol, 'manifest.json'))) {
    const rel = await (await fetch('https://api.github.com/repos/uBlockOrigin/uBOL-home/releases/latest')).json();
    const zip = rel.assets.find(a => /chromium\.zip$/.test(a.name));
    await baixar(zip.browser_download_url, path.join(PASTA, 'ubol.zip'));
    fs.mkdirSync(ubol, { recursive: true });
    execFileSync('tar', ['-xf', path.join(PASTA, 'ubol.zip'), '-C', ubol]);
  }
  for (const lista of ['easylist', 'easyprivacy']) {
    const arq = path.join(PASTA, `${lista}.txt`);
    if (!fs.existsSync(arq)) await baixar(`https://easylist.to/easylist/${lista}.txt`, arq);
  }
  return ubol;
}

async function testarRede(ubol) {
  const perfil = fs.mkdtempSync(path.join(os.tmpdir(), 'pafanny-ubol-'));
  const ctx = await chromium.launchPersistentContext(perfil, {
    channel: 'chromium',
    args: [`--disable-extensions-except=${ubol}`, `--load-extension=${ubol}`]
  });
  try {
    // Dá tempo para a extensão carregar as regras.
    if (!ctx.serviceWorkers().length) await ctx.waitForEvent('serviceworker', { timeout: 15000 }).catch(() => {});
    const p = await ctx.newPage();
    await p.waitForTimeout(3000);
    // O uBlock bloqueia alguns pedidos (BLOCKED_BY_CLIENT) e troca outros por uma versão neutra
    // da própria extensão (pedido a chrome-extension://). Os dois contam como "barrado".
    const barrados = [];
    p.on('requestfailed', r => { if (/BLOCKED_BY_CLIENT/.test(r.failure()?.errorText || '')) barrados.push('bloqueado: ' + r.url()); });
    p.on('request', r => { if (r.url().startsWith('chrome-extension://')) barrados.push('trocado: ' + r.url()); });

    // Controle: o bloqueador precisa estar ativo (o script do Google Ads deve ser barrado).
    await p.goto(LOJA_URL + '/robots.txt');
    await p.setContent('<script src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"></script>');
    await p.waitForTimeout(2000);
    const controle = barrados.length ? 'bloqueado' : 'carregou';

    barrados.length = 0;
    await p.goto(LOJA_URL + '/');
    await p.waitForSelector('.produto-card', { timeout: 30000 });
    await p.evaluate(async () => { for (let y = 0; y < document.body.scrollHeight; y += 400) { scrollTo(0, y); await new Promise(r => setTimeout(r, 50)); } });
    await p.waitForLoadState('networkidle');
    const cards = await p.locator('.produto-card').count();
    return { controle, barrados, cards };
  } finally {
    await ctx.close();
    fs.rmSync(perfil, { recursive: true, force: true });
  }
}

function seletoresGenericos() {
  const seletores = [];
  for (const lista of ['easylist', 'easyprivacy']) {
    for (const linha of fs.readFileSync(path.join(PASTA, `${lista}.txt`), 'utf8').split(/\r?\n/)) {
      // Só regras genéricas ("##seletor"), que valem para qualquer site.
      if (!linha.startsWith('##')) continue;
      const sel = linha.slice(2);
      if (/:-abp-|:has-text|:contains|:xpath|:upward|:matches-|:style|:remove|\{/.test(sel)) continue;
      seletores.push(sel);
    }
  }
  return seletores;
}

async function testarEsconder(seletores) {
  const b = await chromium.launch();
  try {
    const p = await b.newPage({ viewport: { width: 1440, height: 900 } });
    await p.goto(LOJA_URL + '/');
    await p.waitForSelector('.produto-card');
    await p.evaluate(() => { limiteGridAtual = 100000; renderGrid(); }); // todas as peças no grid
    return await p.evaluate(lista => {
      const achados = [];
      for (const sel of lista) {
        let els;
        try { els = document.querySelectorAll(sel); } catch { continue; }
        for (const el of els) achados.push(`${sel}  ->  <${el.tagName.toLowerCase()} class="${el.className}" id="${el.id}">`);
      }
      return achados;
    }, seletores);
  } finally {
    await b.close();
  }
}

(async () => {
  const ubol = await prepararArquivos();
  const rede = await testarRede(ubol);
  const seletores = seletoresGenericos();
  const escondidos = await testarEsconder(seletores);

  console.log(`uBlock Origin Lite ativo? ${rede.controle === 'bloqueado' ? 'sim (controle bloqueado)' : 'NÃO — resultado da rede inválido'}`);
  console.log(`Arquivos da loja barrados pelo uBlock: ${rede.barrados.length ? '\n  ' + rede.barrados.join('\n  ') : 'nenhum'}`);
  console.log(`Peças exibidas com o bloqueador ligado: ${rede.cards}`);
  console.log(`Regras genéricas de esconder testadas: ${seletores.length}`);
  console.log(`Elementos da loja que seriam escondidos: ${escondidos.length ? '\n  ' + escondidos.join('\n  ') : 'nenhum'}`);
  const ok = rede.controle === 'bloqueado' && !rede.barrados.length && rede.cards > 0 && !escondidos.length;
  console.log(ok ? '\nOK: a loja passa ilesa pelo bloqueador.' : '\nATENÇÃO: veja os itens acima.');
  process.exit(ok ? 0 : 1);
})();
