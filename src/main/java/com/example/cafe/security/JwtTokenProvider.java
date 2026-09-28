package com.example.cafe.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
  @Value("${JWT_SECRET}")
  private String JWT_SECRET;
  
  @Value("${JWT_EXPIRATION}")
  private long JWT_EXPIRATION;

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
  }

  // 1. TẠO (Token) khi khách đăng nhập thành công
  public String generateToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);
    
    return Jwts.builder().setSubject(username).setIssuedAt(now).setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
  }

  // 2. Trích xuất tên khách (username) từ tờ vé (Token) khách đưa
  public String getUsernameFromToken(String token) {
    Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
        .parseClaimsJws(token).getBody();
    return claims.getSubject();
  }

  // 3. KIỂM VÉ: Kiểm tra vé thật hay vé giả, còn hạn hay hết hạn
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
