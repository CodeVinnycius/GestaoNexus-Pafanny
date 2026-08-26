package com.estoque.service;

import com.estoque.model.*;
import com.estoque.repository.MovimentacaoRepository;
import com.estoque.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MovimentacaoService {

    private final MovimentacaoRepository movRepo;
    private final ProdutoRepository      prodRepo;

    public MovimentacaoService(MovimentacaoRepository movRepo, ProdutoRepository prodRepo) {
        this.movRepo  = movRepo;
        this.prodRepo = prodRepo;
    }

    // ── Registrar movimentação genérica ───────────────────────────────────────

    public Movimentacao registrar(Long produtoId,
                                   TipoMovimentacao tipo,
                                   int quantidade,
                                   String responsavel,
                                   String motivo,
                                   Empresa empresa) {

        Produto produto = prodRepo.findByIdAndEmpresa(produtoId, empresa)
            .orElseThrow(() -> new RuntimeException("Produto não encontrado."));

        // Calcula delta de estoque conforme o tipo
        int delta = switch (tipo) {
            case VENDA   -> -Math.abs(quantidade);   // saída
            case ENTRADA -> +Math.abs(quantidade);   // entrada
            case AJUSTE  -> quantidade;               // pode ser positivo ou negativo
            case DEVOLUCAO -> +Math.abs(quantidade); // devolução = entrada
        };

        int novaQtd = produto.getQuantidade() + delta;
        if (novaQtd < 0)
            throw new RuntimeException("Estoque insuficiente. Disponível: " + produto.getQuantidade());

        produto.setQuantidade(novaQtd);
        prodRepo.save(produto);

        // Preço de referência: venda para saídas, custo para entradas
        double precoRef = (tipo == TipoMovimentacao.VENDA)
            ? (produto.getPrecoVenda() != null ? produto.getPrecoVenda() : 0)
            : produto.getPrecoCusto();

        Movimentacao mov = new Movimentacao();
        mov.setTipo(tipo);
        mov.setQuantidade(delta);        // valor com sinal
        mov.setNomeProduto(produto.getNome());
        mov.setDataHora(LocalDateTime.now());
        mov.setResponsavel(responsavel != null ? responsavel.trim() : "—");
        mov.setMotivo(motivo != null ? motivo.trim() : "");
        mov.setPrecoUnitario(precoRef);
        mov.setEmpresa(empresa);
        mov.setProduto(produto);

        return movRepo.save(mov);
    }

    // ── Listar histórico ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Movimentacao> listar(Empresa empresa) {
        return movRepo.findByEmpresaOrderByDataHoraDesc(empresa);
    }

    @Transactional(readOnly = true)
    public List<Movimentacao> listarPorTipo(Empresa empresa, TipoMovimentacao tipo) {
        return movRepo.findByEmpresaAndTipoOrderByDataHoraDesc(empresa, tipo);
    }

    @Transactional(readOnly = true)
    public double receitaTotalVendas(Empresa empresa) {
        return movRepo.somarReceitaVendas(empresa);
    }
}
