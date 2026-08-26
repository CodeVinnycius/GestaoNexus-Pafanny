package com.estoque.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey chave;
    private final long expiracao;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration}") long expiracao) {
        byte[] bytes = Base64.getDecoder().decode(secret);
        this.chave     = Keys.hmacShaKeyFor(bytes);
        this.expiracao = expiracao;
    }

    public String gerarToken(Long empresaId, String login, boolean admin) {
        return Jwts.builder()
            .subject(login)
            .claim("empresaId", empresaId)
            .claim("admin", admin)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiracao))
            .signWith(chave)
            .compact();
    }

    public Claims extrairClaims(String token) {
        return Jwts.parser()
            .verifyWith(chave)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validar(String token) {
        try { extrairClaims(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }

    public String extrairLogin(String token)   { return extrairClaims(token).getSubject(); }
    public Long extrairEmpresaId(String token) { return extrairClaims(token).get("empresaId", Long.class); }
    public boolean extrairAdmin(String token)  { return Boolean.TRUE.equals(extrairClaims(token).get("admin", Boolean.class)); }
}
