package com.estoque.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas de login erradas (força bruta) em memória.
 * Conta falhas por IP e por login numa janela de 15 minutos; passou do limite, bloqueia até a janela
 * expirar. Um login correto zera a contagem daquele IP e daquele login.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FALHAS_POR_IP    = 8;
    private static final int MAX_FALHAS_POR_LOGIN = 30;
    private static final long JANELA_MS = Duration.ofMinutes(15).toMillis();

    private static final class Contador {
        int falhas;
        long inicioJanela;
    }

    private final ConcurrentHashMap<String, Contador> porIp    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Contador> porLogin = new ConcurrentHashMap<>();

    public boolean bloqueado(String ip, String login) {
        long agora = System.currentTimeMillis();
        return excedeu(porIp, ip, MAX_FALHAS_POR_IP, agora)
            || excedeu(porLogin, login.toLowerCase(), MAX_FALHAS_POR_LOGIN, agora);
    }

    public void registrarFalha(String ip, String login) {
        long agora = System.currentTimeMillis();
        incrementar(porIp, ip, agora);
        incrementar(porLogin, login.toLowerCase(), agora);
        limparExpirados(agora);
    }

    public void registrarSucesso(String ip, String login) {
        porIp.remove(ip);
        porLogin.remove(login.toLowerCase());
    }

    private boolean excedeu(ConcurrentHashMap<String, Contador> mapa, String chave, int max, long agora) {
        Contador c = mapa.get(chave);
        if (c == null) return false;
        synchronized (c) {
            if (agora - c.inicioJanela > JANELA_MS) return false;
            return c.falhas >= max;
        }
    }

    private void incrementar(ConcurrentHashMap<String, Contador> mapa, String chave, long agora) {
        Contador c = mapa.computeIfAbsent(chave, k -> new Contador());
        synchronized (c) {
            if (agora - c.inicioJanela > JANELA_MS) {
                c.falhas = 0;
                c.inicioJanela = agora;
            }
            c.falhas++;
        }
    }

    private void limparExpirados(long agora) {
        if (porIp.size() + porLogin.size() < 5000) return;
        porIp.entrySet().removeIf(e -> agora - e.getValue().inicioJanela > JANELA_MS);
        porLogin.entrySet().removeIf(e -> agora - e.getValue().inicioJanela > JANELA_MS);
    }
}
