package com.easypan.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileCategoryEnum {
    VIDEO(1, "video", "视频"),
    MUSIC(2, "music", "音频"),
    IMAGE(3, "image", "图片"),
    DOC(4, "doc", "文档"),
    OTHERS(5, "others", "其他");

    private final Integer category;
    private final String code;
    private final String desc;

    public static FileCategoryEnum getByCode(String code) {
        for (FileCategoryEnum item : FileCategoryEnum.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }
}
