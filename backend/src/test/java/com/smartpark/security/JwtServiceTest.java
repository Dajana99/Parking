package com.smartpark.security;

import com.smartpark.user.Role;
import com.smartpark.user.RoleName;
import com.smartpark.user.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "u6QhPNeZcXaHj+ZbP5dafwDNYQlAdELH559A46oK/TXtezTLboHnlmz6n3KPVbRl";
    private static final String OTHER_SECRET = "Z2xhdmFzdmV0YWRydWdhdGFqbmFzaWZyYWtvamFuaWplaXN0YXphdGVzdG92ZQ==";

    private static User user(long id) {
        User u = new User("Ana Anic", "ana@example.com", "hash", new Role(RoleName.VOZAC));
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    void tokenSadrziIdKorisnika() {
        JwtService jwt = new JwtService(SECRET, 60);

        String token = jwt.generateToken(user(42L));

        assertThat(jwt.extractUserId(token)).isEqualTo(42L);
        assertThat(jwt.getExpirationSeconds()).isEqualTo(3600);
    }

    @Test
    void tokenPotpisanDrugimKljucemSeOdbija() {
        String token = new JwtService(OTHER_SECRET, 60).generateToken(user(1L));

        assertThatThrownBy(() -> new JwtService(SECRET, 60).extractUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void istekaoTokenSeOdbija() {
        JwtService jwt = new JwtService(SECRET, -1); // rok vazenja vec prosao

        String token = jwt.generateToken(user(1L));

        assertThatThrownBy(() -> jwt.extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void praznaTajnaZaustavljaPokretanje() {
        assertThatThrownBy(() -> new JwtService("", 60)).isInstanceOf(IllegalStateException.class);
    }
}
