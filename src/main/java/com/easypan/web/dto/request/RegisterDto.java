package com.easypan.web.dto.request;

import com.easypan.common.constants.VerifyRegex;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDto {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20)
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 18)
    @Pattern(regexp = VerifyRegex.PASSWORD, message = "只能是数字，字母，特殊字符 8-18位")
    private String password;

    @NotBlank(message = "图片验证码不能为空")
    private String checkCode;

    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;

    @NotBlank(message = "验证码Token不能为空")
    private String captchaToken;
}
