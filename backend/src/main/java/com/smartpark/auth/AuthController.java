package com.smartpark.auth;

import com.smartpark.auth.dto.AuthResponse;
import com.smartpark.auth.dto.LoginRequest;
import com.smartpark.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autentifikacija")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @SecurityRequirements // javno - bez tokena
    @Operation(summary = "Registracija vozaca", description = "201 + token; 400 neispravni podaci; 409 e-mail zauzet")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @SecurityRequirements // javno - bez tokena
    @Operation(summary = "Prijava", description = "200 + token; 400 neispravni podaci; 401 pogresan e-mail ili lozinka")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * JWT je bez stanja na serveru: odjava znaci da klijent obrise svoj token.
     * Endpoint postoji da bi frontend imao jasan poziv i da se kasnije lako doda crna lista tokena.
     */
    @PostMapping("/logout")
    @Operation(summary = "Odjava", description = "204; klijent brise sacuvani token")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
