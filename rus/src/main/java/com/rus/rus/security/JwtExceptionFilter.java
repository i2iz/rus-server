package com.rus.rus.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rus.rus.common.ErrorResponseDTO;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

@Component
public class JwtExceptionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 다음 필터(JwtAuthenticationFilter) 실행
            filterChain.doFilter(request, response);
        } catch (JwtException ex) {
            // JwtAuthenticationFilter에서 발생한 예외를 여기서 처리
            setErrorResponse(HttpStatus.UNAUTHORIZED, response, ex);
        }
    }

    private void setErrorResponse(HttpStatus status, HttpServletResponse response, Throwable ex) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json; charset=UTF-8");

        // ErrorResponseDTO를 사용하여 응답 본문 생성
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(status.value(), ex.getMessage());

        // ObjectMapper를 사용하여 DTO를 JSON 문자열로 변환
        ObjectMapper mapper = new ObjectMapper();
        String jsonResponse = mapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }
}
