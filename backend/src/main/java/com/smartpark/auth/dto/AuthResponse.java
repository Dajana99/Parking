package com.smartpark.auth.dto;

/**
 * Odgovor na registraciju i prijavu.
 * Klijent cuva {@code token} i salje ga u zaglavlju: {@code Authorization: Bearer <token>}.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse bearer(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
