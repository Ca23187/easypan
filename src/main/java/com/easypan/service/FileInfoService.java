package com.easypan.service;

import com.easypan.infra.jpa.entity.FileInfo;
import com.easypan.infra.jpa.entity.FileInfoId;
import com.easypan.infra.secure.LoginUser;
import com.easypan.service.dto.UploadResultDto;
import com.easypan.web.dto.query.FileInfoQuery;
import com.easypan.web.dto.response.FileInfoVo;
import com.easypan.web.dto.response.PaginationResultVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileInfoService {
    Long findUsedSpaceByUserId(String userId);

    PaginationResultVo<FileInfoVo> findPageByParam(FileInfoQuery query);

    UploadResultDto uploadFile(LoginUser loginUser, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    List<FileInfo> findByFileId(String realFileId);

    int countByFilePathAndUserId(String filePath, String userId);

    FileInfo findById(FileInfoId fileInfoId);
}
