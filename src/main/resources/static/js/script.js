/**
 * GestãoNexus — script.js
 * Multi-tenant · JWT · Produtos com custo/venda · Movimentações · Histórico
 */

// ── Auth guard ────────────────────────────────────────────────────────────────
const token = localStorage.getItem('token');
if (!token) location.href = '/login.html';

const nomeEl = document.getElementById('empresa-nome');
if (nomeEl) nomeEl.textContent = localStorage.getItem('nome') || '';

const $ = id => document.getElementById(id);
const API = '/api';

// ── Fetch autenticado (redireciona se token expirar) ──────────────────────────
async function api(url, opts = {}) {
  const res = await fetch(url, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token,
      ...(opts.headers || {})
    }
  });
  if (res.status === 401 || res.status === 403) {
    localStorage.clear(); location.href = '/login.html'; return null;
  }
  return res;
}

// ── Estado ────────────────────────────────────────────────────────────────────
let produtos      = [];
let modoEdicao    = false;
let idParaEditar  = null;
let idParaRemover = null;

// ══════════════════════════════════════════════════════════════════════════════
//  CARREGAMENTO
// ══════════════════════════════════════════════════════════════════════════════
async function carregarTudo() {
  await Promise.all([carregarProdutos(), carregarResumo(), carregarReceita()]);
}

async function carregarProdutos() {
  try {
    const res = await api(`${API}/produtos`);
    if (!res) return;
    produtos = await res.json();
    renderDashboard();
    renderProdutos(produtos);
    preencherSelectProdutos();
  } catch {
    $('tbody-dashboard').innerHTML = `<tr><td colspan="6" class="table__empty">Erro ao conectar.</td></tr>`;
    $('tbody-produtos').innerHTML  = `<tr><td colspan="7" class="table__empty">Erro ao conectar.</td></tr>`;
  }
}

async function carregarResumo() {
  try {
    const res = await api(`${API}/produtos/valor-total`);
    if (!res) return;
    const d = await res.json();
    $('stat-custo').textContent  = moeda(d.custoTotal    ?? 0);
    $('stat-venda').textContent  = moeda(d.valorVenda    ?? 0);
    $('stat-lucro').textContent  = moeda(d.lucroPotencial ?? 0);
    $('stat-margem').textContent = (d.margemMedia ?? 0).toFixed(1) + '%';
  } catch { /* silencioso */ }
}

async function carregarReceita() {
  try {
    const res = await api(`${API}/movimentacoes/receita`);
    if (!res) return;
    const d = await res.json();
    $('stat-receita').textContent = moeda(d.receita ?? 0);
  } catch { /* silencioso */ }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DASHBOARD
// ══════════════════════════════════════════════════════════════════════════════
function renderDashboard() {
  const qtd      = produtos.length;
  const unidades = produtos.reduce((s, p) => s + p.quantidade, 0);
  const top      = qtd ? produtos.reduce((a, b) =>
    (a.lucroPotencial ?? 0) > (b.lucroPotencial ?? 0) ? a : b) : null;

  $('stat-total').textContent    = qtd;
  $('stat-unidades').textContent = unidades;
  $('stat-top').textContent      = top ? top.nome : '—';

  const top6 = [...produtos].sort((a, b) => (b.lucroPotencial ?? 0) - (a.lucroPotencial ?? 0)).slice(0, 6);

  $('tbody-dashboard').innerHTML = top6.length
    ? top6.map(p => {
        const margem = p.margemLucro ?? 0;
        const cls    = margem >= 40 ? 'good' : margem >= 20 ? 'mid' : 'low';
        return `<tr>
          <td>${esc(p.nome)}</td>
          <td><span class="badge-qty ${p.quantidade <= 5 ? 'low' : ''}">${p.quantidade}</span></td>
          <td class="price">${moeda(p.precoCusto)}</td>
          <td class="price">${moeda(p.precoVenda ?? p.preco)}</td>
          <td><span class="badge-margem ${cls}">${margem.toFixed(1)}%</span></td>
          <td class="price-total">${moeda(p.lucroPotencial ?? 0)}</td>
        </tr>`;
      }).join('')
    : '<tr><td colspan="6" class="table__empty">Nenhum produto cadastrado.</td></tr>';
}

// ══════════════════════════════════════════════════════════════════════════════
//  PRODUTOS
// ══════════════════════════════════════════════════════════════════════════════
function renderProdutos(lista) {
  $('tbody-produtos').innerHTML = lista.length
    ? lista.map(p => {
        const margem = p.margemLucro ?? 0;
        const cls    = margem >= 40 ? 'good' : margem >= 20 ? 'mid' : 'low';
        return `<tr data-id="${p.id}">
          <td>${esc(p.nome)}${p.visivelLoja ? ' <span class="badge-margem good" title="Visível na loja online">loja</span>' : ''}</td>
          <td><span class="badge-qty ${p.quantidade <= 5 ? 'low' : ''}">${p.quantidade}</span></td>
          <td class="price">${moeda(p.precoCusto)}</td>
          <td class="price">${moeda(p.precoVenda ?? p.preco)}</td>
          <td><span class="badge-margem ${cls}">${margem.toFixed(1)}%</span></td>
          <td class="price-total">${moeda(p.valorTotal ?? 0)}</td>
          <td><div class="actions">
            <button class="btn-icon" onclick="abrirEdicao(${p.id})">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              Editar
            </button>
            <button class="btn-icon btn-icon--danger" onclick="confirmarRemocao(${p.id},'${escAttr(p.nome)}')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6M14 11v6"/></svg>
              Remover
            </button>
          </div></td>
        </tr>`;
      }).join('')
    : '<tr><td colspan="7" class="table__empty">Nenhum produto encontrado.</td></tr>';
}

// Busca
let searchTimer;
$('search').addEventListener('input', function () {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(async () => {
    try {
      const res = await api(`${API}/produtos${this.value.trim() ? '?busca=' + encodeURIComponent(this.value.trim()) : ''}`);
      if (res) renderProdutos(await res.json());
    } catch {
      renderProdutos(produtos.filter(p => p.nome.toLowerCase().includes(this.value.toLowerCase())));
    }
  }, 300);
});

// ── Modal produto ─────────────────────────────────────────────────────────────
function abrirModal(modo, produto = null) {
  modoEdicao = modo === 'editar'; resetForm();
  if (modoEdicao && produto) {
    $('modal-title').textContent = 'Editar produto';
    idParaEditar = produto.id;
    $('f-nome').value        = produto.nome;
    $('f-quantidade').value  = produto.quantidade;
    $('f-custo').value       = produto.precoCusto ?? '';
    $('f-venda').value       = produto.precoVenda ?? produto.preco ?? '';
    $('f-categoria').value   = produto.categoria ?? '';
    $('f-estampa').value     = produto.estampa ?? '';
    $('f-codigo').value      = produto.codigo ?? '';
    $('f-disponibilidade').value = produto.disponibilidade ?? 'CONSULTAR';
    $('f-descricao').value   = produto.descricao ?? '';
    $('f-visivel-loja').checked = !!produto.visivelLoja;
    atualizarPreviewMargem();

    $('bloco-variacoes').style.display = 'block';
    $('bloco-fotos').style.display     = 'block';
    $('f-quantidade').disabled = (produto.variacoes ?? []).length > 0;
    renderVariacoes(produto.variacoes ?? []);
    renderFotos(produto.imagens ?? []);
  } else {
    $('modal-title').textContent = 'Novo produto'; idParaEditar = null;
    $('bloco-variacoes').style.display = 'none';
    $('bloco-fotos').style.display     = 'none';
    $('f-quantidade').disabled = false;
  }
  $('modal-overlay').classList.add('open');
  setTimeout(() => $('f-nome').focus(), 80);
}

function fecharModal() { $('modal-overlay').classList.remove('open'); resetForm(); }
function resetForm() {
  $('modal-form').reset();
  ['nome','quantidade','custo','venda'].forEach(c => {
    const e = $(`err-${c}`); if (e) e.textContent = '';
    const i = $(`f-${c}`);   if (i) i.classList.remove('error');
  });
  $('margem-preview').style.display = 'none';
  $('lista-variacoes').innerHTML = '';
  $('lista-fotos').innerHTML = '';
  $('err-variacao').textContent = '';
  $('err-foto').textContent = '';
  $('btn-salvar').disabled = false; $('btn-salvar').textContent = 'Salvar';
}

// Recarrega só o produto em edição (dados de variações/fotos) sem fechar o modal
async function recarregarProdutoEmEdicao() {
  if (!idParaEditar) return;
  const res = await api(`${API}/produtos/${idParaEditar}`);
  if (!res || !res.ok) return;
  const p = await res.json();
  const idx = produtos.findIndex(x => x.id === p.id);
  if (idx >= 0) produtos[idx] = p;
  $('f-quantidade').value = p.quantidade;
  $('f-quantidade').disabled = (p.variacoes ?? []).length > 0;
  renderVariacoes(p.variacoes ?? []);
  renderFotos(p.imagens ?? []);
  renderProdutos(produtos);
  renderDashboard();
  preencherSelectProdutos();
}

// ── Variações (tamanho / cor) ──────────────────────────────────────────────
function renderVariacoes(lista) {
  $('lista-variacoes').innerHTML = lista.length
    ? lista.map(v => `
        <div class="variacao-row" data-id="${v.id}">
          <span class="variacao-row__label">${esc(v.tamanho)}${v.cor ? ' · ' + esc(v.cor) : ''}</span>
          <input class="variacao-row__qtd" type="number" min="0" step="1" value="${v.quantidade}"
                 onchange="editarVariacao(${v.id}, this.value)"/>
          <button type="button" class="variacao-row__remover" onclick="removerVariacao(${v.id})" title="Remover">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>`).join('')
    : '<p class="bloco-extra__hint">Nenhuma variação cadastrada — o produto usa a quantidade única acima.</p>';
}

$('btn-add-variacao').onclick = async () => {
  if (!idParaEditar) return;
  const tamanho = $('nv-tamanho').value.trim();
  const cor     = $('nv-cor').value.trim();
  const qtd     = parseInt($('nv-qtd').value);
  $('err-variacao').textContent = '';
  if (!tamanho) { $('err-variacao').textContent = 'Informe o tamanho.'; return; }
  if (!Number.isInteger(qtd) || qtd < 0) { $('err-variacao').textContent = 'Quantidade inválida.'; return; }
  try {
    const res = await api(`${API}/produtos/${idParaEditar}/variacoes`, {
      method: 'POST', body: JSON.stringify({ tamanho, cor: cor || null, quantidade: qtd })
    });
    const json = await res.json();
    if (!res.ok) { $('err-variacao').textContent = json.erro || 'Erro.'; return; }
    $('nv-tamanho').value = ''; $('nv-cor').value = ''; $('nv-qtd').value = '';
    await recarregarProdutoEmEdicao();
    toast('Variação adicionada!', 'success');
  } catch { $('err-variacao').textContent = 'Erro de conexão.'; }
};

async function editarVariacao(variacaoId, novaQtd) {
  const qtd = parseInt(novaQtd);
  if (!Number.isInteger(qtd) || qtd < 0) { toast('Quantidade inválida.', 'error'); return; }
  try {
    const res = await api(`${API}/produtos/${idParaEditar}/variacoes/${variacaoId}`, {
      method: 'PUT', body: JSON.stringify({ quantidade: qtd })
    });
    if (!res.ok) { const j = await res.json(); toast(j.erro || 'Erro.', 'error'); return; }
    await recarregarProdutoEmEdicao();
  } catch { toast('Erro de conexão.', 'error'); }
}

async function removerVariacao(variacaoId) {
  try {
    const res = await api(`${API}/produtos/${idParaEditar}/variacoes/${variacaoId}`, { method: 'DELETE' });
    if (!res.ok) { const j = await res.json(); toast(j.erro || 'Erro.', 'error'); return; }
    await recarregarProdutoEmEdicao();
    toast('Variação removida.', 'success');
  } catch { toast('Erro de conexão.', 'error'); }
}

// ── Fotos ────────────────────────────────────────────────────────────────────
function renderFotos(lista) {
  $('lista-fotos').innerHTML = lista.map(url => `
    <div class="foto-thumb">
      <img src="${esc(url)}" alt="Foto do produto"/>
      <button type="button" class="foto-thumb__remover" onclick="removerFoto('${escAttr(url)}')" title="Remover">✕</button>
    </div>`).join('');
}

$('btn-add-foto').onclick = () => { if (idParaEditar) $('input-foto').click(); };

$('input-foto').addEventListener('change', async () => {
  const arquivo = $('input-foto').files[0];
  $('input-foto').value = '';
  if (!arquivo || !idParaEditar) return;
  $('err-foto').textContent = '';
  const formData = new FormData();
  formData.append('arquivo', arquivo);
  try {
    const res = await fetch(`${API}/produtos/${idParaEditar}/imagens`, {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token },
      body: formData
    });
    if (res.status === 401 || res.status === 403) { localStorage.clear(); location.href = '/login.html'; return; }
    const json = await res.json();
    if (!res.ok) { $('err-foto').textContent = json.erro || 'Erro ao enviar foto.'; return; }
    await recarregarProdutoEmEdicao();
    toast('Foto adicionada!', 'success');
  } catch { $('err-foto').textContent = 'Erro de conexão.'; }
});

async function removerFoto(url) {
  try {
    const res = await api(`${API}/produtos/${idParaEditar}/imagens?url=${encodeURIComponent(url)}`, { method: 'DELETE' });
    if (!res.ok) { const j = await res.json(); toast(j.erro || 'Erro.', 'error'); return; }
    await recarregarProdutoEmEdicao();
    toast('Foto removida.', 'success');
  } catch { toast('Erro de conexão.', 'error'); }
}

function atualizarPreviewMargem() {
  const custo = parseFloat($('f-custo').value) || 0;
  const venda = parseFloat($('f-venda').value) || 0;
  const prev  = $('margem-preview');
  if (custo > 0 && venda > 0) {
    const lucro  = venda - custo;
    const margem = ((lucro / venda) * 100).toFixed(1);
    prev.style.display = 'flex';
    prev.innerHTML = `<span>Margem: <span class="badge-margem ${+margem >= 40 ? 'good' : +margem >= 20 ? 'mid' : 'low'}">${margem}%</span></span><strong>Lucro unit.: ${moeda(lucro)}</strong>`;
  } else { prev.style.display = 'none'; }
}

$('f-custo').addEventListener('input', atualizarPreviewMargem);
$('f-venda').addEventListener('input', atualizarPreviewMargem);

$('btn-novo').onclick      = () => abrirModal('novo');
$('modal-close').onclick   = fecharModal;
$('btn-cancelar').onclick  = fecharModal;
$('modal-overlay').onclick = e => { if (e.target === $('modal-overlay')) fecharModal(); };

function abrirEdicao(id) { const p = produtos.find(x => x.id === id); if (p) abrirModal('editar', p); }

$('modal-form').addEventListener('submit', async e => {
  e.preventDefault();
  if (!validarProduto()) return;
  const vendaTexto = $('f-venda').value.trim();
  const dados = {
    nome:            $('f-nome').value.trim(),
    quantidade:      parseInt($('f-quantidade').value),
    precoCusto:      parseFloat($('f-custo').value),
    precoVenda:      vendaTexto === '' ? null : parseFloat(vendaTexto),
    categoria:       $('f-categoria').value.trim(),
    estampa:         $('f-estampa').value.trim(),
    codigo:          $('f-codigo').value.trim(),
    disponibilidade: $('f-disponibilidade').value,
    descricao:       $('f-descricao').value.trim(),
    visivelLoja:     $('f-visivel-loja').checked
  };
  $('btn-salvar').disabled = true; $('btn-salvar').textContent = 'Salvando…';
  try {
    const res = modoEdicao
      ? await api(`${API}/produtos/${idParaEditar}`, { method:'PUT', body:JSON.stringify(dados) })
      : await api(`${API}/produtos`, { method:'POST', body:JSON.stringify(dados) });
    const json = await res.json();
    if (!res.ok) { toast(json.erro || 'Erro.', 'error'); return; }
    toast(modoEdicao ? 'Produto atualizado!' : 'Produto cadastrado!', 'success');
    fecharModal(); await carregarTudo();
  } catch { toast('Erro de conexão.', 'error'); }
  finally { $('btn-salvar').disabled = false; $('btn-salvar').textContent = 'Salvar'; }
});

function validarProduto() {
  let ok = true;
  const chk = (campo, cond, msg) => {
    $(`err-${campo}`).textContent = cond ? '' : msg;
    $(`f-${campo}`).classList.toggle('error', !cond);
    if (!cond) ok = false;
  };
  const nome  = $('f-nome').value.trim();
  const qtd   = $('f-quantidade').value.trim();
  const custo = $('f-custo').value.trim();
  const venda = $('f-venda').value.trim();
  chk('nome',       nome.length >= 2,                      !nome ? 'Obrigatório.' : 'Mínimo 2 chars.');
  chk('quantidade', qtd !== '' && +qtd >= 0 && Number.isInteger(+qtd), 'Inteiro ≥ 0.');
  chk('custo',      custo !== '' && +custo >= 0,           'Valor ≥ 0.');
  chk('venda',      venda === '' || +venda >= 0,           'Valor ≥ 0 (ou deixe vazio).');
  return ok;
}

// ── Remoção ────────────────────────────────────────────────────────────────
function confirmarRemocao(id, nome) {
  idParaRemover = id; $('confirm-nome').textContent = nome;
  $('confirm-overlay').classList.add('open');
}
function fecharConfirm() { $('confirm-overlay').classList.remove('open'); idParaRemover = null; }
$('confirm-close').onclick  = fecharConfirm;
$('confirm-cancel').onclick = fecharConfirm;
$('confirm-overlay').onclick = e => { if (e.target === $('confirm-overlay')) fecharConfirm(); };
$('confirm-ok').addEventListener('click', async () => {
  const id = idParaRemover; const nome = $('confirm-nome').textContent; fecharConfirm();
  try {
    const res = await api(`${API}/produtos/${id}`, { method:'DELETE' });
    const j   = await res.json();
    toast(res.ok ? `"${nome}" removido.` : (j.erro || 'Erro.'), res.ok ? 'success' : 'error');
    if (res.ok) await carregarTudo();
  } catch { toast('Erro de conexão.', 'error'); }
});

// ══════════════════════════════════════════════════════════════════════════════
//  MOVIMENTAÇÕES
// ══════════════════════════════════════════════════════════════════════════════
function preencherSelectProdutos() {
  const sel = $('mov-produto');
  const val = sel.value;
  sel.innerHTML = '<option value="">Selecione…</option>' +
    produtos.map(p => `<option value="${p.id}" data-venda="${p.precoVenda ?? p.preco}" data-custo="${p.precoCusto}" data-qtd="${p.quantidade}">${esc(p.nome)} (estoque: ${p.quantidade})</option>`).join('');
  if (val) sel.value = val;
}

function atualizarPreviewMov() {
  const sel  = $('mov-produto');
  const opt  = sel.selectedOptions[0];
  const qtd  = parseInt($('mov-quantidade').value) || 0;
  const tipo = $('mov-tipo').value;
  const prev = $('mov-preview');

  if (!opt || !opt.value || qtd <= 0) { prev.style.display = 'none'; return; }

  const precoVenda = parseFloat(opt.dataset.venda) || 0;
  const precoCusto = parseFloat(opt.dataset.custo) || 0;
  const estoqueAtual = parseInt(opt.dataset.qtd) || 0;
  const preco = (tipo === 'VENDA') ? precoVenda : precoCusto;
  const total = qtd * preco;

  let label = '';
  if (tipo === 'VENDA') {
    const lucro = qtd * (precoVenda - precoCusto);
    label = `Venda de ${qtd} un. × ${moeda(precoVenda)} | Lucro estimado: ${moeda(lucro)} | Estoque após: ${estoqueAtual - qtd}`;
  } else if (tipo === 'ENTRADA') {
    label = `Entrada de ${qtd} un. × ${moeda(precoCusto)} custo | Estoque após: ${estoqueAtual + qtd}`;
  } else if (tipo === 'AJUSTE') {
    label = `Ajuste de ${qtd} un. no estoque`;
  } else {
    label = `Devolução de ${qtd} un. | Estoque após: ${estoqueAtual + qtd}`;
  }

  prev.style.display = 'flex';
  $('prev-label').textContent = label;
  $('prev-valor').textContent = moeda(total);
}

$('mov-produto').addEventListener('change', atualizarPreviewMov);
$('mov-quantidade').addEventListener('input', atualizarPreviewMov);
$('mov-tipo').addEventListener('change', atualizarPreviewMov);

$('form-mov').addEventListener('submit', async e => {
  e.preventDefault();
  const produtoId   = $('mov-produto').value;
  const tipo        = $('mov-tipo').value;
  const quantidade  = parseInt($('mov-quantidade').value);
  const responsavel = $('mov-responsavel').value.trim();
  const motivo      = $('mov-motivo').value.trim();

  let ok = true;
  const setErr = (id, msg) => { $(id).textContent = msg; ok = false; };
  const clrErr = id        => $(id).textContent = '';

  clrErr('err-mov-produto'); clrErr('err-mov-qtd'); clrErr('err-mov-resp');

  if (!produtoId)   { setErr('err-mov-produto', 'Selecione um produto.'); }
  if (!quantidade || quantidade <= 0) { setErr('err-mov-qtd', 'Quantidade deve ser ≥ 1.'); }
  if (!responsavel) { setErr('err-mov-resp', 'Informe o responsável.'); }
  if (!ok) return;

  $('btn-mov-registrar').disabled = true;
  $('btn-mov-registrar').textContent = 'Registrando…';

  try {
    const res = await api(`${API}/movimentacoes`, {
      method: 'POST',
      body: JSON.stringify({ produtoId: +produtoId, tipo, quantidade, responsavel, motivo })
    });
    const json = await res.json();
    if (!res.ok) { toast(json.erro || 'Erro.', 'error'); return; }
    toast('Movimentação registrada!', 'success');
    $('form-mov').reset();
    $('mov-preview').style.display = 'none';
    await carregarTudo();
    // Recarrega histórico se estiver visível
    if ($('section-historico').classList.contains('active')) await carregarHistorico();
  } catch { toast('Erro de conexão.', 'error'); }
  finally {
    $('btn-mov-registrar').disabled = false;
    $('btn-mov-registrar').textContent = 'Registrar';
  }
});

// ══════════════════════════════════════════════════════════════════════════════
//  HISTÓRICO
// ══════════════════════════════════════════════════════════════════════════════
async function carregarHistorico() {
  const tipo = $('filtro-tipo').value;
  const url  = tipo ? `${API}/movimentacoes?tipo=${tipo}` : `${API}/movimentacoes`;
  try {
    const res  = await api(url);
    if (!res) return;
    const lista = await res.json();
    renderHistorico(lista);
  } catch { $('tbody-historico').innerHTML = '<tr><td colspan="8" class="table__empty">Erro ao carregar histórico.</td></tr>'; }
}

function renderHistorico(lista) {
  const tipos = { VENDA:'Venda', ENTRADA:'Entrada', AJUSTE:'Ajuste', DEVOLUCAO:'Devolução' };
  $('tbody-historico').innerHTML = lista.length
    ? lista.map(m => {
        const sinal = m.quantidade > 0 ? '+' : '';
        const cls   = m.quantidade > 0 ? 'qtd-pos' : 'qtd-neg';
        return `<tr>
          <td style="white-space:nowrap;font-size:.8125rem;">${m.dataFormatada ?? m.dataHora ?? '—'}</td>
          <td><span class="badge-tipo ${m.tipo}">${tipos[m.tipo] ?? m.tipo}</span></td>
          <td>${esc(m.nomeProduto)}</td>
          <td class="${cls}">${sinal}${m.quantidade}</td>
          <td class="price">${moeda(m.precoUnitario)}</td>
          <td class="price-total">${moeda(m.valorTotal ?? Math.abs(m.quantidade) * m.precoUnitario)}</td>
          <td>${esc(m.responsavel ?? '—')}</td>
          <td style="font-size:.8125rem;color:var(--text-2)">${esc(m.motivo ?? '')}</td>
        </tr>`;
      }).join('')
    : '<tr><td colspan="8" class="table__empty">Nenhuma movimentação registrada.</td></tr>';
}

$('filtro-tipo').addEventListener('change', carregarHistorico);

// ══════════════════════════════════════════════════════════════════════════════
//  NAVEGAÇÃO
// ══════════════════════════════════════════════════════════════════════════════
document.querySelectorAll('.sidebar__link').forEach(link => {
  link.addEventListener('click', async e => {
    e.preventDefault();
    document.querySelectorAll('.sidebar__link').forEach(l => l.classList.remove('active'));
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    link.classList.add('active');
    $('section-' + link.dataset.section).classList.add('active');
    document.querySelector('.sidebar').classList.remove('open');
    $('sidebar-overlay').classList.remove('open');
    if (link.dataset.section === 'historico') await carregarHistorico();
  });
});

// ── Sidebar mobile ────────────────────────────────────────────────────────────
$('hamburger').onclick = () => {
  document.querySelector('.sidebar').classList.toggle('open');
  $('sidebar-overlay').classList.toggle('open');
};
$('sidebar-overlay').onclick = () => {
  document.querySelector('.sidebar').classList.remove('open');
  $('sidebar-overlay').classList.remove('open');
};

// ── Logout ────────────────────────────────────────────────────────────────────
$('btn-logout').onclick = () => { localStorage.clear(); location.href = '/login.html'; };

// ── Toast ─────────────────────────────────────────────────────────────────────
let tt;
function toast(msg, tipo = 'success') {
  const el = $('toast');
  el.textContent = msg; el.className = `toast ${tipo} show`;
  clearTimeout(tt); tt = setTimeout(() => el.classList.remove('show'), 3500);
}

// ── Utils ─────────────────────────────────────────────────────────────────────
const moeda   = v => (+v || 0).toLocaleString('pt-BR', { style:'currency', currency:'BRL' });
const esc     = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
const escAttr = s => String(s).replace(/'/g,"\\'").replace(/"/g,'&quot;');

// ── Init ──────────────────────────────────────────────────────────────────────
carregarTudo();
