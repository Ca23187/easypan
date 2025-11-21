package com.easypan.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileDeleteFlagEnum {
    DEL(0, "删除"),
    RECYCLE (1, "回收站"),
    USING(2, "使用中");

    private final Integer flag;
    private final String desc;

}
