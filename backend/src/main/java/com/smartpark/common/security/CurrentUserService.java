package com.smartpark.common.security;

import com.smartpark.common.exception.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Daje ID i uloge prijavljenog korisnika.
 *
 * Pretpostavka: JWT filter postavlja e-mail kao "name" u Authentication objektu (subject tokena).
 * ID se cita direktno iz tabele users, pa ova klasa ne zavisi od vase User klase.
 * Ako vas principal vec ima getId(), ovde mozete samo da ga vratite.
 */
@Service
public class CurrentUserService {

    public static final String ROLE_DRIVER = "ROLE_VOZAC";
    public static final String ROLE_OPERATOR = "ROLE_OPERATER";
    public static final String ROLE_ADMIN = "ROLE_ADMINISTRATOR";

    private final JdbcTemplate jdbcTemplate;

    public CurrentUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long currentUserId() {
        String email = authentication().getName();
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT user_id FROM users WHERE email = ?", Long.class, email);
        if (ids.isEmpty()) {
            throw new NotFoundException("Prijavljeni korisnik ne postoji");
        }
        return ids.get(0);
    }

    /** Operater ili administrator - vide sve rezervacije (FR-25). */
    public boolean isStaff() {
        Set<String> roles = currentRoles();
        return roles.contains(ROLE_OPERATOR) || roles.contains(ROLE_ADMIN);
    }

    public boolean isAdmin() {
        return currentRoles().contains(ROLE_ADMIN);
    }

    private Set<String> currentRoles() {
        Set<String> roles = new java.util.HashSet<>();
        for (GrantedAuthority a : authentication().getAuthorities()) {
            roles.add(a.getAuthority());
        }
        return roles;
    }

    private Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Nema prijavljenog korisnika");
        }
        return auth;
    }
}
