package id.ac.ui.cs.advprog.kki.json.auth.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InternalServiceAuthFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validInternalServiceTokenAuthenticatesInternalWalletRequest() throws Exception {
        InternalServiceAuthFilter filter = new InternalServiceAuthFilter("test-internal-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/internal/wallet/refund");
        request.addHeader(InternalServiceAuthFilter.INTERNAL_SERVICE_HEADER, "test-internal-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("internal-service", authentication.getPrincipal());
        assertEquals("ROLE_INTERNAL_SERVICE", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void missingInternalServiceTokenLeavesRequestUnauthenticated() throws Exception {
        InternalServiceAuthFilter filter = new InternalServiceAuthFilter("test-internal-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/internal/wallet/refund");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
