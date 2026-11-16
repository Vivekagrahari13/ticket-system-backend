package com.ticket.system.common.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    private String runAndCaptureIdSeenByTheChain(MockHttpServletRequest request, MockHttpServletResponse response)
            throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(MDC.get(RequestLoggingFilter.REQUEST_ID_KEY));
        filter.doFilter(request, response, chain);
        return seen.get();
    }

    @Test
    void generatesARequestIdAndReturnsItInTheResponseHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String seen = runAndCaptureIdSeenByTheChain(new MockHttpServletRequest("GET", "/tickets"), response);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(seen).isEqualTo(response.getHeader("X-Request-Id"));
    }

    @Test
    void reusesASafeIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tickets");
        request.addHeader("X-Request-Id", "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String seen = runAndCaptureIdSeenByTheChain(request, response);

        assertThat(seen).isEqualTo("abc-123");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("abc-123");
    }

    @Test
    void replacesAnUnsafeIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tickets");
        request.addHeader("X-Request-Id", "bad id\nINFO fake log line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String seen = runAndCaptureIdSeenByTheChain(request, response);

        assertThat(seen).doesNotContain("fake").doesNotContain("\n");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(seen);
    }

    @Test
    void clearsTheContextAfterTheRequest() throws Exception {
        runAndCaptureIdSeenByTheChain(new MockHttpServletRequest("GET", "/tickets"), new MockHttpServletResponse());

        assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_KEY)).isNull();
    }
}
