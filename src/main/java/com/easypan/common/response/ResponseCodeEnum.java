package com.easypan.common.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum {
    OK(200, "请求成功"),
    NOT_FOUND(404, "请求地址不存在"),
    BAD_REQUEST(600, "请求参数错误"),
    ALREADY_EXISTS(601, "信息已经存在"),
    INTERNAL_ERROR(500, "服务器返回错误，请联系管理员"),
    LOGIN_TIMEOUT(901, "登录超时，请重新登录"),
    SHARE_NOT_FOUND(902, "分享连接不存在，或者已失效"),
    SHARE_EXPIRED(903, "分享验证失效，请重新验证"),
    STORAGE_INSUFFICIENT(904, "网盘空间不足，请扩容"),
    NOT_LOGGED_IN(905, "未登录，请先登录"),
    TOKEN_INVALID(906, "token非法"),
    NO_PERMISSION(907, "没有访问权限");

    private final Integer code;
    private final String msg;
}