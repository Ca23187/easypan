package com.easypan.service;

/**
 * 邮箱验证码 业务接口
 */
public interface EmailCodeService {
    void sendEmail(String email, Integer type);
    void checkCode(String redisKey, String checkCode, boolean isCaptcha);
}
