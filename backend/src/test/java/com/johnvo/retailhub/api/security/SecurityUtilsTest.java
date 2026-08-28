package com.johnvo.retailhub.api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUtilsTest {
    @Test
    void missingAuthenticationIsNotAdmin() {
        assertThat(SecurityUtils.isAdmin(null)).isFalse();
    }
}
