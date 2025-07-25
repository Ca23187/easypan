package com.easypan.service;

/**
 * 邮箱验证码 业务接口
 */
public interface EmailCodeService {
    void sendEmailCode(String email, Integer type);
    void checkCode(String email, String code);
}
