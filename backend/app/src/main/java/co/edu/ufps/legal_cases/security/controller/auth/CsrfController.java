package co.edu.ufps.legal_cases.security.controller.auth;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ufps.legal_cases.security.dto.auth.CsrfTokenResponseDTO;

@RestController
@RequestMapping("/api/auth")
public class CsrfController {

    @GetMapping("/csrf")
    public CsrfTokenResponseDTO csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponseDTO(
                csrfToken.getHeaderName(),
                csrfToken.getToken());
    }
}