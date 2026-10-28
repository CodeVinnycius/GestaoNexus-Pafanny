package com.estoque;

import com.estoque.model.Empresa;
import com.estoque.model.Produto;
import com.estoque.repository.EmpresaRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Vendas, entradas e ajustes pela API, do jeito que a tela de movimentações usa. */
@SpringBootTest
@AutoConfigureMockMvc
class MovimentacaoApiTest {

    @Autowired MockMvc mvc;
    @Autowired EmpresaRepository empresas;
    @Autowired ProdutoRepository produtos;
    @Autowired JwtUtil jwt;

    Empresa lojaA;
    Empresa lojaB;

    @BeforeEach
    void criarEmpresas() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        lojaA = empresas.save(new Empresa("Loja A", "mov-a-" + sufixo, "sem-login"));
        lojaB = empresas.save(new Empresa("Loja B", "mov-b-" + sufixo, "sem-login"));
    }

    @Test
    void receitaDasVendasEPositiva() throws Exception {
        Produto p = produtos.save(produtoDe(lojaA, "Pijama vendido", 5, 10.0));

        registrar(lojaA, p.getId(), "VENDA", 2).andExpect(status().isOk());

        mvc.perform(como(lojaA, get("/api/movimentacoes/receita")))
            .andExpect(jsonPath("$.receita").value(20.0));
    }

    @Test
    void dadosFaltandoOuErradosExplicamOProblema() throws Exception {
        mvc.perform(como(lojaA, post("/api/movimentacoes")).content("{\"tipo\": \"VENDA\", \"quantidade\": 1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Informe o produto, o tipo e a quantidade."));

        mvc.perform(como(lojaA, post("/api/movimentacoes"))
                .content("{\"produtoId\": 1, \"tipo\": \"VENDA\", \"quantidade\": \"dois\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Produto e quantidade precisam ser números inteiros."));

        mvc.perform(como(lojaA, post("/api/movimentacoes"))
                .content("{\"produtoId\": 1, \"tipo\": \"TROCA\", \"quantidade\": 1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", containsString("Tipo inválido")));
    }

    @Test
    void ajustePodeSerNegativoMasVendaNao() throws Exception {
        Produto p = produtos.save(produtoDe(lojaA, "Pijama ajustado", 5, 10.0));

        registrar(lojaA, p.getId(), "AJUSTE", -2).andExpect(status().isOk());
        assertThat(quantidadeDe(p)).isEqualTo(3);

        registrar(lojaA, p.getId(), "VENDA", -2).andExpect(status().isBadRequest());
        registrar(lojaA, p.getId(), "AJUSTE", 0).andExpect(status().isBadRequest());
        assertThat(quantidadeDe(p)).isEqualTo(3);
    }

    @Test
    void vendaMaiorQueOEstoqueERecusada() throws Exception {
        Produto p = produtos.save(produtoDe(lojaA, "Pijama quase esgotado", 1, 10.0));

        registrar(lojaA, p.getId(), "VENDA", 2)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", containsString("Estoque insuficiente")));
        assertThat(quantidadeDe(p)).isEqualTo(1);
    }

    @Test
    void produtoDeOutraEmpresaNaoAparece() throws Exception {
        Produto daA = produtos.save(produtoDe(lojaA, "Pijama só da A", 5, 10.0));

        registrar(lojaB, daA.getId(), "VENDA", 1).andExpect(status().isNotFound());
        assertThat(quantidadeDe(daA)).isEqualTo(5);
    }

    private ResultActions registrar(Empresa empresa, Long produtoId, String tipo, int quantidade) throws Exception {
        return mvc.perform(como(empresa, post("/api/movimentacoes")).content(
            "{\"produtoId\": %d, \"tipo\": \"%s\", \"quantidade\": %d, \"responsavel\": \"Teste\"}"
                .formatted(produtoId, tipo, quantidade)));
    }

    private int quantidadeDe(Produto p) {
        return produtos.findById(p.getId()).orElseThrow().getQuantidade();
    }

    private MockHttpServletRequestBuilder como(Empresa empresa, MockHttpServletRequestBuilder req) {
        return req.header("Authorization", "Bearer " + jwt.gerarToken(empresa.getId(), empresa.getLogin(), false))
                  .contentType(MediaType.APPLICATION_JSON);
    }

    private static Produto produtoDe(Empresa empresa, String nome, int quantidade, Double precoVenda) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setEmpresa(empresa);
        p.setQuantidade(quantidade);
        p.setPrecoVenda(precoVenda);
        return p;
    }
}
