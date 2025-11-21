package com.easypan.infra.jpa.repository;

import com.easypan.infra.jpa.entity.FileInfo;
import com.easypan.infra.jpa.entity.FileInfoId;
import com.easypan.service.dto.UserSpaceDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FileInfoRepository extends JpaRepository<FileInfo, FileInfoId> {

    // NOTE: JPQL 里没有 IFNULL，需要用 coalesce
    @Query("""
        select coalesce(sum(f.fileSize), 0)
            from FileInfo f
            where f.userId = :userId
        """)
    Long findUsedSpaceByUserId(String userId);

    List<FileInfo> findByFileIdAndUserId(String fileId, String userId);

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

    List<FileInfo> findByFileId(String fileId);

    int countByFilePathAndUserId(String filePath, String userId);

    List<FileInfo> findByFileMd5AndStatus(String fileMd5, Integer status);

    long countByUserId_AndFilePid_AndDelFlag_AndFileName(String userId, String filePid, Integer flag, String fileName);

    @Query("""
        select new com.easypan.service.dto.UserSpaceDto(u.usedSpace, u.totalSpace)
        from UserInfo u
        where u.userId = :userId
    """)
    UserSpaceDto findUserSpaceDtoByUserId(String userId);
}
