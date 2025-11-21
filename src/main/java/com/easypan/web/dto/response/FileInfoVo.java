package com.easypan.web.dto.response;

import com.easypan.infra.jpa.entity.QFileInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.querydsl.core.annotations.QueryProjection;
import com.querydsl.core.types.Expression;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


@Getter
@Setter
public class FileInfoVo {

    @QueryProjection
    public FileInfoVo(String fileId, String filePid, Long fileSize, String fileName, String fileCover, LocalDateTime recoveryTime, LocalDateTime lastUpdateTime, Integer folderType, Integer fileCategory, Integer fileType, Integer status) {
        this.fileId = fileId;
        this.filePid = filePid;
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.fileCover = fileCover;
        this.recoveryTime = recoveryTime;
        this.lastUpdateTime = lastUpdateTime;
        this.folderType = folderType;
        this.fileCategory = fileCategory;
        this.fileType = fileType;
        this.status = status;
    }

    // NOTE: 提供让 queryDSL 从实体类映射到 VO 类的 projection 的方法，注意必须与 @QueryProjection + 全构造函数一起使用
    public static Expression<FileInfoVo> selectBase(QFileInfo f) {
        return new QFileInfoVo(
                f.fileId,
                f.filePid,
                f.fileSize,
                f.fileName,
                f.fileCover,
                f.recoveryTime,
                f.lastUpdateTime,
                f.folderType,
                f.fileCategory,
                f.fileType,
                f.status
        );
    }

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 父级ID
     */
    private String filePid;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 封面
     */
    private String fileCover;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recoveryTime;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateTime;


    /**
     * 0:文件 1:目录
     */
    private Integer folderType;

    /**
     * 1:视频 2:音频  3:图片 4:文档 5:其他
     */
    private Integer fileCategory;

    /**
     * 1:视频 2:音频  3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他
     */
    private Integer fileType;

    /**
     * 0:转码中 1转码失败 2:转码成功
     */
    private Integer status;

}
