package com.medclinic.auth.controller;

import com.medclinic.auth.dto.AuthResponse;
import com.medclinic.auth.dto.ChangePasswordRequest;
import com.medclinic.auth.dto.LoginRequest;
import com.medclinic.auth.dto.RefreshTokenRequest;
import com.medclinic.auth.dto.UpdateUserRequest;
import com.medclinic.auth.dto.UserResponse;
import com.medclinic.auth.security.JwtProperties;
import com.medclinic.auth.service.AuthService;
import com.medclinic.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final UserService userService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);
        ResponseCookie cookie = buildRefreshCookie(auth.refreshToken(), jwtProperties.getRefreshTokenExpiration());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(auth.accessToken(), null, auth.roles(), auth.permissions(), auth.username()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) RefreshTokenRequest request) {
        String token = refreshTokenFromCookie != null && !refreshTokenFromCookie.isBlank()
                ? refreshTokenFromCookie
                : (request != null && request.refreshToken() != null ? request.refreshToken() : null);
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Refresh token required");
        }
        AuthResponse auth = authService.refresh(token);
        ResponseCookie cookie = buildRefreshCookie(auth.refreshToken(), jwtProperties.getRefreshTokenExpiration());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(auth.accessToken(), null, auth.roles(), auth.permissions(), auth.username()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeMs) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .path(COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserByUsername(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(Authentication authentication,
                                                          @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUserByUsername(authentication.getName(), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
