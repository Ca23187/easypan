package com.easypan.infra.secure;

import org.springframework.http.ResponseCookie;

public class CookieUtils {

    /**
     * 生成 token Cookie（HttpOnly + SameSite=Lax）
     */
    public static ResponseCookie buildTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("token", token == null ? "" : token)
                .httpOnly(true)  // JS 不能读，防止 XSS 窃取
                .path("/")  // 整个站点有效
                .sameSite("Lax")  // 防 CSRF，同时保持大多数情况下可用
                .maxAge(maxAgeSeconds)
                .build();
    }

    /**
     * 构造一个清空 token 的 Set-Cookie
     */
    public static ResponseCookie clearTokenCookie() {
        return ResponseCookie.from("token", "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)   // 清除 Cookie
                .build();
    }
}
