package com.smartpark.auth;

import com.smartpark.auth.dto.AuthResponse;
import com.smartpark.auth.dto.LoginRequest;
import com.smartpark.auth.dto.RegisterRequest;
import com.smartpark.auth.dto.UserResponse;
import com.smartpark.security.JwtService;
import com.smartpark.user.Role;
import com.smartpark.user.RoleName;
import com.smartpark.user.RoleRepository;
import com.smartpark.user.User;
import com.smartpark.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Hes koji se poredi kad korisnik ne postoji, da odgovor traje isto kao za pogresnu lozinku. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("smartpark-dummy-password");
    }

    /**
     * FR-01: registracija novog vozaca. E-mail je jedinstven (bez obzira na velika/mala slova),
     * lozinka se cuva kao BCrypt hes. Posle registracije korisnik je odmah prijavljen.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        Role driverRole = roleRepository.findByName(RoleName.VOZAC)
                .orElseThrow(() -> new IllegalStateException("Uloga VOZAC ne postoji u bazi (proveri migracije)."));

        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                driverRole);

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // dva istovremena zahteva sa istim e-mailom - jedinstveno ogranicenje u bazi
            throw new EmailAlreadyUsedException();
        }

        return toAuthResponse(user);
    }

    /** FR-02: prijava e-mailom i lozinkom, vraca JWT. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(normalizeEmail(request.email()));

        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), UserResponse.from(user));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
