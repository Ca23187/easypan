package com.easypan.infra.jpa.entity;

import com.easypan.common.enums.DateTimePatternEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件信息
 */
@Setter
@Getter
@IdClass(FileInfoId.class)
@Entity
public class FileInfo implements Serializable {

    /**
     * 文件ID
     */
    @Id
    private String fileId;

    /**
     * 用户ID
     */
    @Id
    private String userId;

    /**
     * md5值，第一次上传记录
     */
    private String fileMd5;

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
     * 文件路径
     */
    private String filePath;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
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

    /**
     * 回收站时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recoveryTime;

    /**
     * 删除标记 0:删除  1:回收站  2:正常
     */
    private Integer delFlag;

    @Override
    public String toString() {
        return """
           文件ID: %s
           用户ID: %s
           md5值，第一次上传记录: %s
           父级ID: %s
           文件大小: %s
           文件名称: %s
           封面: %s
           文件路径: %s
           创建时间: %s
           最后更新时间: %s
           0:文件 1:目录: %s
           1:视频 2:音频 3:图片 4:文档 5:其他: %s
           1:视频 2:音频 3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他: %s
           0:转码中 1转码失败 2:转码成功: %s
           回收站时间: %s
           删除标记 0:删除 1:回收站 2:正常: %s
           """.formatted(
                fileId == null ? "空" : fileId,
                userId == null ? "空" : userId,
                fileMd5 == null ? "空" : fileMd5,
                filePid == null ? "空" : filePid,
                fileSize == null ? "空" : fileSize,
                fileName == null ? "空" : fileName,
                fileCover == null ? "空" : fileCover,
                filePath == null ? "空" : filePath,
                createTime == null ? "空" : createTime.format(DateTimeFormatter.ofPattern(DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())),
                lastUpdateTime == null ? "空" : lastUpdateTime.format(DateTimeFormatter.ofPattern(DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())),
                folderType == null ? "空" : folderType,
                fileCategory == null ? "空" : fileCategory,
                fileType == null ? "空" : fileType,
                status == null ? "空" : status,
                recoveryTime == null ? "空" : recoveryTime.format(DateTimeFormatter.ofPattern(DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())),
                delFlag == null ? "空" : delFlag
        );
    }

}
