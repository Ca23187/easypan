package com.easypan.service.impl;

import com.easypan.common.constants.Constants;
import com.easypan.common.enums.UserStatusEnum;
import com.easypan.common.exception.BusinessException;
import com.easypan.common.util.JsonUtils;
import com.easypan.common.util.OKHttpUtils;
import com.easypan.common.util.StringTools;
import com.easypan.config.AppProperties;
import com.easypan.infra.jpa.entity.UserInfo;
import com.easypan.infra.jpa.repository.UserInfoRepository;
import com.easypan.infra.redis.RedisComponent;
import com.easypan.infra.secure.JwtUtils;
import com.easypan.infra.secure.LoginUser;
import com.easypan.service.EmailCodeService;
import com.easypan.service.FileInfoService;
import com.easypan.service.UserInfoService;
import com.easypan.service.dto.QQInfoDto;
import com.easypan.service.dto.SysSettingsDto;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    private final static Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);

    @Resource
    private UserInfoRepository userInfoRepository;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppProperties appProperties;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickname, String password, String emailCode) {
        UserInfo userInfo = userInfoRepository.findByEmailOrNickname(email, nickname);
        if (null != userInfo) {
            if (email.equals(userInfo.getEmail())) {
                throw new BusinessException("邮箱账号已经存在");
            }
            else {
                throw new BusinessException("昵称已经存在");
            }
        }
        //校验邮箱验证码
        emailCodeService.checkCode(Constants.REDIS_KEY_EMAIL_CODE + email, emailCode, false);

        userInfo = new UserInfo();
        userInfo.setUserId(UUID.randomUUID().toString());
        userInfo.setNickname(nickname);
        userInfo.setEmail(email);
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfo.setJoinTime(LocalDateTime.now());
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
        userInfo.setTotalSpace(sysSettingsDto.getUserInitTotalSpace() * Constants.MB);
        userInfo.setUsedSpace(0L);
        userInfoRepository.save(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String login(String email, String password) {
        UserInfo userInfo = userInfoRepository.findByEmail(email);
        if (null == userInfo || !userInfo.getPassword().equals(password)) {
            throw new BusinessException("账号或者密码错误");
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException("账号已禁用");
        }
        userInfoRepository.updateLastLoginTimeByUserId(LocalDateTime.now(), userInfo.getUserId());

        // 生成token
        return buildLoginToken(userInfo, userInfo.getQqAvatar());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(String email, String password, String emailCode) {
        UserInfo userInfo = userInfoRepository.findByEmail(email);
        if (null == userInfo) {
            throw new BusinessException("邮箱账号不存在");
        }
        //校验邮箱验证码
        emailCodeService.checkCode(Constants.REDIS_KEY_EMAIL_CODE + email, emailCode, false);
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfoRepository.save(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQqAvatarByUserId(String s, String userId) {
        userInfoRepository.updateQqAvatarByUserId(s, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePasswordByUserId(String s, String userId) {
        userInfoRepository.updatePasswordByUserId(s, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String qqLogin(String code) {
        String accessToken = getQQAccessToken(code);
        String openId = getQQOpenId(accessToken);
        UserInfo user = userInfoRepository.findByQqOpenId(openId);
        String avatar = null;
        if (null == user) {
            QQInfoDto qqInfo = getQQUserInfo(accessToken, openId);
            String nickname = buildUniqueNickName(qqInfo.getNickname(), openId);
            LocalDateTime now = LocalDateTime.now();
            user = new UserInfo();

            //上传头像到本地
            user.setQqOpenId(openId);
            user.setJoinTime(now);
            user.setNickname(nickname);
            user.setQqAvatar(avatar);
            user.setUserId(UUID.randomUUID().toString());
            user.setLastLoginTime(now);
            user.setStatus(UserStatusEnum.ENABLE.getStatus());
            user.setUsedSpace(0L);
            user.setTotalSpace(redisComponent.getSysSettingsDto().getUserInitTotalSpace() * Constants.MB);
            userInfoRepository.save(user);
        } else {
            userInfoRepository.updateLastLoginTimeByQqOpenId(LocalDateTime.now(), openId);
            avatar = user.getQqAvatar();
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(user.getStatus())) {
            throw new BusinessException("账号被禁用无法登录");
        }
        return buildLoginToken(user, avatar);
    }

    private String getQQAccessToken(String code) {
        String accessToken = null;
        String url = null;
        url = appProperties.getQq().getUrlAccessToken().formatted(
                appProperties.getQq().getAppId(),
                appProperties.getQq().getAppKey(),
                code,
                URLEncoder.encode(appProperties.getQq().getUrlRedirect(), StandardCharsets.UTF_8));
        String tokenResult = OKHttpUtils.getRequest(url, null);
        if (tokenResult == null || tokenResult.contains(Constants.VIEW_OBJ_RESULT_KEY)) {
            logger.error("获取qqToken失败:{}", tokenResult);
            throw new BusinessException("获取qqToken失败");
        }
        String[] params = tokenResult.split("&");
        for (String p : params) {
            if (p.contains("access_token")) {
                accessToken = p.split("=")[1];
                break;
            }
        }
        return accessToken;
    }


    private String getQQOpenId(String accessToken) {
        // 获取openId
        String url = appProperties.getQq().getUrlOpenId().formatted(accessToken);
        String openIDResult = OKHttpUtils.getRequest(url, null);
        String tmpJson = this.getQQResp(openIDResult);
        if (tmpJson == null) {
            logger.error("调qq接口获取openID失败:tmpJson{}", tmpJson);
            throw new BusinessException("调qq接口获取openID失败");
        }
        Map jsonData = JsonUtils.toObj(tmpJson, Map.class);
        if (jsonData == null || jsonData.containsKey(Constants.VIEW_OBJ_RESULT_KEY)) {
            logger.error("调qq接口获取openID失败:{}", jsonData);
            throw new BusinessException("调qq接口获取openID失败");
        }
        return String.valueOf(jsonData.get("openid"));
    }


    private QQInfoDto getQQUserInfo(String accessToken, String qqOpenId) {
        String url = appProperties.getQq().getUrlUserInfo().formatted(accessToken, appProperties.getQq().getAppId(), qqOpenId);
        String response = OKHttpUtils.getRequest(url, null);
        if (StringUtils.isNotBlank(response)) {
            QQInfoDto qqInfo = JsonUtils.toObj(response, QQInfoDto.class);
            if (qqInfo.getRet() != 0) {
                logger.error("qqInfo:{}", response);
                throw new BusinessException("调qq接口获取用户信息异常");
            }
            return qqInfo;
        }
        throw new BusinessException("调qq接口获取用户信息异常");
    }

    private String getQQResp(String result) {
        if (StringUtils.isNotBlank(result)) {
            // 使用正则提取括号里的内容
            Pattern pattern = Pattern.compile("callback\\s*\\((.*)\\)"); // 捕获括号内内容
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                return matcher.group(1).trim(); // 去掉前后空格
            }
        }
        return null;
    }

    private String buildUniqueNickName(String nickname, String openId) {
        if (nickname == null) {
            nickname = "QQ用户"; // 默认昵称
        }
        int maxNickLength = 15; // 昵称最长 15 个字符
        int suffixLength = 5;   // openId 后 5 位作为后缀

        // 处理原始昵称长度，按 Unicode 字符截断，兼容 emoji
        int nickCodePoints = nickname.codePointCount(0, nickname.length());
        if (nickCodePoints > maxNickLength) {
            nickname = nickname.substring(0, nickname.offsetByCodePoints(0, maxNickLength));
        }

        // 获取 openId 后 5 位
        String suffix;
        if (openId.length() >= suffixLength) {
            suffix = openId.substring(openId.length() - suffixLength);
        } else {
            suffix = openId; // openId 不足 5 位就全部使用
        }
        return nickname + suffix;
    }

    private String buildLoginToken(UserInfo user, String avatar) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(avatar);
        loginUser.setIsAdmin(
                appProperties.getAdminEmails().contains(
                        user.getEmail() == null ? "" : user.getEmail()
                )
        );
        return jwtUtils.createToken(loginUser);
    }

}
