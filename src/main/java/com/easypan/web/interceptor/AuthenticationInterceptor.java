package com.easypan.web.interceptor;

import com.easypan.common.annotation.RequiresAdmin;
import com.easypan.common.annotation.RequiresLogin;
import com.easypan.common.constants.Constants;
import com.easypan.common.exception.BusinessException;
import com.easypan.common.response.ResponseCodeEnum;
import com.easypan.infra.redis.RedisUtils;
import com.easypan.infra.secure.JwtUtils;
import com.easypan.infra.secure.LoginUser;
import com.easypan.infra.secure.LoginUserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisUtils<String> redisUtils;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // 1. 只拦截 Controller 层方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 2. 检查方法 / 类上是否有注解
        boolean requiresLogin =
                handlerMethod.hasMethodAnnotation(RequiresLogin.class) ||
                        handlerMethod.getBeanType().isAnnotationPresent(RequiresLogin.class);

        boolean requiresAdmin =
                handlerMethod.hasMethodAnnotation(RequiresAdmin.class) ||
                        handlerMethod.getBeanType().isAnnotationPresent(RequiresAdmin.class);

        // 如果既不需要登录也不需要管理员，直接放行
        if (!requiresLogin && !requiresAdmin) {
            return true;
        }

        // 3. 获取 token（优先 Cookie，其次 Authorization 头），这里返回的就是裸 token
        String token = extractToken(request);

        if (token == null || token.isBlank()) {
            throw new BusinessException(ResponseCodeEnum.LOGIN_TIMEOUT);
        }

        // 3.1 黑名单校验：用裸 token 当 key
        String blacklistKey = Constants.REDIS_KEY_TOKEN_BLACKLIST + token;
        String v = redisUtils.get(blacklistKey);
        if (v != null) {
            // 已被注销 / 拉黑，视为登录失效
            throw new BusinessException(ResponseCodeEnum.LOGIN_TIMEOUT);
        }

        // 3.2 解析用户信息
        LoginUser loginUser = jwtUtils.getLoginUser(token);

        // 4. 保存到 ThreadLocal，供后续使用
        LoginUserHolder.setLoginUser(loginUser);

        // 5. 如果需要管理员，做额外校验
        if (requiresAdmin) {
            Boolean isAdmin = loginUser.getIsAdmin();
            if (isAdmin == null || !isAdmin) {
                throw new BusinessException(ResponseCodeEnum.NO_PERMISSION);
            }
        }

        return true;
    }

    /**
     * 优先从 Cookie 中读取 token，其次从 Authorization 头读取
     */
    private String extractToken(HttpServletRequest request) {
        // 1. 优先从 Cookie 中读取
        Cookie tokenCookie = WebUtils.getCookie(request, "token");
        if (tokenCookie != null) {
            String val = tokenCookie.getValue();
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }

        // 2. 再从 Authorization 头中读取
        String headerToken = request.getHeader("Authorization");
        if (headerToken != null && !headerToken.isBlank()) {
            headerToken = headerToken.trim();

            // 如果带 Bearer 前缀，剔除掉
            if (headerToken.startsWith("Bearer ")) {
                return headerToken.substring("Bearer ".length()).trim();
            }

            // 不带前缀，直接视为 token
            return headerToken;
        }

        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception e) {
        // 防止线程复用导致的脏数据
        LoginUserHolder.clear();
    }
}
