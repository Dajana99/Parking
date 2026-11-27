package com.smartpark.auth.dto;

import com.smartpark.user.User;

import java.time.Instant;

/** Podaci o korisniku koje vracamo klijentu - nikad ne sadrzi hes lozinke. */
public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName().name(),
                user.getCreatedAt());
    }
}
