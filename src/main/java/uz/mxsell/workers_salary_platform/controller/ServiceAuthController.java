package uz.mxsell.workers_salary_platform.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyService;
import uz.mxsell.workers_salary_platform.dto.api.ErrorResponse;
import uz.mxsell.workers_salary_platform.dto.api.TokenResponse;
import uz.mxsell.workers_salary_platform.security.JwtTokenProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ServiceAuthController {

    private final CompanyService companyService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/api/{slug}/service/token")
    public ResponseEntity<?> generateToken(@PathVariable String slug,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", "Basic autentifikatsiya kerak"));
        }

        String login;
        String password;
        try {
            String decoded = new String(Base64.getDecoder().decode(authHeader.substring("Basic ".length())),
                    StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Unauthorized", "Login yoki parol xato"));
            }
            login = decoded.substring(0, colon);
            password = decoded.substring(colon + 1);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", "Login yoki parol xato"));
        }

        Company company = companyService.findBySlug(slug).orElse(null);
        if (company == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", "Kompaniya topilmadi: " + slug));
        }

        boolean usernameMatches = MessageDigest.isEqual(
                company.getServiceAuthUsername().getBytes(StandardCharsets.UTF_8),
                login.getBytes(StandardCharsets.UTF_8));
        boolean passwordMatches = MessageDigest.isEqual(
                company.getServiceAuthPassword().getBytes(StandardCharsets.UTF_8),
                password.getBytes(StandardCharsets.UTF_8));

        if (!usernameMatches || !passwordMatches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", "Login yoki parol xato"));
        }

        String token = jwtTokenProvider.generateToken(company.getId());
        return ResponseEntity.ok(TokenResponse.of(token, jwtTokenProvider.getExpirationMs()));
    }
}
