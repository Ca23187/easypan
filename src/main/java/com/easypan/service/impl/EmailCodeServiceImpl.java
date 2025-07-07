package com.easypan.service.impl;

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.po.EmailCode;
import com.easypan.entity.po.EmailCodeId;
import com.easypan.entity.po.UserInfo;
import com.easypan.exception.BusinessException;
import com.easypan.repository.EmailCodeRepository;
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
import java.util.Objects;

@Service
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Resource
    private UserInfoRepository userInfoRepository;

    @Resource
    private EmailCodeRepository emailCodeRepository;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, Integer type) {
        if (Objects.equals(type, Constants.ZERO)) {
            UserInfo userInfo = userInfoRepository.findByEmail(email);
            if (null != userInfo) {
                throw new BusinessException("邮箱已经存在");
            }
        }
        String code = StringTools.getRandomNumber(Constants.LENGTH_5);

        // 发送验证码
        sendEmailCode(email, code);

        // 让之前发的所有验证码都无效
        emailCodeRepository.disableEmailCode(email);

        EmailCode emailCode = new EmailCode();
        emailCode.setId(new EmailCodeId(email, code));
        emailCode.setStatus(Constants.ZERO);
        emailCode.setCreateTime(new Date());
        emailCodeRepository.save(emailCode);
    }

    private void sendEmailCode(String toEmail, String code){
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
