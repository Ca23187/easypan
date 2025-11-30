package com.easypan.web.controller;

import com.easypan.common.annotation.RequiresLogin;
import com.easypan.common.constants.Constants;
import com.easypan.common.constants.VerifyRegex;
import com.easypan.common.exception.BusinessException;
import com.easypan.common.response.ResponseCodeEnum;
import com.easypan.common.response.ResponseVo;
import com.easypan.common.util.FileTools;
import com.easypan.common.util.StringTools;
import com.easypan.config.AppProperties;
import com.easypan.infra.redis.RedisComponent;
import com.easypan.infra.redis.RedisUtils;
import com.easypan.infra.secure.CookieUtils;
import com.easypan.infra.secure.JwtUtils;
import com.easypan.infra.secure.LoginUser;
import com.easypan.infra.secure.LoginUserHolder;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import com.easypan.service.dto.UserSpaceDto;
import com.easypan.web.dto.request.RegisterDto;
import com.easypan.web.dto.request.ResetPwdDto;
import com.easypan.web.dto.request.SendEmailCodeDto;
import com.pig4cloud.captcha.SpecCaptcha;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController("userInfoController")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private AppProperties appProperties;

    @Resource
    private RedisUtils<String> redisUtils;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private JwtUtils jwtUtils;

    @GetMapping("/checkCode")
    public void checkCode(HttpServletResponse response, Integer type) throws IOException {
        SpecCaptcha captcha = new SpecCaptcha(130, 38, Constants.CHECK_CODE_LENGTH);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = captcha.text();

        String token = UUID.randomUUID().toString();
        String keyPrefix = (type == null || type == 0) ?
                Constants.REDIS_KEY_CAPTCHA :
                Constants.REDIS_KEY_CAPTCHA_FOR_EMAIL;
        String redisKey = keyPrefix + token;

        // 验证码5分钟有效期
        redisUtils.setex(
                redisKey,
                code,
                Constants.REDIS_EXPIRATION_CHECK_CODE,
                Constants.REDIS_TIME_UNIT_CHECK_CODE
        );
        response.setHeader("captcha-token", token);
        captcha.out(response.getOutputStream());
    }

    @PostMapping("/sendEmailCode")
    public ResponseVo<Void> sendEmailCode(@RequestBody @Valid SendEmailCodeDto dto) {
        validateAndClearCaptcha(
                Constants.REDIS_KEY_CAPTCHA_FOR_EMAIL,
                dto.getCaptchaToken(),
                dto.getCheckCode()
        );
        emailCodeService.sendEmail(dto.getEmail(), dto.getType());
        return ResponseVo.ok();
    }

    @PostMapping("/register")
    public ResponseVo<Void> register(@RequestBody @Valid RegisterDto dto) {
        validateAndClearCaptcha(
                Constants.REDIS_KEY_CAPTCHA,
                dto.getCaptchaToken(),
                dto.getCheckCode()
        );
        userInfoService.register(
                dto.getEmail(),
                dto.getNickname(),
                dto.getPassword(),
                dto.getEmailCode()
        );
        return ResponseVo.ok();
    }

    @PostMapping("/login")
    public ResponseVo<Map<String, String>> login(
            HttpServletResponse response,
            @NotBlank(message = "邮箱不能为空") String email,
            @NotBlank(message = "密码不能为空") String password,
            @NotBlank(message = "图片验证码不能为空") String checkCode,
            @NotBlank(message = "验证码Token不能为空") String captchaToken
    ) {
        validateAndClearCaptcha(Constants.REDIS_KEY_CAPTCHA, captchaToken, checkCode);
        String token = userInfoService.login(email, password);
        long maxAgeSeconds = jwtUtils.getDefaultMillis() / 1000;
        ResponseCookie cookie = CookieUtils.buildTokenCookie(token, maxAgeSeconds);
        response.addHeader("Set-Cookie", cookie.toString());
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ResponseVo.ok(result);
    }

    @PostMapping("/resetPwd")
    public ResponseVo<Void> resetPwd(@RequestBody @Valid ResetPwdDto dto) {
        validateAndClearCaptcha(
                Constants.REDIS_KEY_CAPTCHA,
                dto.getCaptchaToken(),
                dto.getCheckCode()
        );
        userInfoService.resetPwd(
                dto.getEmail(),
                dto.getPassword(),
                dto.getEmailCode()
        );
        return ResponseVo.ok();
    }

    @GetMapping("/getAvatar/{userId}")
    public void getAvatar(HttpServletResponse response, @PathVariable String userId) {
        // 1. 校验路径合法性（防止目录穿越）
        if (!StringTools.pathIsOk(userId)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        Path avatarRoot = Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER_FILE,
                Constants.FILE_FOLDER_AVATAR_NAME
        );
        try {
            Files.createDirectories(avatarRoot);
        } catch (IOException e) {
            logger.error("创建头像目录失败: {}", avatarRoot, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
        // 找用户头像
        Path avatarPath = avatarRoot.resolve(userId + Constants.AVATAR_SUFFIX);

        if (!Files.exists(avatarPath)) {  // 找默认头像
            Path defaultAvatarPath = avatarRoot.resolve(Constants.DEFAULT_AVATAR_NAME);
            if (!Files.exists(defaultAvatarPath)) {  // 默认头像不存在
                response.setHeader("Content-Type", "application/json;charset=UTF-8");
                response.setStatus(HttpStatus.OK.value());
                try (PrintWriter writer = response.getWriter()) {
                    writer.print("请在头像目录下放置默认头像default_avatar.jpg");
                } catch (Exception e) {
                    logger.error("输出无默认图失败", e);
                }
                return;
            }
            avatarPath = defaultAvatarPath;
        }
        FileTools.readFile(response, avatarPath);
    }

    @GetMapping(value = "/getUserInfo")
    @RequiresLogin
    public ResponseVo<LoginUser> getUserInfo() {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        return ResponseVo.ok(loginUser);
    }

    @GetMapping("/getUsedSpace")
    @RequiresLogin
    public ResponseVo<UserSpaceDto> getUserSpaceInfo() {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        return ResponseVo.ok(redisComponent.getUserSpaceInfo(loginUser.getUserId()));
    }

    @PostMapping("/logout")
    public ResponseVo<Void> logout(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, "token");
        String token = cookie == null ? null : cookie.getValue();

        if (token != null && !token.isBlank()) {
            long expireMillis = jwtUtils.getRemainingMillis(token);
            if (expireMillis > 0) {
                redisUtils.setex(
                        Constants.REDIS_KEY_TOKEN_BLACKLIST + token,
                        "1",
                        expireMillis,
                        TimeUnit.MILLISECONDS
                );
            }
        }
        return ResponseVo.ok(null);
    }

    @PostMapping("/updateUserAvatar")
    @RequiresLogin
    public ResponseVo<Map<String, String>> updateUserAvatar(HttpServletResponse response, MultipartFile avatar) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Path avatarRoot = Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER_FILE,
                Constants.FILE_FOLDER_AVATAR_NAME
        );
        try {
            Files.createDirectories(avatarRoot);
        } catch (IOException e) {
            logger.error("创建头像目录失败: {}", avatarRoot, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
        Path targetFile = avatarRoot.resolve(loginUser.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile.toFile());
        } catch (Exception e) {
            logger.error("上传头像失败, userId={}, path={}", loginUser.getUserId(), targetFile, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
        userInfoService.updateQqAvatarByUserId("", loginUser.getUserId());
        loginUser.setAvatar(null);

        String token = jwtUtils.createToken(loginUser);
        long maxAgeSeconds = jwtUtils.getDefaultMillis() / 1000;
        ResponseCookie cookie = CookieUtils.buildTokenCookie(token, maxAgeSeconds);
        response.addHeader("Set-Cookie", cookie.toString());

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ResponseVo.ok(result);
    }

    @PostMapping("/updatePassword")
    @RequiresLogin
    public ResponseVo<Void> updatePassword(
            @RequestParam @Size(min = 8, max = 18) @Pattern(regexp = VerifyRegex.PASSWORD)
            String password
    ) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        userInfoService.updatePasswordByUserId(StringTools.encodeByMD5(password), loginUser.getUserId());
        return ResponseVo.ok(null);
    }

    @GetMapping("/qqlogin")
    public ResponseVo<String> qqlogin(String callbackUrl) {
        String state = StringTools.getRandomString(Constants.QQ_LOGIN_STATE_LENGTH);
        if (!StringTools.isEmpty(callbackUrl)) {
            redisUtils.setex(Constants.REDIS_KEY_QQ_LOGIN_STATE + state, callbackUrl, Constants.REDIS_EXPIRATION_QQ_LOGIN, Constants.REDIS_TIME_UNIT_QQ_LOGIN);
        }
        String url = appProperties.getQq().getUrlAuthorization().formatted(appProperties.getQq().getAppId(), URLEncoder.encode(appProperties.getQq().getUrlRedirect(), StandardCharsets.UTF_8), state);
        return ResponseVo.ok(url);
    }

    @GetMapping("/qqlogin/callback")
    public ResponseVo<Map<String, Object>> qqLoginCallback(@NotBlank String code, @NotBlank String state) {
        String token = userInfoService.qqLogin(code);
        Map<String, Object> result = new HashMap<>();
        result.put("callbackUrl", redisUtils.get(Constants.REDIS_KEY_QQ_LOGIN_STATE + state));
        result.put("token", token);
        return ResponseVo.ok(result);
    }

    private void validateAndClearCaptcha(String redisKeyPrefix, String captchaToken, String checkCode) {
        String redisKey = redisKeyPrefix + captchaToken;
        emailCodeService.checkCode(redisKey, checkCode, true);
        redisUtils.delete(redisKey);
    }
}
