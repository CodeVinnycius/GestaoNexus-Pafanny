package com.estoque.controller;

import com.estoque.model.Empresa;
import com.estoque.service.BackupService;
import com.estoque.service.EmpresaService;
import com.estoque.service.RestauracaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/empresas")
public class AdminController {

    private final EmpresaService service;
    private final BackupService backupService;

    @Value("${app.data.dir:./data}")
    private String dadosDir;

    public AdminController(EmpresaService service, BackupService backupService) {
        this.service = service;
        this.backupService = backupService;
    }

    @GetMapping
    public ResponseEntity<List<Empresa>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody Map<String, String> body) {
        try {
            String nome  = body.get("nome");
            String login = body.get("login");
            String senha = body.get("senha");

            if (nome == null || login == null || senha == null ||
                nome.isBlank() || login.isBlank() || senha.isBlank())
                return ResponseEntity.badRequest().body(Map.of("erro", "Nome, login e senha são obrigatórios."));

            Empresa e = service.cadastrar(nome.trim(), login.trim(), senha);
            return ResponseEntity.status(HttpStatus.CREATED).body(e);

        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        try {
            String  nome     = (String)  body.get("nome");
            String  login    = (String)  body.get("login");
            String  senha    = (String)  body.get("senha");
            Boolean ativo    = body.get("ativo") != null ? (Boolean) body.get("ativo") : null;

            Empresa e = service.atualizar(id, nome, login, senha, ativo);
            return ResponseEntity.ok(e);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remover(@PathVariable Long id) {
        try {
            service.remover(id);
            return ResponseEntity.ok(Map.of("mensagem", "Empresa removida com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /** Backup completo do banco (todas as empresas) — só o admin pode baixar. */
    @GetMapping("/backup")
    public ResponseEntity<byte[]> backup() {
        byte[] zip = backupService.gerarBackup();
        String nomeArquivo = "estoque-backup-" + LocalDate.now() + ".zip";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(zip);
    }

    /**
     * Recebe um .zip (estoque.mv.db + uploads/) e o deixa pendente; a troca acontece no próximo
     * reinício do serviço. Só o admin. O banco atual vira estoque.mv.db.bak-AAAAMMDD-HHMMSS.
     */
    @PostMapping("/restaurar")
    public ResponseEntity<?> restaurar(@RequestParam("arquivo") MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("erro", "Nenhum arquivo enviado."));
        try {
            Path destino = RestauracaoService.arquivoPendente(Path.of(dadosDir)).toAbsolutePath();
            Files.createDirectories(destino.getParent());
            arquivo.transferTo(destino);
            try {
                RestauracaoService.validar(destino);
            } catch (IllegalArgumentException e) {
                Files.deleteIfExists(destino);
                return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
            }
            return ResponseEntity.ok(Map.of("mensagem",
                "Arquivo recebido. Reinicie o serviço para aplicar a restauração."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Falha ao salvar o arquivo."));
        }
    }
}
