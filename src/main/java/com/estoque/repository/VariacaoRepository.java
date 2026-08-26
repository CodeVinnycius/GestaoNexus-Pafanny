package com.estoque.repository;

import com.estoque.model.Produto;
import com.estoque.model.Variacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VariacaoRepository extends JpaRepository<Variacao, Long> {

    Optional<Variacao> findByIdAndProduto(Long id, Produto produto);
}
