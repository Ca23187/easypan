package com.easypan.infra.secure;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * 原始配置字符串（可以是纯文本，也可以是 Base64）
     */
    private String secret;

    /**
     * 过期时间（毫秒）
     */
    private long expireMillis;

    /**
     * 签发者
     */
    private String issuer;

    /**
     * 主题（subject）
     */
    private String subject;

    /**
     * 允许的时钟偏差（秒）
     */
    private long clockSkewSeconds = 60L;

    /**
     * 初始化好的密钥
     */
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("jwt.secret 未配置");
        }

        byte[] keyBytes;
        try {
            // 优先当作 Base64 解码
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            // 如果不是合法 Base64，就当普通字符串处理
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) { // 256 bit
            throw new IllegalStateException("JWT secret key 太短，至少需要 256-bit (32 bytes) 长度");
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }
}
