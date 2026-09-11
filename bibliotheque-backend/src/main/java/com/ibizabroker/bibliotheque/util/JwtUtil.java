package com.ibizabroker.bibliotheque.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Génération et validation des tokens JWT.
 * Le secret et la durée de validité ne sont jamais codés en dur :
 * ils viennent de application.properties (JWT_SECRET, JWT_VALIDITY_SECONDS).
 */
@Component
public class JwtUtil {

    private static final long MILLIS_PAR_SECONDE = 1000L;

    private final String secret;
    private final long validiteSecondes;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.validity-seconds}") long validiteSecondes) {
        this.secret = secret;
        this.validiteSecondes = validiteSecondes;
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public String generateToken(UserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validiteSecondes * MILLIS_PAR_SECONDE))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
}