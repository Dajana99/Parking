package com.smartpark.security;

import com.smartpark.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Cita "Authorization: Bearer <token>" i, ako je token ispravan, prijavljuje korisnika za taj zahtev.
 * Korisnik se svaki put ucitava iz baze, pa promena uloge ili brisanje naloga vazi odmah.
 * Ako tokena nema ili je neispravan, zahtev ide dalje neprijavljen - Spring Security ce vratiti 401
 * za zasticene putanje.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                Long userId = jwtService.extractUserId(token);
                userRepository.findById(userId).ifPresent(user -> {
                    var principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().getName());
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            } catch (JwtException | IllegalArgumentException e) {
                // neispravan ili istekao token - ostavljamo zahtev neprijavljenim
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
