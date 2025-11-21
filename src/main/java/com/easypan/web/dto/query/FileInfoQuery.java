package com.easypan.web.dto.query;

import com.easypan.infra.jpa.entity.FileInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * 文件信息参数
 */
@Setter
@Getter
public class FileInfoQuery extends FileInfo {

    private String fileIdFuzzy;

    private String userIdFuzzy;

    private String fileMd5Fuzzy;

    private String filePidFuzzy;

    private String fileNameFuzzy;

    private String fileCoverFuzzy;

    private String filePathFuzzy;

    private String createTimeStart;

    private String createTimeEnd;

    private String lastUpdateTimeStart;

    private String lastUpdateTimeEnd;

    private String recoveryTimeStart;

    private String recoveryTimeEnd;

    private String[] fileIdArray;

    private String[] filePidArray;

    private String[] excludeFileIdArray;

    private Boolean queryExpire;

    private Boolean queryNickname;

    private Integer pageNo;

    private Integer pageSize;

    private String orderBy;
}
