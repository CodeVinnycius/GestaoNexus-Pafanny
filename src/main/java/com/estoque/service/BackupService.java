package com.estoque.service;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Gera um backup consistente do banco H2 mesmo com o servidor no ar (comando nativo BACKUP TO). */
@Service
public class BackupService {

    private final DataSource dataSource;

    public BackupService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public byte[] gerarBackup() {
        try {
            Path temp = Files.createTempFile("estoque-backup-", ".zip");
            try {
                String caminho = temp.toAbsolutePath().toString().replace("'", "''");
                try (Connection conn = dataSource.getConnection();
                     Statement st = conn.createStatement()) {
                    st.execute("BACKUP TO '" + caminho + "'");
                }
                return Files.readAllBytes(temp);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Erro ao gerar backup do banco: " + e.getMessage(), e);
        }
    }
}
