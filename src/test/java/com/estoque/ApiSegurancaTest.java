package com.estoque;

import com.estoque.model.Empresa;
import com.estoque.model.Produto;
import com.estoque.repository.EmpresaRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.security.JwtUtil;
import com.estoque.service.EmpresaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * O que impede uma empresa de mexer nos dados de outra, testado pela API como o painel usa.
 * Cada teste cria as próprias empresas, com login único.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiSegurancaTest {

    @Autowired MockMvc mvc;
    @Autowired EmpresaRepository empresas;
    @Autowired EmpresaService empresaService;
    @Autowired ProdutoRepository produtos;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwt;

    @Value("${app.uploads.dir}") String uploadsDir;

    Empresa lojaA;
    Empresa lojaB;

    @BeforeEach
    void criarEmpresas() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        lojaA = empresas.save(new Empresa("Loja A", "loja-a-" + sufixo, encoder.encode("senha-da-a")));
        lojaB = empresas.save(new Empresa("Loja B", "loja-b-" + sufixo, encoder.encode("senha-da-b")));
    }

    @Test
    void cadastroIgnoraIdFotosEVariacoesVindosDoCliente() throws Exception {
        Produto daA = produtoDe(lojaA, "Pijama da A", 3, 50.0);
        daA.getImagens().add("/uploads/produtos/foto-da-a.jpg");
        daA = produtos.save(daA);

        mvc.perform(como(lojaB, post("/api/produtos")).content("""
                {"id": %d, "nome": "Pijama da B", "quantidade": 1, "precoCusto": 10,
                 "imagens": ["/uploads/produtos/foto-da-a.jpg"],
                 "variacoes": [{"tamanho": "M", "quantidade": 9}]}
                """.formatted(daA.getId())))
            .andExpect(status().isCreated());

        Produto aindaDaA = produtos.findById(daA.getId()).orElseThrow();
        assertThat(aindaDaA.getNome()).isEqualTo("Pijama da A");
        assertThat(aindaDaA.getEmpresa().getId()).isEqualTo(lojaA.getId());

        mvc.perform(como(lojaB, get("/api/produtos")))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(not(daA.getId().intValue())))
            .andExpect(jsonPath("$[0].imagens").isEmpty())
            .andExpect(jsonPath("$[0].variacoes").isEmpty());
    }

    @Test
    void removerFotoQueNaoEDoProdutoNaoApagaOArquivo() throws Exception {
        Path pasta = Path.of(uploadsDir, "produtos");
        Files.createDirectories(pasta);
        Path fotoDaA = Files.writeString(pasta.resolve(UUID.randomUUID() + ".jpg"), "foto");
        String url = "/uploads/produtos/" + fotoDaA.getFileName();

        Produto daA = produtoDe(lojaA, "Pijama com foto", 1, 50.0);
        daA.getImagens().add(url);
        produtos.save(daA);
        Produto daB = produtos.save(produtoDe(lojaB, "Pijama sem foto", 1, 50.0));

        mvc.perform(como(lojaB, delete("/api/produtos/" + daB.getId() + "/imagens").param("url", url)))
            .andExpect(status().isNotFound());

        assertThat(fotoDaA).exists();
    }

    @Test
    void tokenDeLoginRenomeadoNaoEntraNaContaDeQuemPegouOLogin() throws Exception {
        String loginAntigo = lojaA.getLogin();
        String tokenAntigo = jwt.gerarToken(lojaA.getId(), loginAntigo, false);

        empresaService.atualizar(lojaA.getId(), null, loginAntigo + "-novo", null, null);
        Empresa outra = empresas.save(new Empresa("Outra", loginAntigo, encoder.encode("x")));
        produtos.save(produtoDe(outra, "Pijama da outra", 1, 10.0));

        mvc.perform(get("/api/produtos").header("Authorization", "Bearer " + tokenAntigo))
            .andExpect(status().isForbidden());
    }

    @Test
    void tokenComAdminFalsoNaoAbreOPainelDoAdmin() throws Exception {
        String tokenForjadoComChaveValida = jwt.gerarToken(lojaA.getId(), lojaA.getLogin(), true);

        mvc.perform(get("/api/admin/empresas").header("Authorization", "Bearer " + tokenForjadoComChaveValida))
            .andExpect(status().isForbidden());
    }

    @Test
    void empresaInativaComSenhaErradaRecebeAMesmaRespostaDeSenhaErrada() throws Exception {
        empresaService.atualizar(lojaA.getId(), null, null, null, false);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"%s\", \"senha\": \"chute\"}".formatted(lojaA.getLogin())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.erro").value("Login ou senha incorretos."));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"%s\", \"senha\": \"senha-da-a\"}".formatted(lojaA.getLogin())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.erro", containsString("inativa")));
    }

    @Test
    void loginCorretoDevolveTokenQueFunciona() throws Exception {
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"%s\", \"senha\": \"senha-da-b\"}".formatted(lojaB.getLogin())))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = resposta.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/produtos").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Pijama da A"))));
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
