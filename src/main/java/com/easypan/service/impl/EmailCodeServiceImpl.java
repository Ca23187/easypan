package com.easypan.service.impl;

import com.easypan.infra.redis.RedisComponent;
import com.easypan.infra.redis.RedisUtils;
import com.easypan.config.AppProperties;
import com.easypan.common.constants.Constants;
import com.easypan.service.dto.SysSettingsDto;
import com.easypan.infra.jpa.entity.UserInfo;
import com.easypan.common.exception.BusinessException;
import com.easypan.infra.jpa.repository.UserInfoRepository;
import com.easypan.service.EmailCodeService;
import com.easypan.common.util.StringTools;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Resource
    private UserInfoRepository userInfoRepository;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private AppProperties appProperties;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private RedisUtils<String> redisUtils;

    @Override
    public void sendEmail(String email, Integer type) {
        // 1. 基本校验
        if (StringTools.isEmpty(email)) {
            throw new BusinessException("邮箱不能为空");
        }

        // 2. 注册场景：校验邮箱是否已存在
        if (type != null && type.equals(Constants.EMAIL_CODE_TYPE_REGISTER)) {
            UserInfo userInfo = userInfoRepository.findByEmail(email);
            if (userInfo != null) {
                throw new BusinessException("邮箱已经存在");
            }
        }

        // 3. 生成验证码
        String code = StringTools.getRandomNumber(Constants.EMAIL_CODE_LENGTH);

        // 4. 读取系统配置（标题、模板）
        SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
        String subject = sysSettingsDto.getRegisterEmailTitle();
        String content = String.format(sysSettingsDto.getRegisterEmailContent(), code);

        // 5. 发送邮件
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, false); // 如果你模板是 HTML，可以改为 true
            helper.setSentDate(new Date());
            javaMailSender.send(message);
        } catch (MessagingException e) {
            logger.error("邮件发送失败，email={}", email, e);
            throw new BusinessException("邮件发送失败，请稍后再试");
        }

        // 6. 保存验证码到 Redis
        String redisKey = Constants.REDIS_KEY_EMAIL_CODE + type + ":" + email;
        redisUtils.setex(
                redisKey,
                code,
                Constants.REDIS_EXPIRATION_EMAIL_CODE,
                Constants.REDIS_TIME_UNIT_CHECK_CODE
        );
    }

    @Override
    public void checkCode(String redisKey, String checkCode, boolean isCaptcha) {
        String scene = isCaptcha ? "图片" : "邮箱";

        if (StringTools.isEmpty(checkCode)) {
            throw new BusinessException(scene + "验证码不能为空");
        }

        String realCode = redisUtils.get(redisKey);
        if (realCode == null) {
            throw new BusinessException(scene + "验证码已失效，请重新获取");
        }

        if (!checkCode.trim().equalsIgnoreCase(realCode.trim())) {
            if (isCaptcha) redisUtils.delete(redisKey);
            throw new BusinessException(scene + "验证码不正确");
        }
    }
}
