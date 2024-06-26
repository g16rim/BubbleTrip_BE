package com.ssafy.BonVoyage.auth.config.security.token;


import com.ssafy.BonVoyage.auth.advice.error.ExpiredTokenException;
import com.ssafy.BonVoyage.auth.advice.payload.ErrorCode;
import com.ssafy.BonVoyage.auth.service.CustomTokenProviderService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class CustomOncePerRequestFilter extends OncePerRequestFilter {

    @Autowired
    private CustomTokenProviderService customTokenProviderService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String[] tokens = getJwtFromRequest(request);
        try {
            if (StringUtils.hasText(tokens[0]) && customTokenProviderService.validateToken(tokens[0])) { // 어세스 토큰이 있고 토큰이 유효한 경우 (만료된 토큰의 예외를 잡아서 추가 처리)
                UsernamePasswordAuthenticationToken authentication = customTokenProviderService.getAuthenticationById(tokens[0]);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        catch (ExpiredTokenException e){
            log.info(e.getMessage());
            request.setAttribute("error", ErrorCode.EXPIRED_ACCESS_TOKEN);
        }

        filterChain.doFilter(request, response);
    }

    private String[] getJwtFromRequest(HttpServletRequest request) {
        String[] tokens = new String[2];

        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            log.info("accessToken = {}", bearerToken.substring(7, bearerToken.length()));
            tokens[0] = bearerToken.substring(7, bearerToken.length());
        }
        String refreshToken = request.getHeader("Authorization_refresh");
        if (StringUtils.hasText(refreshToken) && refreshToken.startsWith("Bearer")) {
            log.info("refreshToken = {}", refreshToken.substring(7, refreshToken.length()));
            tokens[1] = refreshToken.substring(7, refreshToken.length());
        }
        return tokens;
    }

}