package com.estoque.repository;

import com.estoque.model.Empresa;
import com.estoque.model.Movimentacao;
import com.estoque.model.Produto;
import com.estoque.model.TipoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    List<Movimentacao> findByEmpresaOrderByDataHoraDesc(Empresa empresa);

    List<Movimentacao> findByEmpresaAndTipoOrderByDataHoraDesc(Empresa empresa, TipoMovimentacao tipo);

    /** Movimentações que ainda referenciam este produto (usado para "soltar" a referência antes de remover). */
    List<Movimentacao> findByProduto(Produto produto);

    /** Remove todo o histórico de uma empresa — usado só quando a empresa inteira está sendo excluída. */
    void deleteByEmpresa(Empresa empresa);

    @Query("SELECT COALESCE(SUM(m.quantidade * m.precoUnitario), 0) " +
           "FROM Movimentacao m WHERE m.empresa = :empresa AND m.tipo = 'VENDA'")
    double somarReceitaVendas(Empresa empresa);
}
