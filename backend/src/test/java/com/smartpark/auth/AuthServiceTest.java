package com.smartpark.auth;

import com.smartpark.auth.dto.AuthResponse;
import com.smartpark.auth.dto.LoginRequest;
import com.smartpark.auth.dto.RegisterRequest;
import com.smartpark.security.JwtService;
import com.smartpark.user.Role;
import com.smartpark.user.RoleName;
import com.smartpark.user.RoleRepository;
import com.smartpark.user.User;
import com.smartpark.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(4); // brzo za testove
        jwtService = mock(JwtService.class);
        when(jwtService.generateToken(any())).thenReturn("test-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);
    }

    @Test
    void registracijaCuvaHesLozinkeINormalizovanEmail() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.VOZAC)).thenReturn(Optional.of(new Role(RoleName.VOZAC)));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(
                new RegisterRequest("  Ana Anic ", "  Ana@Example.COM ", "tajna1234"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        User user = saved.getValue();

        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(user.getName()).isEqualTo("Ana Anic");
        assertThat(user.getPasswordHash()).isNotEqualTo("tajna1234");
        assertThat(passwordEncoder.matches("tajna1234", user.getPasswordHash())).isTrue();
        assertThat(user.getRole().getName()).isEqualTo(RoleName.VOZAC);
        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.user().role()).isEqualTo("VOZAC");
    }

    @Test
    void registracijaSaPostojecimEmailomBacaKonflikt() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ana", "ana@example.com", "tajna1234")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void prijavaSaIspravnomLozinkomVracaToken() {
        User user = new User("Ana", "ana@example.com", passwordEncoder.encode("tajna1234"), new Role(RoleName.VOZAC));
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("ANA@example.com", "tajna1234"));

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void prijavaSaPogresnomLozinkomSeOdbija() {
        User user = new User("Ana", "ana@example.com", passwordEncoder.encode("tajna1234"), new Role(RoleName.VOZAC));
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@example.com", "pogresna")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void prijavaNepostojecegKorisnikaSeOdbijaIstomPorukom() {
        when(userRepository.findByEmail("niko@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("niko@example.com", "bilosta1")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Pogresan e-mail ili lozinka.");
    }
}
