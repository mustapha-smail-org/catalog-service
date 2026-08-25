package com.citypulse.catalog.config;

import com.citypulse.catalog.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminTokenGuardTest {

    @Test
    void shouldAcceptMatchingToken() {
        AdminTokenGuard guard = new AdminTokenGuard(new AdminProperties("s3cret"));

        assertThatCode(() -> guard.requireValidToken("s3cret")).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWrongToken() {
        AdminTokenGuard guard = new AdminTokenGuard(new AdminProperties("s3cret"));

        assertThatThrownBy(() -> guard.requireValidToken("nope"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldRejectMissingToken() {
        AdminTokenGuard guard = new AdminTokenGuard(new AdminProperties("s3cret"));

        assertThatThrownBy(() -> guard.requireValidToken(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldDenyByDefaultWhenNoSecretConfigured() {
        AdminTokenGuard blank = new AdminTokenGuard(new AdminProperties("  "));
        AdminTokenGuard missing = new AdminTokenGuard(new AdminProperties(null));

        assertThatThrownBy(() -> blank.requireValidToken("anything"))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> missing.requireValidToken("anything"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
