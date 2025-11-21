package com.easypan.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class SendEmailCodeDto {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "图片验证码不能为空")
    private String checkCode;

    @NotNull(message = "类型不能为空")
    private Integer type;

    @NotBlank(message = "验证码Token不能为空")
    private String captchaToken;
}
