package com.easypan.exception;

import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.vo.ResponseVO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice  // 跟ControllerAdvice的区别只是自动为每一个方法加了@RequestBody注解
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseVO<?> handleValidationExceptions(MethodArgumentNotValidException e, HttpServletRequest request) {
        logger.error("参数校验错误，地址:{}，错误信息: {}", request.getRequestURL(), e.getMessage());
        return ResponseVO.error(ResponseCodeEnum.CODE_600.getCode(), e.getMessage());
    }
    // 处理 404 - 资源未找到
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseVO<?> handleNotFound(NoHandlerFoundException e, HttpServletRequest request) {
        logger.error("请求地址{}不存在，错误信息: {}", request.getRequestURL(), e.getMessage());
        return ResponseVO.error(ResponseCodeEnum.CODE_404);
    }

    // 处理自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseVO<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        logger.error("业务异常，地址:{}，错误信息: {}", request.getRequestURL(), e.getMessage());
        return ResponseVO.error(e.getCode() == null ? ResponseCodeEnum.CODE_600.getCode() : e.getCode(), e.getMessage());
    }

    // 参数绑定异常
    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
    public ResponseVO<?> handleBindException(Exception e, HttpServletRequest request) {
        logger.error("参数异常，地址:{}，错误信息: {}", request.getRequestURL(), e.getMessage());
        return ResponseVO.error(ResponseCodeEnum.CODE_600);
    }

    // 唯一键冲突异常
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseVO<?> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        logger.error("数据库主键冲突，地址:{}，错误信息: {}", request.getRequestURL(), e.getMessage());
        return ResponseVO.error(ResponseCodeEnum.CODE_601);
    }

    // 其他所有异常
    @ExceptionHandler(Exception.class)
    public ResponseVO<?> handleException(Exception e, HttpServletRequest request) {
        logger.error("系统异常，地址:{}，错误信息:", request.getRequestURL(), e);
        return ResponseVO.error(ResponseCodeEnum.CODE_500);
    }
}