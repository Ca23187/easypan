package com.easypan.controller;

import com.easypan.component.RedisUtils;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.CreateImageCode;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController("userInfoController")
public class AccountController {
    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private RedisUtils<String> redisUtils;

    @GetMapping("/checkCode")
    public void checkCode(HttpServletResponse response, Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();

        String token = UUID.randomUUID().toString();
        String keyPrefix = (type == null || type == 0) ?
                Constants.REDIS_CHECK_CODE_KEY_PREFIX :
                Constants.REDIS_CHECK_CODE_FOR_EMAIL_KEY_PREFIX;
        String redisKey = keyPrefix + token;

        // 验证码5分钟有效期
        redisUtils.setex(redisKey, code, Constants.LENGTH_5 * 60);
        response.setHeader("captcha-token", token);

//        if (type == null || type == 0) {
//            session.setAttribute(Constants.CHECK_CODE_KEY, code);  // 登录所需的图片验证码
//        } else {
//            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);  // 发送邮箱验证码所需的图片验证码
//        }
        vCode.write(response.getOutputStream());
    }

    // NOTE: Spring注解还是建议封装在dto类里，但前端需要把传参方式从fromData转为Content-Type: application/json
    @RequestMapping("/sendEmailCode")
//    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> sendEmailCode(
//            @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
//            @VerifyParam(required = true) String checkCode,
//            @VerifyParam(required = true) Integer type,
//            @VerifyParam(required = true) String captchaToken
            @RequestParam @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确") @Size(max = 150)
            String email,

            @RequestParam @NotBlank(message = "图片验证码不能为空")
            String checkCode,

            @RequestParam @NotNull(message = "类型不能为空")
            Integer type,

            @RequestParam @NotBlank(message = "验证码Token不能为空")
            String captchaToken
    ) {
        String redisKey = Constants.REDIS_CHECK_CODE_FOR_EMAIL_KEY_PREFIX + captchaToken;
        String realCode = redisUtils.get(redisKey);
        if (realCode == null) {
            throw new BusinessException("验证码已失效，请刷新验证码");
        }
        if (!checkCode.equalsIgnoreCase(realCode)) {
            throw new BusinessException("图片验证码不正确");
        }

        // 验证通过后立即删除验证码，防止重复使用
        redisUtils.delete(redisKey);
        emailCodeService.sendEmailCode(email, type);
        return ResponseVO.ok();

//        try {
//            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
//                throw new BusinessException("图片验证码不正确");
//            }
//            emailCodeService.sendEmailCode(email, type);
//            return ResponseVO.ok(null);
//        } finally {
//            // 移除验证码
//            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
//        }
    }

    @RequestMapping("/register")
//    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> register(
//            @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
//            @VerifyParam(required = true) String nickName,
//            @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
//            @VerifyParam(required = true) String checkCode,
//            @VerifyParam(required = true) String emailCode,
//            @VerifyParam(required = true) String captchaToken
            @RequestParam @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确") @Size(max = 150)
            String email,

            @RequestParam @NotBlank(message = "昵称不能为空")
            String nickName,

            @RequestParam @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 18)
            String password,
            @RequestParam @NotBlank(message = "图片验证码不能为空")
            String checkCode,

            @RequestParam @NotBlank(message = "邮箱验证码不能为空")
            String emailCode,
            @RequestParam @NotBlank(message = "验证码Token不能为空")
            String captchaToken
    ) {
        String redisKey = Constants.REDIS_CHECK_CODE_KEY_PREFIX + captchaToken;
        String realCode = redisUtils.get(redisKey);
        if (realCode == null) {
            throw new BusinessException("验证码已失效，请刷新验证码");
        }
        if (!checkCode.equalsIgnoreCase(realCode)) {
            throw new BusinessException("图片验证码不正确");
        }
        redisUtils.delete(redisKey);
        userInfoService.register(email, nickName, password, emailCode);
        return ResponseVO.ok();

//        try {
//            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
//                throw new BusinessException("图片验证码不正确");
//            }
//            userInfoService.register(email, nickName, password, emailCode);
//            return ResponseVO.ok(null);
//        } finally {
//            // 移除验证码
//            session.removeAttribute(Constants.CHECK_CODE_KEY);
//        }
    }
}
