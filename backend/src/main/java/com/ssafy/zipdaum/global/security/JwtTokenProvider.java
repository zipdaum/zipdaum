package com.ssafy.zipdaum.global.security;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.user.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final SecretKey secretKey;
  private final long accessTokenExpiration;

  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-expiration}") long accessTokenExpiration
  ) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiration = accessTokenExpiration;
  }

  public String createAccessToken(UserDto user) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + accessTokenExpiration);

    return Jwts.builder()
        .subject(user.getEmail())
        .claim("userId", user.getId())
            .claim("role", user.getRole())
        .issuedAt(now)
        .expiration(expiration)
        .signWith(secretKey)
        .compact();
  }

  public AuthenticatedUser getAuthenticatedUser(String token) {
    Claims claims = parseClaims(token);
    Object userIdClaim = claims.get("userId");

    Long userId = Long.valueOf(String.valueOf(userIdClaim));
    String email = claims.getSubject();
    String role = claims.get("role").toString();

    return new AuthenticatedUser(userId, email, role);
  }

  private Claims parseClaims(String token) {
    try {
      return Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException e) {
      throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    } catch (JwtException | IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.TOKEN_INVALID);
    }
  }
}
