package com.easypan.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UploadStatusEnum {
    INSTANT_UPLOAD("upload_seconds", "秒传"),
    UPLOADING("uploading", "上传中"),
    UPLOAD_FINISH("upload_finish", "上传完成");

    private final String code;
    private final String desc;

}
