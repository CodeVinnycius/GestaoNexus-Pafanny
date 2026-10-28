package com.estoque;

import com.estoque.model.Empresa;
import com.estoque.repository.EmpresaRepository;
import com.estoque.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cadastro e edição de empresas pelo painel do admin. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminApiTest {

    @Autowired MockMvc mvc;
    @Autowired EmpresaRepository empresas;
    @Autowired JwtUtil jwt;

    @Value("${app.admin.login}") String adminLogin;

    @Test
    void campoComTipoErradoNaoMudaNadaNemMostraErroInterno() throws Exception {
        Empresa loja = empresas.save(new Empresa("Loja", "admin-teste-" + UUID.randomUUID().toString().substring(0, 8), "x"));

        mvc.perform(put("/api/admin/empresas/" + loja.getId()).header("Authorization", tokenDoAdmin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\": \"sim\", \"nome\": 42}"))
            .andExpect(status().isOk());

        Empresa depois = empresas.findById(loja.getId()).orElseThrow();
        assertThat(depois.isAtivo()).isTrue();
        assertThat(depois.getNome()).isEqualTo("Loja");
    }

    @Test
    void loginRepetidoExplicaOProblema() throws Exception {
        String login = "repetido-" + UUID.randomUUID().toString().substring(0, 8);
        String corpo = "{\"nome\": \"Loja\", \"login\": \"%s\", \"senha\": \"senha-boa\"}".formatted(login);

        mvc.perform(post("/api/admin/empresas").header("Authorization", tokenDoAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/empresas").header("Authorization", tokenDoAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("Já existe uma empresa com esse login."));
    }

    private String tokenDoAdmin() {
        Empresa admin = empresas.findByLogin(adminLogin).orElseThrow();
        return "Bearer " + jwt.gerarToken(admin.getId(), admin.getLogin(), true);
    }
}
