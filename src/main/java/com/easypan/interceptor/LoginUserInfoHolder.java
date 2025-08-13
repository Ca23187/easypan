package com.easypan.interceptor;

import com.easypan.entity.dto.UserInfoDto;

public class LoginUserInfoHolder {

    public static ThreadLocal<UserInfoDto> threadLocal = new ThreadLocal<>();

    public static void setLoginUserInfo(UserInfoDto userInfoDto) {
        threadLocal.set(userInfoDto);
    }

    public static UserInfoDto getLoginUserInfo() {
        return threadLocal.get();
    }

    public static void clear() {
        threadLocal.remove();
    }
}