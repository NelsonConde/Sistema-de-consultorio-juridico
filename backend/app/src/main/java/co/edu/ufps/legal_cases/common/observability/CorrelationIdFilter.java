package co.edu.ufps.legal_cases.common.observability;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId =
                CorrelationIdContext.resolve(request.getHeader(CorrelationIdContext.HEADER_NAME));

        CorrelationIdContext.bind(request, correlationId);
        response.setHeader(CorrelationIdContext.HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationIdContext.clear();
        }
    }
}
