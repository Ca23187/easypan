package com.easypan.infra.jpa.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoId implements Serializable {
    /**
     * 文件ID
     */
    private String fileId;
    /**
     * 用户ID
     */
    private String userId;
}
