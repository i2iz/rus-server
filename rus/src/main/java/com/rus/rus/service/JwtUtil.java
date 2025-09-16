package com.rus.rus.service;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.Key;

@Component
public class JwtUtil {

    @Value("${supabase.jwks.url}")
    private String jwksUrl;

    // Supabase의 JWKS 엔드포인트에서 공개키를 가져와 토큰 서명을 검증하는 리졸버
    // ✅ SigningKeyResolverAdapter를 직접 사용하도록 수정
    private final SigningKeyResolver signingKeyResolver = new SigningKeyResolver() {
        private JwkProvider provider;

        @Override
        public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {
            try {
                // JwkProvider를 한 번만 초기화합니다.
                if (this.provider == null) {
                    this.provider = new UrlJwkProvider(new URL(jwksUrl));
                }
                // 토큰 헤더에서 kid(Key ID)를 가져와서 일치하는 공개키를 JWKS 목록에서 찾습니다.
                Jwk jwk = provider.get(jwsHeader.getKeyId());
                return jwk.getPublicKey();
            } catch (Exception e) {
                // 이 예외는 JwtExceptionFilter에서 처리됩니다.
                throw new RuntimeException("서명 키를 가져오거나 검증하는 데 실패했습니다.", e);
            }
        }

        @Override
        public Key resolveSigningKey(JwsHeader jwsHeader, String plaintext) {
            // 이 메서드는 사용하지 않으므로 기본 구현을 유지합니다.
            return null;
        }
    };

    /**
     * 토큰에서 모든 Claim을 추출합니다.
     * 이 과정에서 signingKeyResolver를 통해 서명이 자동으로 검증됩니다.
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKeyResolver(signingKeyResolver)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰에서 사용자 UID(sub 클레임)를 추출합니다.
     */
    public String getUidFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * 토큰 유효성을 검증합니다.
     * 참고: getUidFromToken을 호출하면 이미 내부적으로 검증이 수행됩니다.
     */
    public Boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
