package com.easypan.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
// NOTE: 可以忽略类中不存在的字段，确保即使Redis属性与Java对象属性不一致时也不会报错
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SysSettingsDto implements Serializable {
    /**
     * 注册发送邮件标题
     */
    private String registerEmailTitle = "邮箱验证码";

    /**
     * 注册发送邮件内容
     */
    private String registerEmailContent = "您好，您的邮箱验证码是：%s，15分钟有效";

    /**
     * 用户初始化空间大小 5M
     */
    private Integer userInitTotalSpace = 5;

}
