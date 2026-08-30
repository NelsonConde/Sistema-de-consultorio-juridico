package co.edu.ufps.legal_cases.security.controller.auth;

import co.edu.ufps.legal_cases.config.security.AuthCookieProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.dto.auth.AuthMessageResponseDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginRequestDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginResponseDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginResultDTO;
import co.edu.ufps.legal_cases.security.dto.auth.password.CambiarPasswordRequestDTO;
import co.edu.ufps.legal_cases.security.dto.auth.password.RestablecerPasswordDTO;
import co.edu.ufps.legal_cases.security.dto.auth.password.SolicitarRecuperacionPasswordDTO;
import co.edu.ufps.legal_cases.security.service.auth.AuthService;
import co.edu.ufps.legal_cases.security.service.auth.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final long ACCESS_TOKEN_MAX_AGE_SECONDS = 60 * 60;

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final AuthCookieProperties authCookieProperties;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(
        AuthService authService,
        PasswordResetService passwordResetService,
        CsrfTokenRepository csrfTokenRepository,
        AuthCookieProperties authCookieProperties) {

        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.csrfTokenRepository = csrfTokenRepository;
        this.authCookieProperties = authCookieProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {

        LoginResultDTO result = authService.login(dto);

        ResponseCookie authCookie = crearCookieAuth(
                result.getToken(),
                ACCESS_TOKEN_MAX_AGE_SECONDS);

        // Invalida el token CSRF utilizado durante el login.
        csrfTokenRepository.saveToken(null, request, response);

        // Agrega la nueva cookie de autenticación sin reemplazar
        // el Set-Cookie generado al eliminar el CSRF.
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());

        return ResponseEntity.ok(result.getResponse());
    }

    // Permite al frontend verificar sesión y obtener usuario actual.
    @GetMapping("/me")
    public UsuarioSistemaDTO me(
            @CookieValue(name = ACCESS_TOKEN_COOKIE, required = false) String token) {
        return authService.me(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        ResponseCookie authCookie = crearCookieAuth("", 0);

        // Elimina el token CSRF asociado al estado autenticado.
        csrfTokenRepository.saveToken(null, request, response);

        // Elimina también la cookie JWT.
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(
            @CookieValue(name = ACCESS_TOKEN_COOKIE, required = false) String token,
            @Valid @RequestBody CambiarPasswordRequestDTO dto) {

        authService.cambiarPassword(token, dto);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/solicitar-recuperacion")
    public ResponseEntity<AuthMessageResponseDTO> solicitarRecuperacion(
            @Valid @RequestBody SolicitarRecuperacionPasswordDTO dto) {

        passwordResetService.solicitarRecuperacion(dto);

        // Mensaje genérico para no revelar si el correo existe.
        return ResponseEntity.ok(new AuthMessageResponseDTO(
                "Si el correo existe, se enviarán instrucciones para recuperar la contraseña"));
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<AuthMessageResponseDTO> restablecerPassword(
            @Valid @RequestBody RestablecerPasswordDTO dto) {

        passwordResetService.restablecerPassword(dto);

        return ResponseEntity.ok(new AuthMessageResponseDTO(
                "La contraseña se restableció correctamente"));
    }

    private ResponseCookie crearCookieAuth(String value, long maxAgeSeconds) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(authCookieProperties.isSecure())
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(authCookieProperties.getSameSite())
                .build();
    }
}