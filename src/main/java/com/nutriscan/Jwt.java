package com.nutriscan;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class Jwt {
    private final SecretKey key;

    public Jwt(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String sign(String userId) {
        return Jwts.builder().subject(userId)
                .expiration(new Date(System.currentTimeMillis() + 7L * 24 * 3600 * 1000))
                .signWith(key).compact();
    }

    public String verify(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
