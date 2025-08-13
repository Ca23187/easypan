package com.easypan.controller;

import com.easypan.annotation.RequiresLogin;
import com.easypan.component.RedisComponent;
import com.easypan.component.RedisUtils;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.constants.VerifyRegex;
import com.easypan.entity.dto.UserInfoDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.interceptor.LoginUserInfoHolder;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.CreateImageCode;
import com.easypan.utils.FileUtils;
import com.easypan.utils.JwtUtil;
import com.easypan.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

@RestController("userInfoController")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisUtils<String> redisUtils;

    @Resource
    private RedisComponent redisComponent;

    @Validated
    @GetMapping("/checkCode")
    public void checkCode(HttpServletResponse response, Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, Constants.CHECK_CODE_SIZE, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();

        String token = UUID.randomUUID().toString();
        String keyPrefix = (type == null || type == 0) ?
                Constants.REDIS_KEY_CHECK_CODE_PREFIX :
                Constants.REDIS_KEY_CHECK_CODE_FOR_EMAIL_PREFIX;
        String redisKey = keyPrefix + token;

        // 验证码5分钟有效期
        redisUtils.setex(
                redisKey,
                code,
                Constants.REDIS_EXPIRATION_CHECK_CODE,
                Constants.REDIS_TIME_UNIT_CHECK_CODE
        );
        response.setHeader("captcha-token", token);
        vCode.write(response.getOutputStream());
    }

    // NOTE: Spring注解还是建议封装在dto类里，但前端需要把传参方式从fromData转为Content-Type: application/json
    @RequestMapping("/sendEmailCode")
    public ResponseVO<Void> sendEmailCode(
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
        String redisKey = Constants.REDIS_KEY_CHECK_CODE_FOR_EMAIL_PREFIX + captchaToken;
        String realCode = redisUtils.get(redisKey);
        if (realCode == null) {
            throw new BusinessException("验证码已失效，请刷新验证码");
        }
        if (!checkCode.equalsIgnoreCase(realCode)) {
            redisUtils.delete(redisKey);
            throw new BusinessException("图片验证码不正确");
        }

        // 验证通过后立即删除验证码，防止重复使用
        redisUtils.delete(redisKey);
        emailCodeService.sendEmailCode(email, type);
        return ResponseVO.ok();
    }

    @RequestMapping("/register")
    public ResponseVO<Void> register(
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
        String redisKey = Constants.REDIS_KEY_CHECK_CODE_PREFIX + captchaToken;
        String realCode = redisUtils.get(redisKey);
        if (realCode == null) {
            throw new BusinessException("验证码已失效，请刷新验证码");
        }
        if (!checkCode.equalsIgnoreCase(realCode)) {
            redisUtils.delete(redisKey);
            throw new BusinessException("图片验证码不正确");
        }
        redisUtils.delete(redisKey);
        userInfoService.register(email, nickName, password, emailCode);
        return ResponseVO.ok();
    }
    @RequestMapping("/login")
    public ResponseVO<String> login(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam @NotBlank(message = "图片验证码不能为空") String checkCode,
            @RequestParam @NotBlank(message = "验证码Token不能为空") String captchaToken
    ) {
        String redisKey = Constants.REDIS_KEY_CHECK_CODE_PREFIX + captchaToken;
        String realCode = redisUtils.get(redisKey);
        if (realCode == null) {
            throw new BusinessException("验证码已失效，请刷新验证码");
        }
        if (!checkCode.equalsIgnoreCase(realCode)) {
            throw new BusinessException("图片验证码不正确");
        }
        redisUtils.delete(redisKey);
        String token = userInfoService.login(email, password);
        return ResponseVO.ok(token);
    }

    @RequestMapping("/resetPwd")
    public ResponseVO<Void> resetPwd(
            @RequestParam @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确") @Size(max = 150)
            String email,

            @RequestParam @NotBlank(message = "密码不能为空") @Size(min = 8, max = 18)
            @Pattern(regexp = VerifyRegex.PASSWORD, message = "只能是数字，字母，特殊字符 8-18位")
            String password,

            @RequestParam String checkCode,

            @RequestParam String emailCode,

            @RequestParam String captchaToken
    ) {
        String redisKey = Constants.REDIS_KEY_CHECK_CODE_PREFIX + captchaToken;
        String realCode = redisUtils.get(redisKey);
        if (realCode == null) {
            throw new BusinessException("验证码已失效，请刷新验证码");
        }
        if (!checkCode.equalsIgnoreCase(realCode)) {
            redisUtils.delete(redisKey);
            throw new BusinessException("图片验证码不正确");
        }
        redisUtils.delete(redisKey);
        userInfoService.resetPwd(email, password, emailCode);
        return ResponseVO.ok();
    }

    @RequestMapping("/getAvatar/{userId}")
    // NOTE: @PathVariable 和 @RequestParam 冲突
    public void getAvatar(HttpServletResponse response, @PathVariable String userId) {
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File folder = new File(appConfig.getProjectFolder() + avatarFolderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // 找用户头像
        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarPath);
        if (!file.exists()) {  // 找默认头像
            String defaultAvatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.DEFAULT_AVATAR_NAME;
            // 如果默认头像不存在
            if (!new File(defaultAvatarPath).exists()) {
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
        response.setContentType("image/jpg");
        FileUtils.readFile(response, avatarPath);
    }

    @RequestMapping(value = "/getUserInfo")
    @RequiresLogin
    public ResponseVO<UserInfoDto> getUserInfo() {
        UserInfoDto userInfoDto = LoginUserInfoHolder.getLoginUserInfo();
        String a = userInfoDto.getNickName();
        return ResponseVO.ok(userInfoDto);
    }

    @RequestMapping("/getUseSpace")
    @RequiresLogin
    public ResponseVO<UserSpaceDto> getUseSpace() {
        UserInfoDto userInfoDto = LoginUserInfoHolder.getLoginUserInfo();
        return ResponseVO.ok(redisComponent.getUserSpaceUse(userInfoDto.getUserId()));
    }

    @RequestMapping("/updateUserAvatar")
    @RequiresLogin
    public ResponseVO<String> updateUserAvatar(MultipartFile avatar) {
        UserInfoDto userInfoDto = LoginUserInfoHolder.getLoginUserInfo();
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + userInfoDto.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
        }
        userInfoService.updateQqAvatarByUserId("", userInfoDto.getUserId());
        userInfoDto.setAvatar(null);
        String token = JwtUtil.createToken(userInfoDto);
        return ResponseVO.ok(token);
    }

    @RequestMapping("/updatePassword")
    @RequiresLogin
    public ResponseVO<Void> updatePassword(
            @RequestParam @Size(min = 8, max = 18)
            @Pattern(regexp = VerifyRegex.PASSWORD)
            String password
    ) {
        UserInfoDto userInfoDto = LoginUserInfoHolder.getLoginUserInfo();
        userInfoService.updatePasswordByUserId(StringTools.encodeByMD5(password), userInfoDto.getUserId());
        return ResponseVO.ok(null);
    }
}
