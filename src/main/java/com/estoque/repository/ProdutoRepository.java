package com.estoque.repository;

import com.estoque.model.Empresa;
import com.estoque.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByEmpresa(Empresa empresa);

    List<Produto> findByEmpresaAndNomeContainingIgnoreCase(Empresa empresa, String termo);

    Optional<Produto> findByIdAndEmpresa(Long id, Empresa empresa);

    boolean existsByNomeIgnoreCaseAndEmpresa(String nome, Empresa empresa);

    @Query("SELECT COALESCE(SUM(p.precoCusto * p.quantidade), 0) FROM Produto p WHERE p.empresa = :empresa")
    double somarCustoTotal(Empresa empresa);

    @Query("SELECT COALESCE(SUM(COALESCE(p.precoVenda, 0) * p.quantidade), 0) FROM Produto p WHERE p.empresa = :empresa")
    double somarValorVenda(Empresa empresa);
}
