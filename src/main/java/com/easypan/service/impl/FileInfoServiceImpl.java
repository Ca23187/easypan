package com.easypan.service.impl;

import com.easypan.common.constants.Constants;
import com.easypan.common.enums.*;
import com.easypan.common.exception.BusinessException;
import com.easypan.common.response.ResponseCodeEnum;
import com.easypan.common.util.FileTools;
import com.easypan.common.util.ScaleFilter;
import com.easypan.common.util.StringTools;
import com.easypan.config.AppProperties;
import com.easypan.infra.jpa.entity.FileInfo;
import com.easypan.infra.jpa.entity.FileInfoId;
import com.easypan.infra.jpa.entity.QFileInfo;
import com.easypan.infra.jpa.queryDsl.QueryDslUtils;
import com.easypan.infra.jpa.queryDsl.file.FileInfoQueryDSL;
import com.easypan.infra.redis.RedisComponent;
import com.easypan.infra.redis.RedisUtils;
import com.easypan.infra.secure.LoginUser;
import com.easypan.infra.jpa.repository.FileInfoRepository;
import com.easypan.infra.jpa.repository.UserInfoRepository;
import com.easypan.service.FileInfoService;
import com.easypan.service.dto.UploadResultDto;
import com.easypan.service.dto.UserSpaceDto;
import com.easypan.web.dto.query.FileInfoQuery;
import com.easypan.web.dto.response.FileInfoVo;
import com.easypan.web.dto.response.PaginationResultVo;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import jakarta.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
public class FileInfoServiceImpl implements FileInfoService {

    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);

    @Lazy  // NOTE: 自己注入自己，防止循环依赖和 AOP（如 @Transactional 和 @Async）失效
    @Resource
    private FileInfoServiceImpl self;

    @Resource
    private QueryDslUtils queryDSLUtils;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private FileInfoRepository fileInfoRepository;

    @Resource
    private UserInfoRepository userInfoRepository;

    @Resource
    private AppProperties appProperties;

    @Resource
    private RedisUtils<String> redisUtils;

    private final QFileInfo qFileInfo = QFileInfo.fileInfo;


    @Override
    public PaginationResultVo<FileInfoVo> findPageByParam(FileInfoQuery param) {
        BooleanBuilder builder = FileInfoQueryDSL.build(param);
        List<OrderSpecifier<?>> orders = List.of(qFileInfo.lastUpdateTime.desc());
        return queryDSLUtils.findPageByParam(
                qFileInfo,
                builder,
                param.getPageNo(),
                param.getPageSize(),
                orders,
                FileInfoVo.selectBase(qFileInfo)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(LoginUser loginUser, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5,
                                      Integer chunkIndex, Integer chunks) {
        Path tempFileFolder = null;
        boolean isUploadSuccessful = true;
        String userId = loginUser.getUserId();
        try {
            UploadResultDto resultDto = new UploadResultDto();
            if (StringTools.isEmpty(fileId)) {
                fileId = StringTools.getRandomString(Constants.RANDOM_FILE_ID_LENGTH);
            }
            resultDto.setFileId(fileId);
            LocalDateTime now = LocalDateTime.now();
            UserSpaceDto spaceDto = redisComponent.getUserSpaceInfo(userId);
            if (chunkIndex == 0) {
                List<FileInfo> dbFileList = fileInfoRepository.findByFileMd5AndStatus(fileMd5, FileStatusEnum.USING.getStatus());
                // 秒传
                if (!dbFileList.isEmpty()) {
                    FileInfo dbFile = dbFileList.get(0);
                    // 判断文件状态
                    if (dbFile.getFileSize() + spaceDto.getUsedSpace() > spaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.STORAGE_INSUFFICIENT);
                    }
                    FileInfo newFile = new FileInfo();
                    newFile.setFileId(fileId);
                    newFile.setUserId(userId);
                    newFile.setFilePid(filePid);
                    newFile.setFileMd5(fileMd5);
                    newFile.setFileSize(dbFile.getFileSize());
                    newFile.setFilePath(dbFile.getFilePath());
                    newFile.setFileCover(dbFile.getFileCover());
                    newFile.setFileCategory(dbFile.getFileCategory());
                    newFile.setFileType(dbFile.getFileType());
                    newFile.setFolderType(dbFile.getFolderType()); // 如果有
                    newFile.setStatus(FileStatusEnum.USING.getStatus());
                    newFile.setDelFlag(FileDeleteFlagEnum.USING.getFlag());
                    newFile.setCreateTime(now);
                    newFile.setLastUpdateTime(now);
                    newFile.setFileName(autoRename(filePid, userId, fileName));

                    fileInfoRepository.save(newFile);

                    //更新用户空间使用
                    updateUserSpaceInfo(userId, dbFile.getFileSize());
                    resultDto.setStatus(UploadStatusEnum.INSTANT_UPLOAD.getCode());
                    return resultDto;
                }
            }
            // 暂存在临时目录
            String currentUserFolderName = userId + fileId;
            tempFileFolder = Paths.get(
                    appProperties.getProjectFolder(),
                    Constants.FILE_FOLDER_TEMP,
                    currentUserFolderName
            );
            Files.createDirectories(tempFileFolder);

            // 判断是否超过剩余空间
            Long currentTempSize = redisComponent.getTempFileCurrentSize(userId, fileId);
            if (file.getSize() + currentTempSize + spaceDto.getUsedSpace() > spaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.STORAGE_INSUFFICIENT);
            }
            Path chunkPath = tempFileFolder.resolve(String.valueOf(chunkIndex));
            file.transferTo(chunkPath.toFile());

            // 更新临时大小
            redisComponent.updateTempFileCurrentSize(userId, fileId, file.getSize());

            // 不是最后一个分片，直接返回
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnum.UPLOADING.getCode());
                return resultDto;
            }

            // 最后一个分片上传完成，记录数据库，异步合并分片
            String month = now.format(DateTimeFormatter.ofPattern(DateTimePatternEnum.YYYYMM.getPattern()));
            String fileSuffix = StringTools.getFileSuffix(fileName);
            FileTypeEnum fileTypeEnum = FileTypeEnum.getFileTypeBySuffix(fileSuffix);

            // 文件夹名
            String folderName = currentUserFolderName + fileSuffix;

            // 自动重命名
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(userId);
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(autoRename(filePid, userId, fileName));
            fileInfo.setFilePath(month + "/" + folderName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(now);
            fileInfo.setLastUpdateTime(now);
            fileInfo.setFileCategory(fileTypeEnum.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnum.getType());
            fileInfo.setStatus(FileStatusEnum.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnum.FILE.getType());
            fileInfo.setDelFlag(FileDeleteFlagEnum.USING.getFlag());
            fileInfoRepository.save(fileInfo);

            Long totalSize = redisComponent.getTempFileCurrentSize(userId, fileId);
            updateUserSpaceInfo(userId, totalSize);

            // 确保只有在事务成功提交后才会调用异步 transferFile 方法
            Path finalTempFileFolder = tempFileFolder;
            // NOTE: 匿名内部类（或 lambda 表达式）只能访问外部作用域中被声明为 final 或 “有效 final” 的局部变量。
            // NOTE: tempFileFolder最开始定义了 null, 但之后赋值了，所以并非 “有效 final”，需要转 final
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 创建目标目录
                    Path targetRoot = Paths.get(appProperties.getProjectFolder(), Constants.FILE_FOLDER_FILE);
                    Path monthFolder = targetRoot.resolve(month);
                    try {
                        Files.createDirectories(monthFolder);
                    } catch (IOException e) {
                        logger.error("创建目标目录失败: {}", monthFolder, e);
                        return;
                    }
                    self.transferFile(fileInfo, finalTempFileFolder, targetRoot);
                }
            });
            resultDto.setStatus(UploadStatusEnum.UPLOAD_FINISH.getCode());
            return resultDto;
        } catch (BusinessException e) {
            isUploadSuccessful = false;
            logger.error("文件上传失败", e);
            throw e;
        } catch (Exception e) {
            isUploadSuccessful = false;
            logger.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        } finally {
            // 如果上传失败，清除临时目录
            if (tempFileFolder != null && !isUploadSuccessful) {
                redisUtils.delete(Constants.REDIS_KEY_USER_TEMP_FILE_CURRENT_SIZE + userId + fileId);
                try {
                    FileUtils.deleteDirectory(tempFileFolder.toFile());
                } catch (IOException e) {
                    logger.error("删除临时目录失败");
                }
            }
        }
    }

    @Override
    public List<FileInfo> findByFileId(String fileId) {
        return fileInfoRepository.findByFileId(fileId);
    }

    @Override
    public int countByFilePathAndUserId(String filePath, String userId) {
        return fileInfoRepository.countByFilePathAndUserId(filePath, userId);
    }

    @Override
    public FileInfo findById(FileInfoId fileInfoId) {
        return fileInfoRepository.findById(fileInfoId).orElse(null);
    }

    @Override
    public Long findUsedSpaceByUserId(String userId) {
        return fileInfoRepository.findUsedSpaceByUserId(userId);
    }

    private String autoRename(String filePid, String userId, String fileName) {
        long count = fileInfoRepository.countByUserId_AndFilePid_AndDelFlag_AndFileName(
                userId,
                filePid,
                FileDeleteFlagEnum.USING.getFlag(),
                fileName
        );
        if (count > 0) {
            return StringTools.rename(fileName);
        }
        return fileName;
    }

    private void updateUserSpaceInfo(String userId, Long fileSize) {
        int count = userInfoRepository.updateUserSpaceInfoByUserId(userId, fileSize, null);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.STORAGE_INSUFFICIENT);
        }
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceInfo(userId);
        userSpaceDto.setUsedSpace(userSpaceDto.getUsedSpace() + fileSize);
        redisComponent.saveUserSpaceInfo(userId, userSpaceDto);
    }

    @Async
    @Transactional
    public void transferFile(FileInfo fileInfo, Path tempFolder, Path targetRoot) {
        if (fileInfo == null) return;
        boolean isTransferSuccessful = true;
        Path targetFile = null;
        String cover = null;

        try {
            if (!FileStatusEnum.TRANSFER.getStatus().equals(fileInfo.getStatus())) return;

            // 目标文件路径
            targetFile = targetRoot.resolve(fileInfo.getFilePath());

            // 合并文件
            FileTools.union(tempFolder, targetFile, fileInfo.getFileName(), true);

            // 视频文件切割
            FileTypeEnum fileTypeEnum = FileTypeEnum.getByType(fileInfo.getFileType());
            if (FileTypeEnum.VIDEO == fileTypeEnum) {
                FileTools.cutFile4Video(fileInfo.getFileId(), targetFile);
                // 视频生成缩略图
                cover = fileInfo.getFilePath().split("\\.")[0] + Constants.IMAGE_PNG_SUFFIX;
                Path coverPath = targetRoot.resolve(cover);
                Files.createDirectories(coverPath.getParent());
                ScaleFilter.createCover4Video(
                        targetFile.toFile(),
                        Constants.THUMBNAIL_WIDTH,
                        coverPath.toFile()
                );
            } else if (FileTypeEnum.IMAGE == fileTypeEnum) {
                // 生成缩略图
                cover = fileInfo.getFilePath().replace(".", "_.");
                Path coverPath = targetRoot.resolve(cover);
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(
                        targetFile.toFile(),
                        Constants.THUMBNAIL_WIDTH,
                        coverPath.toFile(),
                        false);
                if (!created) {
                    FileUtils.copyFile(targetFile.toFile(), coverPath.toFile());
                }
            }
        } catch (Exception e) {
            logger.error("文件转码失败，文件Id:{},userId:{}", fileInfo.getFileId(), fileInfo.getUserId(), e);
            isTransferSuccessful = false;
        } finally {
            long size = 0;
            if (targetFile != null) {
                try {
                    if (Files.exists(targetFile)) {
                        size = Files.size(targetFile);
                    }
                } catch (IOException e) {
                    logger.error("获取文件大小失败: {}", targetFile, e);
                }
            }
            fileInfoRepository.updateFileStatusWithOldStatus(
                    fileInfo.getFileId(),
                    fileInfo.getUserId(),
                    FileStatusEnum.TRANSFER.getStatus(),
                    size,
                    cover,
                    isTransferSuccessful ? FileStatusEnum.USING.getStatus() : FileStatusEnum.TRANSFER_FAIL.getStatus()
            );
        }
    }
}
