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
    public static <T> ResponseVO<T> ok(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(Constants.STATUS_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }
}