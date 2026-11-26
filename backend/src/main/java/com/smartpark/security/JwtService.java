package com.smartpark.security;

import com.smartpark.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Izdaje i proverava JWT tokene (FR-02, spec. 8.1: kratak rok vazenja).
 * Tajna se cita iz promenljive okruzenja JWT_SECRET i nikad nije u gitu.
 */
@Service
public class JwtService {

    static final String CLAIM_ROLE = "role";
    static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${jwt.secret:}") String secret,
                      @Value("${jwt.expiration-minutes:60}") long expirationMinutes) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET nije postavljen. Generisi ga (openssl rand -base64 48) i upisi u .env.");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET mora imati bar 32 bajta (256 bita) posle Base64 dekodiranja.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().getName().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * Vraca ID korisnika iz tokena.
     *
     * @throws JwtException ako je token neispravan, istekao ili nije potpisan nasim kljucem
     */
    public Long extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    /** Trajanje tokena u sekundama - salje se klijentu u odgovoru na prijavu. */
    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }
}
