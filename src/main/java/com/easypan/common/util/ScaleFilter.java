package com.easypan.common.util;

import com.easypan.common.exception.BusinessException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;


public final class ScaleFilter {

    private static final Logger logger = LoggerFactory.getLogger(ScaleFilter.class);

    private ScaleFilter() {}

    /**
     * 按宽度生成缩略图：
     * - 如果原图宽度 <= thumbnailWidth：不压缩，返回 false
     * - 否则调用 ffmpeg 压缩，返回 true
     */
    public static Boolean createThumbnailWidthFFmpeg(File file,
                                                     int thumbnailWidth,
                                                     File targetFile,
                                                     Boolean delSource) {
        try {
            BufferedImage src = ImageIO.read(file);
            if (src == null) {
                logger.error("读取图片失败，文件可能不是有效图片: {}", file);
                return false;
            }

            int sourceW = src.getWidth();
            // 小于指定宽度不压缩
            if (sourceW <= thumbnailWidth) {
                return false;
            }

            compressImage(file, thumbnailWidth, targetFile, delSource);
            return true;
        } catch (IOException e) {
            logger.error("读取图片失败: {}", file, e);
        } catch (BusinessException e) {
            logger.error("生成缩略图失败: {}", file, e);
        } catch (Exception e) {
            logger.error("生成缩略图出现未知异常: {}", file, e);
        }
        return false;
    }

    /**
     * 按宽度百分比压缩图片（例如 0.7 = 宽度压缩到 70%）
     * 压缩完成后删除源文件
     */
    public static void compressImageWidthPercentage(File sourceFile,
                                                    BigDecimal widthPercentage,
                                                    File targetFile) {
        try {
            if (widthPercentage == null) {
                throw new IllegalArgumentException("widthPercentage 不能为空");
            }
            BufferedImage src = ImageIO.read(sourceFile);
            if (src == null) {
                throw new BusinessException("读取图片失败，文件可能不是有效图片");
            }

            int sourceWidth = src.getWidth();
            int targetWidth = widthPercentage
                    .multiply(new BigDecimal(sourceWidth))
                    .intValue();

            if (targetWidth <= 0) {
                throw new BusinessException("计算后的目标宽度非法: " + targetWidth);
            }

            // 按新宽度压缩，并删除源文件
            compressImage(sourceFile, targetWidth, targetFile, true);
        } catch (BusinessException e) {
            logger.error("按比例压缩图片失败, source={}", sourceFile, e);
            throw e;
        } catch (Exception e) {
            logger.error("按比例压缩图片失败, source={}", sourceFile, e);
            throw new BusinessException("压缩图片失败");
        }
    }

    /**
     * 为视频生成封面（截取首帧）
     */
    public static void createCover4Video(File sourceFile,
                                         Integer width,
                                         File targetFile) {
        // 使用：ffmpeg -y -i input -vframes 1 -vf scale=WIDTH:-1 output
        // 让高度按比例缩放
        List<String> cmd = Arrays.asList(
                "ffmpeg",
                "-y",
                "-i", sourceFile.getAbsolutePath(),
                "-vframes", "1",
                "-vf", "scale=" + width + ":-1",
                targetFile.getAbsolutePath()
        );

        int exitCode = ProcessUtils.exec(cmd, sourceFile.getParentFile(), false);
        if (exitCode != 0) {
            logger.error("生成视频封面失败, exitCode={}, source={}, target={}",
                    exitCode, sourceFile, targetFile);
            throw new BusinessException("生成视频封面失败");
        }
    }

    /**
     * 按给定宽度压缩图片，保持宽高比，高度自适应
     *
     * @param delSource 是否在压缩成功后删除源文件
     */
    public static void compressImage(File sourceFile,
                                     Integer width,
                                     File targetFile,
                                     Boolean delSource) {
        // ffmpeg -y -i input -vf scale=WIDTH:-1 output
        List<String> cmd = Arrays.asList(
                "ffmpeg",
                "-y",
                "-i", sourceFile.getAbsolutePath(),
                "-vf", "scale=" + width + ":-1",
                targetFile.getAbsolutePath()
        );

        int exitCode = ProcessUtils.exec(cmd, sourceFile.getParentFile(), false);
        if (exitCode != 0) {
            logger.error("压缩图片失败, exitCode={}, source={}, target={}",
                    exitCode, sourceFile, targetFile);
            throw new BusinessException("压缩图片失败");
        }

        if (Boolean.TRUE.equals(delSource)) {
            try {
                FileUtils.forceDelete(sourceFile);
            } catch (IOException e) {
                // 压缩成功，但删除源图失败，一般不算致命错误
                logger.warn("压缩图片后删除源文件失败: {}", sourceFile, e);
            }
        }
    }
}
