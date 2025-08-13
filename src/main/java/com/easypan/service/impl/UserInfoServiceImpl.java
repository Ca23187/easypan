package com.easypan.service.impl;

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.dto.UserInfoDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.enums.UserStatusEnum;
import com.easypan.entity.po.UserInfo;
import com.easypan.exception.BusinessException;
import com.easypan.repository.UserInfoRepository;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.JwtUtil;
import com.easypan.utils.StringTools;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Resource
    private UserInfoRepository userInfoRepository;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

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
        emailCodeService.checkCode(email, emailCode);

        userInfo = new UserInfo();
        userInfo.setUserId(UUID.randomUUID().toString());
        userInfo.setNickname(nickname);
        userInfo.setEmail(email);
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfo.setJoinTime(new Date());
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
        userInfo.setTotalSpace(sysSettingsDto.getUserInitUseSpace() * Constants.MB);
        userInfo.setUseSpace(0L);
        userInfoRepository.save(userInfo);
    }

    @Override
    @Transactional
    public String login(String email, String password) {
        UserInfo userInfo = userInfoRepository.findByEmail(email);
        if (null == userInfo || !userInfo.getPassword().equals(password)) {
            throw new BusinessException("账号或者密码错误");
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException("账号已禁用");
        }
        userInfoRepository.updateLastLoginTimeByUserId(new Date(), userInfo.getUserId());

        UserInfoDto userInfoDto = new UserInfoDto();
        userInfoDto.setUserId(userInfo.getUserId());
        userInfoDto.setNickName(userInfo.getNickname());
        userInfoDto.setAvatar(userInfo.getQqAvatar());
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), email)) {
            userInfoDto.setIsAdmin(true);
        } else {
            userInfoDto.setIsAdmin(false);
        }
        //用户空间
        UserSpaceDto userSpaceDto = new UserSpaceDto();
//        userSpaceDto.setUseSpace(fileInfoService.getUserUseSpace(userInfo.getUserId()));
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisComponent.saveUserSpaceUse(userInfo.getUserId(), userSpaceDto);

        // 生成token
        return JwtUtil.createToken(userInfoDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(String email, String password, String emailCode) {
        UserInfo userInfo = userInfoRepository.findByEmail(email);
        if (null == userInfo) {
            throw new BusinessException("邮箱账号不存在");
        }
        //校验邮箱验证码
        emailCodeService.checkCode(email, emailCode);
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfoRepository.save(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQqAvatarByUserId(String s, String userId) {
        userInfoRepository.updateQqAvatarByUserId(s, userId);
    }

    @Override
    public void updatePasswordByUserId(String s, String userId) {
        userInfoRepository.updatePasswordByUserId(s, userId);
    }


}
