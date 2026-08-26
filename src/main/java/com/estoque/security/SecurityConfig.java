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
                // Arquivos públicos — apenas estáticos necessários para o login
                .requestMatchers(
                    "/", "/login.html",
                    "/css/**", "/js/**", "/icons/**",
                    "/manifest.json", "/sw.js"
                ).permitAll()

                // Loja pública — catálogo, fotos de produto e a própria página
                .requestMatchers(
                    "/loja.html", "/uploads/**",
                    "/api/loja/**"
                ).permitAll()

                // Auth: público
                .requestMatchers("/api/auth/login").permitAll()

                // index.html e admin.html: exigem token válido (verificado pelo JS)
                // O back-end protege os dados; as páginas HTML são abertas mas inúteis sem token
                .requestMatchers("/index.html", "/admin.html").permitAll()

                // Admin: apenas ROLE_ADMIN — verificado no back-end
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Dados da empresa: exige autenticação
                .requestMatchers("/api/produtos/**").hasRole("EMPRESA")
                .requestMatchers("/api/movimentacoes/**").hasRole("EMPRESA")

                // Tudo mais: autenticado
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS restrito: apenas origem local em dev.
     * Em produção, ajuste para o domínio real do front-end.
     * NUNCA use allowedOrigins("*") com JWT.
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
        // Força 12 — mais seguro que o padrão 10
        return new BCryptPasswordEncoder(12);
    }
}
