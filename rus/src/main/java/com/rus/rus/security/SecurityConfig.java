package com.rus.rus.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rus.rus.common.ErrorResponseDTO;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionFilter jwtExceptionFilter;

    @Value("${security.swagger.enabled:false}")
    private boolean swaggerEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF(Cross-Site Request Forgery) 비활성화
                .csrf(csrf -> csrf.disable())

                // API 서버이므로 기본 로그인 폼 및 HTTP Basic 인증 비활성화
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())

                // 세션 관리 정책을 STATELESS로 설정 (세션을 사용하지 않음)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 요청에 대한 인가 규칙 설정
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/", "/api", "/error", "/favicon.ico", "/actuator/**").permitAll(); // 특정 경로는 인증 없이 허용

            // JWT토큰이 필요 없는 API의 경우, 이곳에 추가해야 정상적으로 요청이 들어갑니다
            auth.requestMatchers("/auth/signup",
                    "/routine/recommend",
                    "/routine",
                    "/routine/challenge",
                    "/routine/collections",
                    "/routine/collections/**",
                    "/users/ranking",
                    "/shop/products",
                    "/shop/products/**",
                    "/kfood/detect").permitAll();

            if (swaggerEnabled) {
                // Swagger UI 및 API docs에 인증 없이 접근 허용
                auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll();
            } else {
                // 안돼 안 열어줘
                auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").denyAll();
            }
            auth.anyRequest().authenticated(); // 나머지 모든 요청은 인증 필요
        });

        // exceptionHandling추가
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler()));

        // 직접 구현한 JWT 필터를 기본 필터 앞에 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            setErrorResponse(HttpStatus.UNAUTHORIZED, response, "Unauthorized: " + authException.getMessage());
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            setErrorResponse(HttpStatus.FORBIDDEN, response, "Forbidden: " + accessDeniedException.getMessage());
        };
    }

    private void setErrorResponse(HttpStatus status, HttpServletResponse response, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json; charset=UTF-8");
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(status.value(), message);
        String jsonResponse = new ObjectMapper().writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
