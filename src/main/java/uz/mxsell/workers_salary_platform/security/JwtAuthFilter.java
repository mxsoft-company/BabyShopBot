package uz.mxsell.workers_salary_platform.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Pattern APPEALS_PATH = Pattern.compile("^/api/[^/]+/appeals(/.*)?$");
    private static final Pattern NOTIFICATIONS_SEND_PATH = Pattern.compile("^/api/[^/]+/notifications/send$");

    private final JwtTokenProvider jwtTokenProvider;
    private final CompanyService companyService;

    // Security filter zanjiri bean'lari juda erta yaratiladi -- CompanyService (JPA)
    // @Lazy: EntityManagerFactory tayyor bo'lmaganda ilova ishga tushishini bloklamasin
    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider, @Lazy CompanyService companyService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.companyService = companyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "Token yo'q. Authorization: Bearer <token> kerak.");
            return;
        }

        Long companyId;
        try {
            companyId = jwtTokenProvider.validateAndGetCompanyId(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "Token yaroqsiz yoki muddati tugagan.");
            return;
        }

        String slug = extractSlug(request.getRequestURI());
        Company company = companyService.findBySlug(slug).orElse(null);
        if (company == null) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Not Found",
                    "Kompaniya topilmadi: " + slug);
            return;
        }

        if (!company.getId().equals(companyId)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                    "Token bu kompaniyaga tegishli emas.");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        company, null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute("company", company);

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String extractSlug(String uri) {
        String[] parts = uri.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "";
    }

    private void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(APPEALS_PATH.matcher(path).matches()
                || NOTIFICATIONS_SEND_PATH.matcher(path).matches());
    }
}
