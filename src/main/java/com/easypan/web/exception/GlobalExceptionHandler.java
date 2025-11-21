package com.easypan.web.exception;

import com.easypan.common.exception.BusinessException;
import com.easypan.common.response.ResponseCodeEnum;
import com.easypan.common.response.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Optional;

@RestControllerAdvice  // 等价于 @ControllerAdvice + 所有方法都加上 @ResponseBody
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseVo<?> handleValidationExceptions(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = Optional.ofNullable(e.getBindingResult().getFieldError())
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");

        logger.warn("参数校验错误，URL={}, 错误={}", request.getRequestURL(), msg);
        return ResponseVo.error(ResponseCodeEnum.BAD_REQUEST.getCode(), msg);
    }

    /** 参数绑定异常 */
    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
    public ResponseVo<?> handleBindException(Exception e, HttpServletRequest request) {
        logger.warn("参数绑定异常，URL={}, 错误={}", request.getRequestURL(), e.getMessage());
        return ResponseVo.error(ResponseCodeEnum.BAD_REQUEST);
    }

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseVo<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        logger.warn("业务异常，URL={}, 错误={}", request.getRequestURL(), e.getMessage());
        int code = e.getCode() == null ? ResponseCodeEnum.BAD_REQUEST.getCode() : e.getCode();
        return ResponseVo.error(code, e.getMessage());
    }

    /** 主键冲突 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseVo<?> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        logger.error("数据库主键冲突，URL={}, 错误={}", request.getRequestURL(), e.getMessage());
        return ResponseVo.error(ResponseCodeEnum.ALREADY_EXISTS);
    }

    /** 404 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseVo<?> handleNotFound(NoHandlerFoundException e, HttpServletRequest request) {
        logger.warn("地址不存在，URL={}, 错误={}", request.getRequestURL(), e.getMessage());
        return ResponseVo.error(ResponseCodeEnum.NOT_FOUND);
    }

    /** 其他所有异常 */
    @ExceptionHandler(Exception.class)
    public ResponseVo<?> handleException(Exception e, HttpServletRequest request) {
        logger.error("系统异常，URL={}", request.getRequestURL(), e);
        return ResponseVo.error(ResponseCodeEnum.INTERNAL_ERROR);
    }
}
