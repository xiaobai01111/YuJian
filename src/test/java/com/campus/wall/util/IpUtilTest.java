package com.campus.wall.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IpUtilTest {

    @AfterEach
    void reset() {
        IpUtil.configure(false, List.of());
    }

    @Test
    void shouldIgnoreForwardedHeadersWhenTrustDisabled() {
        IpUtil.configure(false, List.of("127.0.0.1/32"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        request.addHeader("X-Forwarded-For", "8.8.8.8");

        String clientIp = IpUtil.getClientIp(request);

        assertThat(clientIp).isEqualTo("1.2.3.4");
    }

    @Test
    void shouldUseForwardedHeadersForTrustedProxy() {
        IpUtil.configure(true, List.of("127.0.0.1/32"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "8.8.8.8, 1.1.1.1");

        String clientIp = IpUtil.getClientIp(request);

        assertThat(clientIp).isEqualTo("8.8.8.8");
    }

    @Test
    void shouldIgnoreForwardedHeadersForUntrustedProxy() {
        IpUtil.configure(true, List.of("10.0.0.0/8"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "8.8.8.8");

        String clientIp = IpUtil.getClientIp(request);

        assertThat(clientIp).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldParseForwardedHeader() throws Exception {
        IpUtil.configure(true, List.of("127.0.0.1/32"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Forwarded", "for=\"[2001:db8::1]:8443\";proto=https");

        String clientIp = IpUtil.getClientIp(request);

        String expected = InetAddress.getByName("2001:db8::1").getHostAddress();
        assertThat(clientIp).isEqualTo(expected);
    }
}
