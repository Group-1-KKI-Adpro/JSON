package id.ac.ui.cs.advprog.kki.json.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    public static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service-Token";
    private static final String INTERNAL_WALLET_PATH_PREFIX = "/api/internal/wallet/";

    private final String internalServiceToken;

    public InternalServiceAuthFilter(
            @Value("${app.internal.service-token:dev-internal-service-token}") String internalServiceToken
    ) {
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (request == null) {
            return true;
        }

        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }

        return path == null || !path.startsWith(INTERNAL_WALLET_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String presentedToken = request.getHeader(INTERNAL_SERVICE_HEADER);

        if (presentedToken != null && !presentedToken.isBlank() && presentedToken.equals(internalServiceToken)) {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            "internal-service",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
                    );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
