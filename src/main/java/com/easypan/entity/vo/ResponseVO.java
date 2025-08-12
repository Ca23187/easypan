package com.easypan.entity.vo;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.enums.ResponseCodeEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseVO<T> {
    private String status;
    private Integer code;
    private String info;
    private T data;

    public static ResponseVO<Void> ok() {
        ResponseVO<Void> responseVO = new ResponseVO<>();
        responseVO.setStatus(Constants.STATUS_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        return responseVO;
    }

    public static <T> ResponseVO<T> ok(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(Constants.STATUS_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    public static ResponseVO<Void> error(ResponseCodeEnum codeEnum) {
        ResponseVO<Void> responseVO = new ResponseVO<>();
        responseVO.setStatus(Constants.STATUS_ERROR);
        responseVO.setCode(codeEnum.getCode());
        responseVO.setInfo(codeEnum.getMsg());
        return responseVO;
    }

    public static ResponseVO<Void> error(Integer code, String message) {
        ResponseVO<Void> responseVO = new ResponseVO<>();
        responseVO.setStatus(Constants.STATUS_ERROR);
        responseVO.setCode(code);
        responseVO.setInfo(message);
        return responseVO;
    }
}