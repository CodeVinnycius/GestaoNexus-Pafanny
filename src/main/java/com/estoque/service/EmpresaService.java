package com.estoque.service;

import com.estoque.model.Empresa;
import com.estoque.repository.EmpresaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmpresaService {

    private static final int SENHA_MIN = 6;
    private static final int SENHA_MAX = 72; // BCrypt trunca em 72 bytes

    private final EmpresaRepository repository;
    private final PasswordEncoder encoder;

    public EmpresaService(EmpresaRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder    = encoder;
    }

    @Transactional(readOnly = true)
    public List<Empresa> listarTodas() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Empresa buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empresa não encontrada."));
    }

    public Empresa cadastrar(String nome, String login, String senha) {
        validarLogin(login);
        validarSenha(senha);

        if (repository.existsByLogin(login.trim()))
            throw new RuntimeException("Já existe uma empresa com esse login.");

        Empresa e = new Empresa(nome.trim(), login.trim(), encoder.encode(senha));
        return repository.save(e);
    }

    public Empresa atualizar(Long id, String nome, String login, String novaSenha, Boolean ativo) {
        Empresa e = buscarPorId(id);

        // Proteção: admin não pode ser desativado
        if (e.isAdmin() && Boolean.FALSE.equals(ativo))
            throw new IllegalArgumentException("Não é possível desativar a conta de administrador.");

        if (nome  != null && !nome.isBlank())  e.setNome(nome.trim());

        if (login != null && !login.isBlank()) {
            validarLogin(login);
            String loginNovo = login.trim();
            if (!loginNovo.equals(e.getLogin()) && repository.existsByLogin(loginNovo))
                throw new RuntimeException("Login já está em uso.");
            e.setLogin(loginNovo);
        }

        if (novaSenha != null && !novaSenha.isBlank()) {
            validarSenha(novaSenha);
            e.setSenha(encoder.encode(novaSenha));
        }

        // setAdmin() NUNCA é chamado aqui — nenhuma rota permite promover empresa
        if (ativo != null && !e.isAdmin())
            e.setAtivo(ativo);

        return repository.save(e);
    }

    public void remover(Long id) {
        Empresa e = buscarPorId(id);

        // Proteção: conta de admin nunca pode ser removida
        if (e.isAdmin())
            throw new IllegalArgumentException("Não é possível remover a conta de administrador.");

        repository.delete(e);
    }

    // ── Validações ────────────────────────────────────────────────────────────

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < SENHA_MIN)
            throw new IllegalArgumentException(
                "A senha deve ter no mínimo " + SENHA_MIN + " caracteres.");
        if (senha.length() > SENHA_MAX)
            throw new IllegalArgumentException(
                "A senha deve ter no máximo " + SENHA_MAX + " caracteres.");
    }

    private void validarLogin(String login) {
        String l = login == null ? "" : login.trim();
        if (l.length() < 3)
            throw new IllegalArgumentException("O login deve ter no mínimo 3 caracteres.");
        if (!l.matches("[a-zA-Z0-9_\\-\\.]+"))
            throw new IllegalArgumentException(
                "O login só pode conter letras, números, hifens, pontos e underscores.");
    }
}
