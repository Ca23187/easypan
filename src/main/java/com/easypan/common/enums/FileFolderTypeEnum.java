package com.easypan.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileFolderTypeEnum {
    FILE(0, "文件"),
    FOLDER(1, "目录");

    private final Integer type;
    private final String desc;

}
