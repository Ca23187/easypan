package com.easypan.utils;

import com.easypan.entity.dto.UserInfoDto;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {

    private static final long tokenExpiration = 60 * 60 * 1000L;
    private static final SecretKey secretKey = Keys.hmacShaKeyFor("M0PKKI6pYGVWWfDZw90a0lTpGYX1d4AQ".getBytes());

    public static String createToken(UserInfoDto userInfoDto) {
        return Jwts.builder().
                setSubject("USER_INFO").
                setExpiration(new Date(System.currentTimeMillis() + tokenExpiration)).
                claim("userId", userInfoDto.getUserId()).
                claim("nickName", userInfoDto.getNickName()).
                claim("isAdmin", userInfoDto.getIsAdmin()).
                claim("avatar", userInfoDto.getAvatar()).
                signWith(secretKey).
                compact();
    }

    public static Claims parseToken(String token){

        if (token == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_905);
        }
        if (!token.startsWith("Bearer ")) {
            throw new BusinessException(ResponseCodeEnum.CODE_906);
        }
        try {
            token = token.substring(7);
            JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(secretKey).build();
            return jwtParser.parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        } catch (JwtException e) {
            throw new BusinessException(ResponseCodeEnum.CODE_906);
        }
    }
}