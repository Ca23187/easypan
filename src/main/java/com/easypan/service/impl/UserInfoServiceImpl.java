package com.easypan.service.impl;

import com.easypan.component.RedisComponent;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.enums.UserStatusEnum;
import com.easypan.entity.po.UserInfo;
import com.easypan.exception.BusinessException;
import com.easypan.repository.UserInfoRepository;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.StringTools;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Resource
    private UserInfoRepository userInfoRepository;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private RedisComponent redisComponent;

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

        String userId = StringTools.getRandomNumber(Constants.LENGTH_10);
        userInfo = new UserInfo();
        userInfo.setUserId(userId);
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
}
