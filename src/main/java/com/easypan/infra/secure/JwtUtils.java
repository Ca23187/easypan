package com.easypan.infra.secure;

import com.easypan.common.exception.BusinessException;
import com.easypan.common.response.ResponseCodeEnum;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {

    private final JwtProperties props;
    private final SecretKey secretKey;

    public JwtUtils(JwtProperties props) {
        this.props = props;
        this.secretKey = props.getSecretKey();
    }

    /**
     * 生成 JWT token（裸 token，不带任何前缀）
     */
    public String createToken(LoginUser loginUser) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date exp = new Date(nowMillis + props.getExpireMillis());

        JwtBuilder builder = Jwts.builder()
                .setSubject(props.getSubject())
                .setIssuer(props.getIssuer())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("userId", loginUser.getUserId());

        if (loginUser.getIsAdmin() != null) {
            builder.claim("isAdmin", loginUser.getIsAdmin());
        }
        if (loginUser.getNickname() != null) {
            builder.claim("nickname", loginUser.getNickname());
        }
        if (loginUser.getAvatar() != null) {
            builder.claim("avatar", loginUser.getAvatar());
        }

        return builder
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token
     */
    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResponseCodeEnum.NOT_LOGGED_IN);
        }
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .setAllowedClockSkewSeconds(props.getClockSkewSeconds())
                    .build();

            return parser.parseClaimsJws(token.trim()).getBody();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResponseCodeEnum.LOGIN_TIMEOUT); // token 过期
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ResponseCodeEnum.TOKEN_INVALID); // 非法 token
        }
    }

    /**
     * 从 token 直接构造 LoginUser
     */
    public LoginUser getLoginUser(String token) {
        Claims claims = parseToken(token);
        return buildLoginUserFromClaims(claims);
    }

    /**
     * 从 Claims 构造 LoginUser
     */
    private LoginUser buildLoginUserFromClaims(Claims claims) {
        LoginUser dto = new LoginUser();

        dto.setUserId((String) claims.get("userId"));
        dto.setNickname((String) claims.get("nickname"));
        dto.setAvatar((String) claims.get("avatar"));

        Object isAdmin = claims.get("isAdmin");
        if (isAdmin != null) {
            dto.setIsAdmin(Boolean.parseBoolean(String.valueOf(isAdmin)));
        }

        return dto;
    }

    /**
     * 获取 token 剩余过期的毫秒数
     */
    public long getRemainingMillis(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();

        long nowMillis = System.currentTimeMillis();
        long expMillis = expiration.getTime();

        long diff = expMillis - nowMillis;
        return Math.max(diff, 0);
    }

    /**
     * 简单刷新：解析旧 token（未过期），重新生成一个新 token
     */
    public String refreshToken(String token) {
        Claims claims = parseToken(token);
        LoginUser dto = buildLoginUserFromClaims(claims);
        return createToken(dto);
    }

    /**
     * 静默校验：只返回 true/false，不抛业务异常
     */
    public boolean validateTokenSilently(String token) {
        try {
            parseToken(token);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    public long getDefaultMillis() {
        return props.getExpireMillis();
    }

}