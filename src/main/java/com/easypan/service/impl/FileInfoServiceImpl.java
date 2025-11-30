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
import com.easypan.infra.jpa.repository.FileInfoRepository;
import com.easypan.infra.jpa.repository.UserInfoRepository;
import com.easypan.infra.mapStruct.FileInfoMapper;
import com.easypan.infra.redis.RedisComponent;
import com.easypan.infra.redis.RedisUtils;
import com.easypan.infra.secure.LoginUser;
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
import java.util.*;
import java.util.stream.Collectors;


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

    @Resource
    private FileInfoMapper fileInfoMapper;

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
                // 查询完全相同的文件，查到则能秒传
                FileInfo dbFile = fileInfoRepository.findFirstByFileMd5AndStatus(fileMd5, FileStatusEnum.USING.getStatus());
                if (dbFile != null) {
                    // 秒传
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
    public FileInfo findFirstByFileId(String fileId) {
        return fileInfoRepository.findFirstByFileId(fileId);
    }

    @Override
    public boolean existsByFilePathAndUserId(String filePath, String userId) {
        return fileInfoRepository.existsByFilePathAndUserId(filePath, userId);
    }

    @Override
    public FileInfo findById(FileInfoId fileInfoId) {
        return fileInfoRepository.findById(fileInfoId).orElse(null);
    }

    private String autoRename(String filePid, String userId, String fileName) {
        // 查询是否有重名文件
        boolean exists = fileInfoRepository.existsByUserId_AndFilePid_AndDelFlag_AndFileName(
                userId,
                filePid,
                FileDeleteFlagEnum.USING.getFlag(),
                fileName
        );
        if (exists) {
            return StringTools.rename(fileName);
        }
        return fileName;
    }

    private void updateUserSpaceInfo(String userId, Long fileSize) {
        int count = userInfoRepository.updateUserSpaceInfoByUserId(userId, fileSize, null);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.STORAGE_INSUFFICIENT);
        }
        // 更新完直接删缓存，防并发
        redisUtils.delete(Constants.REDIS_KEY_USER_SPACE_INFO + userId);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVo createFolder(String filePid, String userId, String folderName) {
        checkFileNameOrFolderName(filePid, userId, folderName, FileFolderTypeEnum.FOLDER.getType());
        LocalDateTime now = LocalDateTime.now();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constants.RANDOM_FILE_ID_LENGTH));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnum.FOLDER.getType());
        fileInfo.setCreateTime(now);
        fileInfo.setLastUpdateTime(now);
        fileInfo.setStatus(FileStatusEnum.USING.getStatus());
        fileInfo.setDelFlag(FileDeleteFlagEnum.USING.getFlag());
        fileInfoRepository.save(fileInfo);
        return fileInfoMapper.toVo(fileInfo);
    }

    // 检查文件或文件夹能否在当前目录下重命名，如果当前目录下已有同名文件或文件夹则不能重命名
    private void checkFileNameOrFolderName(String filePid, String userId, String fileName, Integer folderType) {
        FileInfo fileInfo = fileInfoRepository.findFirstByUserId_AndFolderType_AndFileName_AndFilePid_AndDelFlag(
                userId, folderType, fileName, filePid, FileDeleteFlagEnum.USING.getFlag());
        if (fileInfo != null) {
            throw new BusinessException("此目录下已存在同名文件/文件夹，请修改名称");
        }
    }

    @Override
    public List<FileInfoVo> getFolderInfoVoList(String path, String userId) {
        String[] pathArray = path.split("/");
        List<String> ids = Arrays.asList(pathArray);
        List<FileInfoVo> voList = fileInfoRepository.findFolderInfoVoList(userId, FileFolderTypeEnum.FOLDER.getType(), ids);
        // 根据 path 顺序做一个顺序表
        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < pathArray.length; i++) {
            orderMap.put(pathArray[i], i);
        }
        // 在内存中按顺序排序
        voList.sort(Comparator.comparingInt(
                vo -> orderMap.getOrDefault(vo.getFileId(), Integer.MAX_VALUE)
        ));
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVo rename(String fileId, String userId, String newFileName) {
        FileInfoVo fileInfoVo = fileInfoRepository.findVoByFileIdAndUserIdAndDelFlag(fileId, userId, FileDeleteFlagEnum.USING.getFlag());
        if (null == fileInfoVo) {
            throw new BusinessException("文件不存在");
        }
        if (fileInfoVo.getFileName().equals(newFileName)) {  // 新 name 与旧 name 一样，直接返回
            return fileInfoVo;
        }
        String filePid = fileInfoVo.getFilePid();
        checkFileNameOrFolderName(filePid, userId, newFileName, fileInfoVo.getFolderType());
        // 如果是文件，则获取后缀
        if (FileFolderTypeEnum.FILE.getType().equals(fileInfoVo.getFolderType())) {
            newFileName += StringTools.getFileSuffix(fileInfoVo.getFileName());
        }
        LocalDateTime now = LocalDateTime.now();
        int count = fileInfoRepository.renameWithOldName(
                newFileName, now, fileId, userId,
                fileInfoVo.getFileName(),
                FileDeleteFlagEnum.USING.getFlag()
        );
        if (count == 0) {
            throw new BusinessException("重命名失败，请稍后重试");
        }

        fileInfoVo.setFileName(newFileName);
        fileInfoVo.setLastUpdateTime(now);
        return fileInfoVo;
    }

    @Override
    public List<FileInfoVo> findMovableTargetFolders(String userId, String filePid, String currentFileIds) {
        List<String> fileIds = List.of();
        if (!StringTools.isEmpty(currentFileIds)) {
            fileIds = Arrays.asList(currentFileIds.split(","));  // 待移动的多个文件或目录
        }
        return fileInfoRepository.findMovableTargetFolders(userId, filePid, FileFolderTypeEnum.FOLDER.getType(), fileIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        List<String> fileIdList = Arrays.asList(fileIds.split(","));
        for (String fileId : fileIdList) {
            if (fileId.equals(filePid)) {  // 若待移动的多个文件或目录中存在一个目录，该目录不能移动到该目录下（即目录的Pid是其本身）
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }
        }
        if (!filePid.equals("0")) {  // 目标目录不是根目录
            // 查一下目标目录是否存在
            fileInfoRepository.findById(new FileInfoId(filePid, userId))
                    .filter(file -> FileDeleteFlagEnum.USING.getFlag().equals(file.getDelFlag()))
                    .orElseThrow(() -> new BusinessException(ResponseCodeEnum.BAD_REQUEST));
        }
        // 查一下目标目录底下有没有同名文件，如果有就重命名再移动
        List<FileInfo> dbFileList = fileInfoRepository.findByUserIdAndFilePid(userId, filePid);
        // 用 Set 记录目标目录已占用的文件名
        Set<String> occupiedNames = dbFileList.stream()
                .map(FileInfo::getFileName)
                .collect(Collectors.toSet());

        // 查询用户选中的待移动文件
        List<FileInfo> selectedFileList = fileInfoRepository.findByUserIdAndFileIdIn(userId, fileIdList);
        // 重命名 + 移动
        for (FileInfo item : selectedFileList) {
            String originalName = item.getFileName();
            String newName = originalName;
            // 如果名字被占用
            if (occupiedNames.contains(newName)) {
                newName = StringTools.rename(newName);
            }
            // 如果有冲突而重命名了，更新 fileName
            if (!newName.equals(originalName)) {
                item.setFileName(newName);
            }
            // 移动文件：修改父目录
            item.setFilePid(filePid);
            // 把新名字也加入占用集合，防止本次后续文件再次冲突
            occupiedNames.add(newName);
        }
    }
}
