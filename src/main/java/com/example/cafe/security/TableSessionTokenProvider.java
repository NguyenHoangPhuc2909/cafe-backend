package com.example.cafe.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.sql.Date;
import java.time.Instant;


@Component
public class TableSessionTokenProvider {
  private final Key signingKey;

  public TableSessionTokenProvider(@Value("${TABLE_SESSION_JWT_SECRET}") String secret) {
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  public String generateToken(Long sessionId, Long tableId, Instant expiresAt) {
    Instant now = Instant.now();

    if (!expiresAt.isAfter(now)) {
      throw new IllegalArgumentException("Thời điểm hết hạn phải ở tương lai");
    }

    return Jwts.builder()
        .setSubject(sessionId.toString())
        .claim("tableId", tableId)
        .setIssuer("cafe-backend")
        .setAudience("table-session")
        .setIssuedAt(Date.from(now))
        .setExpiration(java.util.Date.from(expiresAt))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(signingKey)
        .requireIssuer("cafe-backend")
        .requireAudience("table-session")
        .build()
        .parseClaimsJws(token)
        .getBody();

    if (claims.getSubject() == null || claims.getExpiration() == null
        || !(claims.get("tableId") instanceof Number)) {
      throw new JwtException("Token phiên thiếu thông tin");
    }

    return claims;
  }
}
