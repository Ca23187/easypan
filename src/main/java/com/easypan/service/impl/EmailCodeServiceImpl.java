package com.easypan.service.impl;

import com.easypan.component.RedisComponent;
import com.easypan.component.RedisUtils;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.po.UserInfo;
import com.easypan.exception.BusinessException;
import com.easypan.repository.UserInfoRepository;
import com.easypan.service.EmailCodeService;
import com.easypan.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Resource
    private UserInfoRepository userInfoRepository;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private RedisUtils<String> redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, Integer type) {
        if (type != null && type == 0) {  // 如果是注册所需邮箱验证码
            UserInfo userInfo = userInfoRepository.findByEmail(email);
            if (null != userInfo) {
                throw new BusinessException("邮箱已经存在");
            }
        }
        String code = StringTools.getRandomNumber(Constants.EMAIL_CODE_SIZE);

        // 发送验证码
        sendEmailCodeEmail(email, code);

        // 删除旧验证码（等价于 disableEmailCode）
        redisUtils.delete(Constants.REDIS_KEY_EMAIL_CODE_PREFIX + email);

        // 保存新验证码到 Redis，过期时间 15 分钟
        redisUtils.setex(
                Constants.REDIS_KEY_EMAIL_CODE_PREFIX + email,
                code,
                Constants.REDIS_EXPIRATION_EMAIL_CODE,
                Constants.REDIS_TIME_UNIT_CHECK_CODE
        );
    }

    @Override
    public void checkCode(String email, String code) {
        String redisKey = Constants.REDIS_KEY_EMAIL_CODE_PREFIX + email;
        String realCode = redisUtils.get(redisKey);

        if (realCode == null) {
            throw new BusinessException("邮箱验证码已失效");
        }
        else if (!realCode.equals(code)) {
            throw new BusinessException("邮箱验证码不正确");
        }

        // 验证通过后删除验证码（一次性使用）
        redisUtils.delete(redisKey);
    }

    private void sendEmailCodeEmail(String toEmail, String code){
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(appConfig.getSendUserName());
            helper.setTo(toEmail);

            SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
            helper.setSubject(sysSettingsDto.getRegisterEmailTitle());
            helper.setText(String.format(sysSettingsDto.getRegisterEmailContent(), code));
            helper.setSentDate(new Date());
            javaMailSender.send(message);
        } catch (MessagingException e) {
            logger.error("邮件发送失败", e);
            throw new BusinessException("邮件发送失败");
        }
    }
}
