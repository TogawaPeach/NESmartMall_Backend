package com.nemall.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

@Component
public class JwtUtil {

    private final JwtEncoder jwtEncoder;

    private final long expiresIn;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expires-in}") long expiresIn)
            throws NoSuchAlgorithmException {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT 密钥不能为空");
        }

        if (expiresIn <= 0) {
            throw new IllegalArgumentException("JWT 有效期必须大于 0");
        }


        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));

        SecretKey secretKey =
                new SecretKeySpec(keyBytes, "HmacSHA256");

        this.jwtEncoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        this.expiresIn = expiresIn;
    }

    /**
     * 登录验证通过后，使用数据库中的账户 ID 和角色签发 JWT。
     */
    public TokenResult createToken(Long accountId, Integer role) {

        if (accountId == null || accountId <= 0 || role == null) {
            throw new IllegalArgumentException("账户 ID 或角色不合法");
        }

        String roleName = switch (role) {
            case 0 -> "MERCHANT";
            case 1 -> "CUSTOMER";
            default -> throw new IllegalArgumentException("不支持的角色");
        };

        Instant now = Instant.now();

        // JWT 载荷：账户、角色、签发时间及过期时间
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(accountId.toString())
                .claim("roles", List.of(roleName))
                .issuer("NESmartMall")
                .audience(List.of("nesmartmall-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        String accessToken = jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new TokenResult(accessToken, expiresIn);
    }

    /**
     * 返回生成的令牌及有效秒数。
     */
    public record TokenResult(
            String accessToken,
            long expiresIn
    ) {
    }
}