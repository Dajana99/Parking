package com.smartpark.user;

import com.smartpark.auth.dto.UserResponse;
import com.smartpark.common.ApiException;
import com.smartpark.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Podaci o prijavljenom korisniku. Ovde kasnije dolaze izmena profila i DELETE /api/me (FR-04). */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Moj nalog")
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Ko sam ja", description = "Vraca prijavljenog korisnika; 401 bez tokena")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser me) {
        return userRepository.findById(me.id())
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Nalog vise ne postoji."));
    }
}
