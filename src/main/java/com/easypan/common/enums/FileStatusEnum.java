package com.easypan.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileStatusEnum {
    TRANSFER(0, "转码中"),
    TRANSFER_FAIL(1, "转码失败"),
    USING(2, "使用中");

    private final Integer status;
    private final String desc;
}
