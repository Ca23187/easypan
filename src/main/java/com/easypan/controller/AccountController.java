package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.CreateImageCode;
import com.easypan.entity.enums.VerifyRegexEnum;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.EmailCodeService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController("userInfoController")
public class AccountController {
    @Resource
    private EmailCodeService emailCodeService;

    @GetMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkParams = true)
    public <T> ResponseVO<T> sendEmailCode(HttpSession session,
                                           @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 15) String email,
                                           @VerifyParam(required = true) String checkCode,
                                           @VerifyParam(required = true) Integer type) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("图片验证码不正确");
            }
            emailCodeService.sendEmailCode(email, type);
            return ResponseVO.ok(null);
        } finally {
            // 移除验证码
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    @RequestMapping("/register")
    @GlobalInterceptor(checkParams = true)
    public <T> ResponseVO<T> register(HttpSession session,
                                      @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 15) String email,
                                      @VerifyParam(required = true) String nickName,
                                      @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                                      @VerifyParam(required = true) String checkCode,
                                      @VerifyParam(required = true) Integer emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            return ResponseVO.ok(null);
        } finally {
            // 移除验证码
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
}
