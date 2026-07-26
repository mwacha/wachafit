package com.github.mwacha.wachafit.shared.security;

import com.github.mwacha.wachafit.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtUtil.isTokenValid(token)) {
            try {
                UUID tenantId = jwtUtil.extractTenantId(token);
                UUID accountId = jwtUtil.extractAccountId(token);
                if (tenantId == null || accountId == null) {
                    // Token sem claim tenantId ou accountId: emitido antes de uma das migrações
                    // multi-tenant/conta-única. Não autenticar -- do contrário o
                    // TenantFilterAspect/fluxo de troca de academia não teriam o que precisam.
                    log.warn("JWT sem claim tenantId/accountId rejeitado (token de versão anterior)");
                } else {
                    String userId = jwtUtil.extractUserId(token).toString();
                    UserDetails user = userDetailsService.loadUserByUsername(userId);
                    if (user.isEnabled()) {
                        UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        TenantContext.set(tenantId);
                    }
                }
            } catch (Exception e) {
                // Token was valid but user lookup failed (deleted user, DB error, malformed claim).
                // Leave SecurityContext empty — Spring Security will return 401 via AuthenticationEntryPoint.
                log.debug("Could not authenticate from JWT token: {}", e.getMessage());
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
