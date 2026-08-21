package co.edu.ufps.legal_cases.security.controller.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import co.edu.ufps.legal_cases.security.dto.auth.login.LoginRequestDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginResponseDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginResultDTO;
import co.edu.ufps.legal_cases.security.service.auth.AuthService;
import co.edu.ufps.legal_cases.security.service.auth.PasswordResetService;

class AuthControllerCsrfTest {

    private AuthService authService;
    private PasswordResetService passwordResetService;
    private CsrfTokenRepository csrfTokenRepository;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        passwordResetService = mock(PasswordResetService.class);
        csrfTokenRepository = mock(CsrfTokenRepository.class);

        authController = new AuthController(
                authService,
                passwordResetService,
                csrfTokenRepository,
                false,
                "Lax");
    }

    @Test
    void shouldInvalidateCsrfAfterSuccessfulLogin() {
        LoginRequestDTO loginRequest = mock(LoginRequestDTO.class);
        LoginResultDTO loginResult = mock(LoginResultDTO.class);
        LoginResponseDTO loginResponse = mock(LoginResponseDTO.class);

        when(authService.login(loginRequest)).thenReturn(loginResult);
        when(loginResult.getToken()).thenReturn("jwt-test");
        when(loginResult.getResponse()).thenReturn(loginResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<LoginResponseDTO> result =
                authController.login(loginRequest, request, response);

        verify(csrfTokenRepository).saveToken(null, request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(loginResponse, result.getBody());

        List<String> cookies =
                response.getHeaders(HttpHeaders.SET_COOKIE);

        assertTrue(cookies.stream()
                .anyMatch(cookie ->
                        cookie.startsWith("access_token=jwt-test")));
    }

    @Test
    void shouldInvalidateCsrfAndAuthCookieOnLogout() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<Void> result =
                authController.logout(request, response);

        verify(csrfTokenRepository).saveToken(null, request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        List<String> cookies =
                response.getHeaders(HttpHeaders.SET_COOKIE);

        assertTrue(cookies.stream()
                .anyMatch(cookie ->
                        cookie.startsWith("access_token=")
                                && cookie.contains("Max-Age=0")));
    }

    @Test
    void shouldNotInvalidateCsrfWhenLoginFails() {
        LoginRequestDTO loginRequest = mock(LoginRequestDTO.class);

        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new RuntimeException("Login rechazado"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(
                RuntimeException.class,
                () -> authController.login(
                        loginRequest,
                        request,
                        response));

        verify(csrfTokenRepository, never())
                .saveToken(null, request, response);
    }
}