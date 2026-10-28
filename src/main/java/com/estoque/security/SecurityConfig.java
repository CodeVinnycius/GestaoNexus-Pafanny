package com.estoque.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .contentTypeOptions(c -> {})
                .referrerPolicy(r -> r.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .authorizeHttpRequests(auth -> auth
                // Arquivos estáticos públicos: tela de login, PWA, ícones e o que o Google lê
                .requestMatchers(
                    "/", "/login.html",
                    "/css/**", "/js/**", "/icons/**", "/img/**", "/fonts/**",
                    "/manifest.json", "/sw.js",
                    "/robots.txt", "/sitemap.xml", "/favicon.ico"
                ).permitAll()

                // Loja pública — catálogo, fotos de produto e a própria página
                .requestMatchers(
                    "/loja.html", "/uploads/**", "/miniaturas/**",
                    "/api/loja/**"
                ).permitAll()

                .requestMatchers("/api/auth/login").permitAll()

                // O HTML do painel abre sem login, mas não mostra nada sem token: quem protege
                // os dados é a API. O JS só redireciona para o login.
                .requestMatchers("/index.html", "/admin.html").permitAll()

                // Empresas, backup e restauração
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Aqui só se exige estar logado; a separação entre empresas fica nos services,
                // que recebem a empresa logada em cada consulta.
                .requestMatchers("/api/produtos/**").hasRole("EMPRESA")
                .requestMatchers("/api/movimentacoes/**").hasRole("EMPRESA")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Em produção o front e a API saem do mesmo domínio, então o navegador nem usa CORS.
     * Isto só existe para desenvolver com o front aberto em outra porta local.
     * Liberar "*" deixaria qualquer site chamar a API com o token de quem estiver logado.
     */
    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Custo 12 em vez do padrão 10: cada tentativa fica 4x mais cara para quem tenta
        // adivinhar senhas, e o login continua rápido para quem acerta.
        return new BCryptPasswordEncoder(12);
    }
}
