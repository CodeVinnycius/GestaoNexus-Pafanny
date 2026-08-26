package com.estoque.security;

import com.estoque.model.Empresa;
import com.estoque.repository.EmpresaRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final EmpresaRepository empresaRepository;

    public JwtFilter(JwtUtil jwtUtil, EmpresaRepository empresaRepository) {
        this.jwtUtil           = jwtUtil;
        this.empresaRepository = empresaRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validar(token)) {
                String login = jwtUtil.extrairLogin(token);
                boolean admin = jwtUtil.extrairAdmin(token);

                Empresa empresa = empresaRepository.findByLogin(login).orElse(null);
                if (empresa != null && empresa.isAtivo()) {
                    List<SimpleGrantedAuthority> roles = admin
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                                  new SimpleGrantedAuthority("ROLE_EMPRESA"))
                        : List.of(new SimpleGrantedAuthority("ROLE_EMPRESA"));

                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(empresa, null, roles);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        chain.doFilter(req, res);
    }
}
