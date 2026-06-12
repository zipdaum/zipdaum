package com.ssafy.zipdaum.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.global.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String token = resolveToken(request);

    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      AuthenticatedUser authenticatedUser = jwtTokenProvider.getAuthenticatedUser(token);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              authenticatedUser,
              null,
              Collections.emptyList()
          );

      SecurityContextHolder.getContext().setAuthentication(authentication);

      filterChain.doFilter(request, response);
    } catch (BusinessException e) {
      SecurityContextHolder.clearContext();
      writeErrorResponse(response, e);
    }
  }

  private String resolveToken(HttpServletRequest request) {
    String authorization = request.getHeader(AUTHORIZATION_HEADER);
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }
    return authorization.substring(BEARER_PREFIX.length());
  }

  private void writeErrorResponse(HttpServletResponse response, BusinessException e)
      throws IOException {
    response.setStatus(e.getErrorCode().getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), new ErrorResponse(e.getErrorCode()));
  }
}
