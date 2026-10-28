package com.estoque.service;

import com.estoque.model.*;
import com.estoque.repository.MovimentacaoRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.util.CsvUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class MovimentacaoService {

    private final MovimentacaoRepository movimentacaoRepository;
    private final ProdutoRepository      produtoRepository;

    public MovimentacaoService(MovimentacaoRepository movimentacaoRepository, ProdutoRepository produtoRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.produtoRepository      = produtoRepository;
    }

    public Movimentacao registrar(Long produtoId,
                                   TipoMovimentacao tipo,
                                   int quantidade,
                                   String responsavel,
                                   String motivo,
                                   Empresa empresa) {

        Produto produto = produtoRepository.findByIdAndEmpresa(produtoId, empresa)
            .orElseThrow(() -> new RuntimeException("Produto não encontrado."));

        // O histórico guarda a quantidade com sinal (saída negativa, entrada positiva), então o
        // saldo de um período é só a soma. Em venda, entrada e devolução o sinal vem do tipo
        // (por isso o abs); só o ajuste usa o número como veio.
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
        produtoRepository.save(produto);

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

        return movimentacaoRepository.save(mov);
    }

    @Transactional(readOnly = true)
    public List<Movimentacao> listar(Empresa empresa) {
        return movimentacaoRepository.findByEmpresaOrderByDataHoraDesc(empresa);
    }

    @Transactional(readOnly = true)
    public List<Movimentacao> listarPorTipo(Empresa empresa, TipoMovimentacao tipo) {
        return movimentacaoRepository.findByEmpresaAndTipoOrderByDataHoraDesc(empresa, tipo);
    }

    @Transactional(readOnly = true)
    public double receitaTotalVendas(Empresa empresa) {
        return movimentacaoRepository.somarReceitaVendas(empresa);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXPORTAÇÃO CSV
    // ══════════════════════════════════════════════════════════════════════════

    private static final Map<TipoMovimentacao, String> ROTULO_TIPO = Map.of(
        TipoMovimentacao.VENDA,      "Venda",
        TipoMovimentacao.ENTRADA,    "Entrada",
        TipoMovimentacao.AJUSTE,     "Ajuste",
        TipoMovimentacao.DEVOLUCAO,  "Devolução"
    );

    @Transactional(readOnly = true)
    public String exportarCsv(Empresa empresa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Data;Tipo;Produto;Quantidade;Valor Unitario;Valor Total;Responsavel;Motivo\r\n");
        for (Movimentacao m : listar(empresa)) {
            sb.append(m.getDataFormatada()).append(';')
              .append(ROTULO_TIPO.getOrDefault(m.getTipo(), String.valueOf(m.getTipo()))).append(';')
              .append(CsvUtil.campo(m.getNomeProduto())).append(';')
              .append(m.getQuantidade()).append(';')
              .append(CsvUtil.numero(m.getPrecoUnitario())).append(';')
              .append(CsvUtil.numero(m.getValorTotal())).append(';')
              .append(CsvUtil.campo(m.getResponsavel())).append(';')
              .append(CsvUtil.campo(m.getMotivo()))
              .append("\r\n");
        }
        return sb.toString();
    }
}
