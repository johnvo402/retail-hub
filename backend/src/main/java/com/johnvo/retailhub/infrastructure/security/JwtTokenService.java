package com.johnvo.retailhub.infrastructure.security;

import com.johnvo.retailhub.application.common.security.AccessTokenVerifier;
import com.johnvo.retailhub.application.common.security.TokenService;
import com.johnvo.retailhub.domain.identity.User;
import com.johnvo.retailhub.domain.identity.UserRole;
import com.johnvo.retailhub.infrastructure.config.SecurityProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Optional;

@Component
public class JwtTokenService implements TokenService, AccessTokenVerifier {
    private final SecurityProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenService(SecurityProperties properties) {
        if (properties.jwtSecret() == null || properties.jwtSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_ACCESS_SECRET must contain at least 32 bytes");
        }
        this.properties = properties;
        SecretKey key = new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(key).build();
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Override
    public AccessToken issueAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.accessExpirationSeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", user.role().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, properties.accessExpirationSeconds());
    }

    @Override
    public RefreshToken issueRefreshToken(Instant now) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new RefreshToken(value, hashRefreshToken(value),
                now.plusSeconds(properties.refreshExpirationSeconds()));
    }

    @Override
    public String hashRefreshToken(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Override
    public Optional<AuthenticatedUser> verify(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            return Optional.of(new AuthenticatedUser(UUID.fromString(jwt.getSubject()),
                    UserRole.valueOf(jwt.getClaimAsString("role"))));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
