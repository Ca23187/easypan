package com.easypan.infra.jpa.repository;

import com.easypan.infra.jpa.entity.FileInfo;
import com.easypan.infra.jpa.entity.FileInfoId;
import com.easypan.service.dto.UserSpaceDto;
import com.easypan.web.dto.response.FileInfoVo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FileInfoRepository extends JpaRepository<FileInfo, FileInfoId> {
    @Modifying
    @Query("""
        update FileInfo f
        set f.fileSize = :fileSize,
            f.fileCover = :fileCover,
            f.status = :newStatus
        where f.fileId = :fileId
          and f.userId = :userId
          and f.status = :oldStatus
        """)
    void updateFileStatusWithOldStatus(String fileId, String userId, Integer oldStatus,
                                       Long fileSize, String fileCover, Integer newStatus);

    FileInfo findFirstByFileId(String fileId);

    boolean existsByFilePathAndUserId(String filePath, String userId);

    FileInfo findFirstByFileMd5AndStatus(String fileMd5, Integer status);

    boolean existsByUserId_AndFilePid_AndDelFlag_AndFileName(String userId, String filePid, Integer flag, String fileName);

    @Query("""
        select new com.easypan.service.dto.UserSpaceDto(u.usedSpace, u.totalSpace)
        from UserInfo u
        where u.userId = :userId
    """)
    UserSpaceDto findUserSpaceDtoByUserId(String userId);

    FileInfo findFirstByUserId_AndFolderType_AndFileName_AndFilePid_AndDelFlag(String userId, Integer folderType, String fileName, String filePid, Integer DelFlag);

    @Query("""
    select new com.easypan.web.dto.response.FileInfoVo(
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
    )
    from FileInfo f where f.userId = :userId and f.folderType = :folderType and f.fileId in :ids
    """)
    List<FileInfoVo> findFolderInfoVoList(String userId, Integer folderType, List<String> ids);

    FileInfoVo findVoByFileIdAndUserIdAndDelFlag(String fileId, String fileId1, Integer delFlag);

    @Modifying
    @Query("""
    update FileInfo f set f.fileName = :newFileName, f.lastUpdateTime = :now
    where f.fileId = :fileId and f.userId = :userId and f.fileName = :oldName and f.delFlag = :delFlag
    """)
    int renameWithOldName(String newFileName, LocalDateTime now, String fileId, String userId, String oldName, Integer delFlag);

    @Query("""
    select new com.easypan.web.dto.response.FileInfoVo(
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
    ) from FileInfo f where
        f.userId = :userId and f.filePid = :filePid and f.folderType = :folderType
            and f.delFlag = :delFlag
            and f.fileId not in :fileIds
      order by f.createTime desc
    """)
    List<FileInfoVo> findMovableTargetFolders(String userId, String filePid, Integer folderType, Integer delFlag, List<String> fileIds);

    List<FileInfo> findByUserIdAndFilePid(String userId, String filePid);

    List<FileInfo> findByUserIdAndFileIdIn(String userId, List<String> fileIds);

    List<FileInfo> findByUserIdAndDelFlagAndFileIdIn(String userId, Integer delFlag, Collection<String> fileIds);

    @Modifying
    @Query("""
    update FileInfo f
       set f.delFlag = :newDelFlag,
           f.recoveryTime = :recoveryTime
     where f.userId  = :userId
       and f.fileId  in :fileIds
       and f.delFlag = :oldDelFlag
    """)
    int updateDelFlagAndRecoveryTimeByFileIds(
            Integer newDelFlag,
            LocalDateTime recoveryTime,
            String userId,
            List<String> fileIds,
            Integer oldDelFlag);

    List<FileInfo> findByUserIdAndFilePidAndDelFlag(String userId, String fileId, Integer delFlag);
}
