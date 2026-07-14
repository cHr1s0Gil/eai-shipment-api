package com.eaishipment.config.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyProperties apiKeyProperties;

    public ApiKeyAuthenticationFilter(ApiKeyProperties apiKeyProperties) {
        this.apiKeyProperties = apiKeyProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isProtectedApi(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(apiKeyProperties.getHeaderName());

        if (!isValidApiKey(apiKey)) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"resultCode":"E","message":"Invalid API key","data":null}
                    """);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("api-key-user",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isValidApiKey(String apiKey) {
        return StringUtils.hasText(apiKey) && apiKey.equals(apiKeyProperties.getValue());
    }

    private boolean isProtectedApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }
}
