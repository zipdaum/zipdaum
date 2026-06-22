package com.ssafy.zipdaum.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  static final String TRACE_ID_KEY = "traceId";
  static final String TRACE_ID_HEADER = "X-Trace-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String traceId = UUID.randomUUID().toString().replace("-", "");

    try {
      MDC.put(TRACE_ID_KEY, traceId);
      response.setHeader(TRACE_ID_HEADER, traceId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(TRACE_ID_KEY);
    }
  }
}
