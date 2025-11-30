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

    PaginationResultVo<FileInfoVo> findPageByParam(FileInfoQuery query);

    UploadResultDto uploadFile(LoginUser loginUser, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    FileInfo findFirstByFileId(String realFileId);

    boolean existsByFilePathAndUserId(String filePath, String userId);

    FileInfo findById(FileInfoId fileInfoId);

    FileInfoVo createFolder(String filePid, String userId, String folderName);

    List<FileInfoVo> getFolderInfoVoList(String path, String userId);

    FileInfoVo rename(String fileId, String userId, String fileName);

    List<FileInfoVo> findMovableTargetFolders(String userId, String filePid, String currentFileIds);

    void changeFileFolder(String fileIds, String filePid, String userId);
}
