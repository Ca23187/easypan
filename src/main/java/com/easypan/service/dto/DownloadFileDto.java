package com.easypan.service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadFileDto {
    private String downloadCode;
    private String fileName;
    private String filePath;
}
