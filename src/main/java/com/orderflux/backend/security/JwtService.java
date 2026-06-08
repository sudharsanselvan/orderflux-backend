package com.orderflux.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtService — All JWT operations in one place.
 *
 * Responsibilities:
 *   1. Generate access tokens
 *   2. Extract claims (data) from tokens
 *   3. Validate tokens
 *
 * @Value("${jwt.secret}"):
 *   Spring reads this value from application.properties.
 *   Injects it into the field at startup.
 *   This is how you externalize configuration — not hardcoded.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Extract the subject (email) from the token.
     * The "subject" is the main identifier we store in the token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor using a Function.
     *
     * Claims = the payload of the JWT (all the data stored inside).
     * claimsResolver = a function that picks one specific claim.
     *
     * Examples:
     *   extractClaim(token, Claims::getSubject)     → gets email
     *   extractClaim(token, Claims::getExpiration)  → gets expiry date
     *
     * Function<Claims, T> is a functional interface:
     *   Takes Claims as input, returns T.
     *   This makes extractClaim reusable for any claim type.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generate a token with only the email as subject.
     * No extra claims needed for basic auth.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generate a token with extra claims (additional data in payload).
     *
     * Extra claims we'll add: role, userId
     * These let us read user info from the token without a DB call.
     *
     * Token structure we build:
     * {
     *   "sub": "user@example.com",    ← subject (email)
     *   "role": "ROLE_CUSTOMER",      ← extra claim
     *   "iat": 1716000000,            ← issued at (auto-set)
     *   "exp": 1716086400             ← expiration (auto-set)
     * }
     */
    public String generateToken(Map<String, Object> extraClaims,
                                UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())   // getUsername() returns email
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())             // sign with our secret
                .compact();                            // build the token string
    }

    /**
     * Validate token:
     *   1. Email in token matches the user we loaded from DB
     *   2. Token is not expired
     *
     * Why check email again?
     *   Defense in depth. Even if signature is valid,
     *   make sure the token belongs to THIS user.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // ─── Private helpers ──────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Parse and extract ALL claims from the token.
     *
     * This is where signature verification happens automatically.
     * If the signature is invalid or token is tampered → JwtException thrown.
     * Our GlobalExceptionHandler will catch it (we'll add that handler next).
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())     // use same key to verify
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Build the signing key from our base64-encoded secret.
     *
     * HMAC-SHA256 (HS256):
     *   A symmetric algorithm — same key signs AND verifies.
     *   Only your server knows this key.
     *   If someone else gets it, they can forge tokens.
     *   → Never commit it to GitHub. Use env variables in prod.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}