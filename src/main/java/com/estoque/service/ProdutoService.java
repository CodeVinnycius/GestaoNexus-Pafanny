package com.estoque.service;

import com.estoque.exception.ProdutoDuplicadoException;
import com.estoque.exception.ProdutoNaoEncontradoException;
import com.estoque.model.Empresa;
import com.estoque.model.Movimentacao;
import com.estoque.model.Produto;
import com.estoque.model.Variacao;
import com.estoque.repository.MovimentacaoRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.repository.VariacaoRepository;
import com.estoque.util.CsvUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ProdutoService {

    private final ProdutoRepository repository;
    private final VariacaoRepository variacaoRepository;
    private final ArmazenamentoImagemService armazenamentoImagemService;
    private final MovimentacaoRepository movimentacaoRepository;

    public ProdutoService(ProdutoRepository repository, VariacaoRepository variacaoRepository,
                           ArmazenamentoImagemService armazenamentoImagemService,
                           MovimentacaoRepository movimentacaoRepository) {
        this.repository = repository;
        this.variacaoRepository = variacaoRepository;
        this.armazenamentoImagemService = armazenamentoImagemService;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public Produto cadastrar(Produto produto, Empresa empresa) {
        if (repository.existsByNomeIgnoreCaseAndEmpresa(produto.getNome(), empresa))
            throw new ProdutoDuplicadoException(produto.getNome());
        produto.setEmpresa(empresa);
        return repository.save(produto);
    }

    @Transactional(readOnly = true)
    public List<Produto> listarTodos(Empresa empresa) {
        return repository.findByEmpresa(empresa);
    }

    @Transactional(readOnly = true)
    public List<Produto> buscarPorNome(String termo, Empresa empresa) {
        if (termo == null || termo.isBlank()) return listarTodos(empresa);
        return repository.findByEmpresaAndNomeContainingIgnoreCase(empresa, termo.trim());
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id, Empresa empresa) {
        return repository.findByIdAndEmpresa(id, empresa)
            .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    public Produto atualizarQuantidade(Long id, int novaQtd, Empresa empresa) {
        if (novaQtd < 0) throw new IllegalArgumentException("Quantidade não pode ser negativa.");
        Produto p = buscarPorId(id, empresa);
        p.setQuantidade(novaQtd);
        return repository.save(p);
    }

    public Produto editar(Long id, Produto dados, Empresa empresa) {
        Produto p = buscarPorId(id, empresa);

        if (dados.getNome() != null && !dados.getNome().isBlank()) {
            String novoNome = dados.getNome().trim();
            if (!novoNome.equalsIgnoreCase(p.getNome())
                    && repository.existsByNomeIgnoreCaseAndEmpresa(novoNome, empresa))
                throw new ProdutoDuplicadoException(novoNome);
            p.setNome(novoNome);
        }
        if (dados.getPrecoCusto() >= 0) p.setPrecoCusto(dados.getPrecoCusto());
        // Preço de venda pode ser nulo de propósito ("consulte o valor") — nunca inventamos um valor aqui.
        if (dados.getPrecoVenda() == null || dados.getPrecoVenda() >= 0) p.setPrecoVenda(dados.getPrecoVenda());
        // Produtos com variações têm a quantidade recalculada a partir delas — ignora valor manual.
        if (dados.getQuantidade()  >= 0 && p.getVariacoes().isEmpty()) p.setQuantidade(dados.getQuantidade());
        p.setDescricao(dados.getDescricao());
        p.setCategoria(dados.getCategoria());
        p.setVisivelLoja(dados.isVisivelLoja());
        p.setEstampa(dados.getEstampa());
        if (dados.getCodigo() != null && !dados.getCodigo().isBlank()) p.setCodigo(dados.getCodigo().trim());
        p.setDisponibilidade(dados.getDisponibilidade());

        return repository.save(p);
    }

    public void remover(Long id, Empresa empresa) {
        Produto p = buscarPorId(id, empresa);
        p.getImagens().forEach(armazenamentoImagemService::remover);

        // Solta a referência das movimentações em vez de apagá-las — o histórico é
        // mantido (nomeProduto já é um snapshot), só o produto_id vira nulo.
        List<Movimentacao> movs = movimentacaoRepository.findByProduto(p);
        movs.forEach(m -> m.setProduto(null));
        movimentacaoRepository.saveAll(movs);

        repository.delete(p);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VARIAÇÕES (tamanho / cor)
    // ══════════════════════════════════════════════════════════════════════════

    public Produto adicionarVariacao(Long produtoId, Variacao dados, Empresa empresa) {
        Produto p = buscarPorId(produtoId, empresa);
        String tamanho = dados.getTamanho() == null ? "" : dados.getTamanho().trim();
        String cor     = dados.getCor()     == null ? null : dados.getCor().trim();
        if (tamanho.isBlank())
            throw new IllegalArgumentException("O tamanho não pode ser vazio.");
        if (dados.getQuantidade() < 0)
            throw new IllegalArgumentException("Quantidade não pode ser negativa.");

        boolean duplicada = p.getVariacoes().stream().anyMatch(v ->
            v.getTamanho().equalsIgnoreCase(tamanho)
            && java.util.Objects.equals(
                   v.getCor() == null ? "" : v.getCor().toLowerCase(),
                   cor == null ? "" : cor.toLowerCase()));
        if (duplicada)
            throw new IllegalArgumentException("Já existe uma variação com esse tamanho/cor para este produto.");

        Variacao v = new Variacao();
        v.setTamanho(tamanho);
        v.setCor(cor);
        v.setQuantidade(dados.getQuantidade());
        v.setProduto(p);
        p.getVariacoes().add(v);
        recalcularQuantidade(p);
        return repository.save(p);
    }

    public Produto editarVariacao(Long produtoId, Long variacaoId, Variacao dados, Empresa empresa) {
        Produto p = buscarPorId(produtoId, empresa);
        Variacao v = variacaoRepository.findByIdAndProduto(variacaoId, p)
            .orElseThrow(() -> new IllegalArgumentException("Variação não encontrada para este produto."));
        if (dados.getQuantidade() < 0)
            throw new IllegalArgumentException("Quantidade não pode ser negativa.");
        v.setQuantidade(dados.getQuantidade());
        recalcularQuantidade(p);
        return repository.save(p);
    }

    public Produto removerVariacao(Long produtoId, Long variacaoId, Empresa empresa) {
        Produto p = buscarPorId(produtoId, empresa);
        Variacao v = variacaoRepository.findByIdAndProduto(variacaoId, p)
            .orElseThrow(() -> new IllegalArgumentException("Variação não encontrada para este produto."));
        p.getVariacoes().remove(v);
        recalcularQuantidade(p);
        return repository.save(p);
    }

    private void recalcularQuantidade(Produto p) {
        if (!p.getVariacoes().isEmpty()) {
            p.setQuantidade(p.getVariacoes().stream().mapToInt(Variacao::getQuantidade).sum());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  IMAGENS
    // ══════════════════════════════════════════════════════════════════════════

    public Produto adicionarImagem(Long produtoId, String url, Empresa empresa) {
        Produto p = buscarPorId(produtoId, empresa);
        p.getImagens().add(url);
        return repository.save(p);
    }

    public Produto removerImagem(Long produtoId, String url, Empresa empresa) {
        Produto p = buscarPorId(produtoId, empresa);
        p.getImagens().remove(url);
        return repository.save(p);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXPORTAÇÃO CSV
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public String exportarCsv(Empresa empresa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nome;Categoria;Estampa;Codigo;Quantidade;Custo Unitario;Preco Venda;Margem (%);Custo Total;Valor Total;Lucro Potencial;Disponibilidade;Visivel na Loja\r\n");
        for (Produto p : listarTodos(empresa)) {
            sb.append(CsvUtil.campo(p.getNome())).append(';')
              .append(CsvUtil.campo(p.getCategoria())).append(';')
              .append(CsvUtil.campo(p.getEstampa())).append(';')
              .append(CsvUtil.campo(p.getCodigo())).append(';')
              .append(p.getQuantidade()).append(';')
              .append(CsvUtil.numero(p.getPrecoCusto())).append(';')
              .append(p.getPrecoVenda() != null ? CsvUtil.numero(p.getPrecoVenda()) : "").append(';')
              .append(CsvUtil.numero(p.getMargemLucro())).append(';')
              .append(CsvUtil.numero(p.getCustoTotal())).append(';')
              .append(CsvUtil.numero(p.getValorTotal())).append(';')
              .append(CsvUtil.numero(p.getLucroPotencial())).append(';')
              .append(p.getDisponibilidade()).append(';')
              .append(p.isVisivelLoja() ? "Sim" : "Nao")
              .append("\r\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public Map<String, Double> resumoFinanceiro(Empresa empresa) {
        double custoTotal  = repository.somarCustoTotal(empresa);
        double valorVenda  = repository.somarValorVenda(empresa);
        double lucroPot    = valorVenda - custoTotal;
        double margem      = valorVenda > 0 ? (lucroPot / valorVenda) * 100 : 0;
        return Map.of(
            "custoTotal",    custoTotal,
            "valorVenda",    valorVenda,
            "lucroPotencial",lucroPot,
            "margemMedia",   margem
        );
    }
}
