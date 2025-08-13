package com.easypan.interceptor;

import com.easypan.annotation.RequiresAdmin;
import com.easypan.annotation.RequiresLogin;
import com.easypan.entity.dto.UserInfoDto;
import com.easypan.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            // 如果方法上有 @RequiresLogin 注解，执行拦截逻辑
            if (handlerMethod.hasMethodAnnotation(RequiresLogin.class)) {
                String token = request.getHeader("Authorization");
                Claims claims = JwtUtil.parseToken(token);
                LoginUserInfoHolder.setLoginUserInfo(
                        new UserInfoDto(
                                claims.get("userId", String.class),
                                claims.get("nickName", String.class),
                                claims.get("isAdmin", Boolean.class),
                                claims.get("avatar", String.class)
                        )
                );
            }
            if (handlerMethod.hasMethodAnnotation(RequiresAdmin.class)) {
                // TODO: 校验管理员身份
            }
        }
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        LoginUserInfoHolder.clear();
    }
}