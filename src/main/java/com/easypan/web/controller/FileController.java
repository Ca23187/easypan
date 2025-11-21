package com.easypan.web.controller;

import com.easypan.common.annotation.RequiresLogin;
import com.easypan.common.constants.Constants;
import com.easypan.common.enums.FileCategoryEnum;
import com.easypan.common.enums.FileDeleteFlagEnum;
import com.easypan.common.exception.BusinessException;
import com.easypan.common.response.ResponseCodeEnum;
import com.easypan.common.response.ResponseVo;
import com.easypan.common.util.FileTools;
import com.easypan.common.util.StringTools;
import com.easypan.config.AppProperties;
import com.easypan.infra.jpa.entity.FileInfo;
import com.easypan.infra.jpa.entity.FileInfoId;
import com.easypan.infra.secure.LoginUser;
import com.easypan.infra.secure.LoginUserHolder;
import com.easypan.service.FileInfoService;
import com.easypan.service.dto.UploadResultDto;
import com.easypan.web.dto.query.FileInfoQuery;
import com.easypan.web.dto.response.FileInfoVo;
import com.easypan.web.dto.response.PaginationResultVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 文件信息 Controller
 */
@RestController("fileInfoController")
@RequestMapping("/file")
@RequiresLogin
public class FileController {

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private AppProperties appProperties;

    @GetMapping("/loadDataList")
    public ResponseVo<PaginationResultVo<FileInfoVo>> loadDataList(String category, FileInfoQuery query) {
        FileCategoryEnum categoryEnum = FileCategoryEnum.getByCode(category);
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setUserId(LoginUserHolder.getLoginUser().getUserId());
        query.setDelFlag(FileDeleteFlagEnum.USING.getFlag());
        return ResponseVo.ok(fileInfoService.findPageByParam(query));
    }

    @PostMapping("/uploadFile")
    public ResponseVo<UploadResultDto> uploadFile(String fileId,
                                                  MultipartFile file,
                                                  @NotBlank String fileName,
                                                  @NotBlank String filePid,
                                                  @NotBlank String fileMd5,
                                                  @NotNull Integer chunkIndex,
                                                  @NotNull Integer chunks) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        UploadResultDto resultDto = fileInfoService.uploadFile(loginUser, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return ResponseVo.ok(resultDto);
    }

    @GetMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response,
                         @PathVariable String imageFolder,
                         @PathVariable String imageName) {
        if (!StringTools.pathIsOk(imageFolder) || !StringTools.pathIsOk(imageName)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        Path imagePath = Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER_FILE,
                imageFolder,
                imageName
        );
        response.setHeader("Cache-Control", "max-age=2592000");
        FileTools.readFile(response, imagePath);
    }

    @GetMapping("/ts/getVideoInfo/{fileId}")
    public void getVideoInfo(HttpServletResponse response, @PathVariable @NotBlank String fileId) {
        readFile(response, fileId);
    }

    @GetMapping("/getFile/{fileId}")
    public void getFile(HttpServletResponse response, @PathVariable @NotBlank String fileId) {
        readFile(response, fileId);
    }

    private void readFile(HttpServletResponse response, String fileId) {
        if (!StringTools.pathIsOk(fileId)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Path filePath = resolveFilePath(fileId, loginUser.getUserId());
        if (filePath == null || !Files.exists(filePath)) {
            throw new BusinessException(ResponseCodeEnum.NOT_FOUND);
        }
        FileTools.readFile(response, filePath);
    }

    private Path resolveFilePath(String fileId, String userId) {
        // ts 切片
        if (fileId.endsWith(".ts")) {
            String realFileId = fileId.split("_")[0];

            // 1. 先尝试按(原视频id, 当前用户)查
            FileInfo fileInfo = fileInfoService.findById(new FileInfoId(realFileId, userId));

            // 2. 如果查不到，说明是分享场景：用原视频id查原文件，再校验当前用户是否拥有相同路径文件
            if (fileInfo == null) {
                List<FileInfo> fileInfoList = fileInfoService.findByFileId(realFileId);
                if (fileInfoList == null || fileInfoList.isEmpty()) {
                    // 没找到原文件
                    return null;
                }
                fileInfo = fileInfoList.get(0);

                // 根据当前用户id和路径去查询当前用户是否有该文件，如果没有直接返回
                int count = fileInfoService.countByFilePathAndUserId(fileInfo.getFilePath(), userId);
                if (count == 0) {
                    // 当前用户没有这个文件的访问权限
                    return null;
                }
            }

            String fileName = fileInfo.getFilePath();
            return Paths.get(
                    appProperties.getProjectFolder(),
                    Constants.FILE_FOLDER_FILE,
                    StringTools.getFileNameNoSuffix(fileName),
                    fileId
            );
        }

        // 非 ts（m3u8 或 普通文件）
        FileInfo fileInfo = fileInfoService.findById(new FileInfoId(fileId, userId));
        if (fileInfo == null) {
            return null;
        }

        // 视频：读 m3u8
        if (FileCategoryEnum.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
            String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
            return Paths.get(
                    appProperties.getProjectFolder(),
                    Constants.FILE_FOLDER_FILE,
                    fileNameNoSuffix,
                    Constants.M3U8_NAME
            );
        }

        // 非视频：读原文件
        return Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER_FILE,
                fileInfo.getFilePath()
        );
    }
}