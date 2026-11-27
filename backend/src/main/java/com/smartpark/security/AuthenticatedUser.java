package com.smartpark.security;

import com.smartpark.user.RoleName;

/**
 * Prijavljeni korisnik kakav se cuva u SecurityContext-u.
 * U kontroleru: {@code @AuthenticationPrincipal AuthenticatedUser me}.
 */
public record AuthenticatedUser(Long id, String email, RoleName role) {
}
