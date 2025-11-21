package com.easypan.common.exception;

import com.easypan.common.response.ResponseCodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private ResponseCodeEnum codeEnum;
    private Integer code;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(ResponseCodeEnum codeEnum) {
        super(codeEnum.getMsg());
        this.codeEnum = codeEnum;
        this.code = codeEnum.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.codeEnum = null;
        this.code = code;
    }

    /**
     * 重写fillInStackTrace 业务异常不需要堆栈信息，提高效率.
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}