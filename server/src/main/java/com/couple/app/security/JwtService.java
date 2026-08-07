package com.couple.app.security;

import com.couple.app.config.CoupleProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final CoupleProperties properties;
    private final SecretKey key;

    public JwtService(CoupleProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String createToken(Long partnerId, String partnerKey, String displayName) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getJwt().getExpireHours(), ChronoUnit.HOURS);
        return Jwts.builder()
                .subject(String.valueOf(partnerId))
                .claim("partnerKey", partnerKey)
                .claim("displayName", displayName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
