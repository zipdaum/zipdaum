package com.ssafy.zipdaum.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

  private final TraceIdFilter filter = new TraceIdFilter();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void 요청_처리_중_traceId를_MDC와_응답_헤더에_설정한다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {
      String traceId = MDC.get(TraceIdFilter.TRACE_ID_KEY);

      assertThat(traceId).hasSize(32);
      assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(traceId);
    });

    assertThat(MDC.get(TraceIdFilter.TRACE_ID_KEY)).isNull();
  }

  @Test
  void 요청_처리에서_예외가_발생해도_MDC의_traceId를_제거한다() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
      throw new IllegalStateException("test");
    })).isInstanceOf(IllegalStateException.class);

    assertThat(MDC.get(TraceIdFilter.TRACE_ID_KEY)).isNull();
  }
}
