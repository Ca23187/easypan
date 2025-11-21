package com.easypan.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public final class UploadResultDto implements Serializable {
    private String fileId;
    private String status;
}
