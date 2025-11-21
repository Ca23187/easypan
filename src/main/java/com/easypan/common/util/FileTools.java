package com.easypan.common.util;

import com.easypan.common.constants.Constants;
import com.easypan.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class FileTools {

    private static final Logger logger = LoggerFactory.getLogger(FileTools.class);

    public static void readFile(HttpServletResponse response, Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            send404(response);
            return;
        }

        try {
            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = "application/octet-stream";
            response.setContentType(contentType);
            response.setContentLengthLong(Files.size(path));

            try (OutputStream out = response.getOutputStream()) {
                Files.copy(path, out);
                out.flush(); // 可省略，close 会 flush
            }
        } catch (IOException e) {
            logger.error("读取文件异常: {}", path, e);
            send500(response);
        }
    }

    private static void send404(HttpServletResponse resp) {
        try { resp.sendError(HttpServletResponse.SC_NOT_FOUND); } catch (IOException ignored) {}
    }
    private static void send400(HttpServletResponse resp, String msg) {
        try { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg); } catch (IOException ignored) {}
    }
    private static void send500(HttpServletResponse resp) {
        try { resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); } catch (IOException ignored) {}
    }

    // 支持 Range（视频/HLS/大文件友好）
    public static void readFileWithRange(HttpServletRequest request, HttpServletResponse response, Path path) {
        if (!Files.exists(path)) { send404(response); return; }

        long fileSize;
        try { fileSize = Files.size(path); } catch (IOException e) { send500(response); return; }

        String range = request.getHeader("Range");
        long start = 0, end = fileSize - 1;
        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.substring(6).split("-", 2);
            try {
                if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {}
            if (parts[0].isEmpty() && !parts[1].isEmpty()) {
                long suffixLen = Long.parseLong(parts[1]);
                if (suffixLen <= 0) {
                    send400(response, "无效 Range");
                    return;
                }
                start = Math.max(0, fileSize - suffixLen);
                end = fileSize - 1;
            }
            if (start > end || start < 0 || end >= fileSize) { send400(response, "无效 Range"); return; }
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        }

        long contentLength = end - start + 1;
        String contentType;
        try { contentType = Files.probeContentType(path); } catch (IOException e) { contentType = null; }
        if (contentType == null) contentType = "application/octet-stream";

        response.setContentType(contentType);
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentLengthLong(contentLength);

        try (OutputStream os = response.getOutputStream();
             SeekableByteChannel sbc = Files.newByteChannel(path, StandardOpenOption.READ)) {
            sbc.position(start);
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            long toWrite = contentLength;
            while (toWrite > 0) {
                buffer.clear();
                int read = sbc.read(buffer);
                if (read == -1) break;
                buffer.flip();
                int chunk = (int)Math.min(read, toWrite);
                os.write(buffer.array(), 0, chunk);
                toWrite -= chunk;
            }
        } catch (IOException e) {
            logger.warn("客户端可能中断连接: {}", e.getMessage());
        }
    }

    public static void union(Path dir, Path target, String fileName, boolean delSource) {
        if (!Files.isDirectory(dir)) {
            throw new BusinessException("目录不存在");
        }

        // 发现所有“数字命名”的分片
        List<Path> chunks;
        try {
            try (Stream<Path> s = Files.list(dir)) {
                chunks = s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().matches("\\d+"))
                        .sorted(Comparator.comparingInt(p -> Integer.parseInt(p.getFileName().toString())))
                        .toList();
            }
        } catch (IOException e) {
            logger.error("读取分片列表失败", e);
            throw new BusinessException("读取分片列表失败");
        }

        if (chunks.isEmpty()) {
            throw new BusinessException("未找到任何分片");
        }

        // 校验连续性：0..n-1 是否齐全
        int expected = chunks.size();
        for (int i = 0; i < expected; i++) {
            Path p = dir.resolve(String.valueOf(i));
            if (!Files.exists(p)) {
                throw new BusinessException("分片缺失: " + i);
            }
        }

        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");

        // 确保父目录存在
        try { Files.createDirectories(target.getParent()); } catch (IOException ignored) {}

        // 合并到临时文件
        try (FileChannel out = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            long pos = 0;
            for (Path c : chunks) {
                try (FileChannel in = FileChannel.open(c, StandardOpenOption.READ)) {
                    long size = in.size();
                    long transferred = 0;
                    while (transferred < size) {
                        long n = out.transferFrom(in, pos + transferred, size - transferred);
                        if (n <= 0) break;
                        transferred += n;
                    }
                    pos += transferred;
                } catch (IOException e) {
                    logger.error("合并分片失败: {}", c.getFileName(), e);
                    throw new BusinessException("合并文件失败");
                }
            }
        } catch (IOException e) {
            logger.error("写入临时文件失败: {}", tmp, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        }

        // 原子替换到目标
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // 跨分区或文件系统不支持，退化为非原子替换
            try { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException ex) {
                logger.error("移动目标文件失败", ex);
                throw new BusinessException("生成目标文件失败");
            }
        } catch (IOException e) {
            logger.error("移动目标文件失败", e);
            throw new BusinessException("生成目标文件失败");
        }

        // 只在成功后删除源目录
        if (delSource) {
            try {
                FileUtils.deleteDirectory(dir.toFile());
            } catch (IOException e) {
                logger.warn("删除源分片目录失败: {}", dir, e);
            }
        }
    }


    public static void cutFile4Video(String fileId, Path input) {
        if (!Files.exists(input)) {
            throw new IllegalArgumentException("输入文件不存在: " + input);
        }

        // 切片目录：与源视频同名（去扩展名）
        File tsFolder = new File(input.toString().substring(0, input.toString().lastIndexOf(".")));
        if (!tsFolder.exists() && !tsFolder.mkdirs()) {
            throw new RuntimeException("无法创建输出目录: " + tsFolder.getAbsolutePath());
        }

        String m3u8Path = new File(tsFolder, Constants.M3U8_NAME).getAbsolutePath();
        String segTmpl  = new File(tsFolder, fileId + "_%04d.ts").getAbsolutePath();

        // 方案A：尽量不转码（仅当输入是 H.264 + AAC 才能成功）
        List<String> cmdCopy = Arrays.asList(
                "ffmpeg", "-y",
                "-i", input.toAbsolutePath().toString(),
                "-map", "0:v:0",
                "-map", "0:a:0?",              // 音频可能不存在，用 ? 容忍
                "-c:v", "copy",
                "-c:a", "copy",
                "-bsf:v", "h264_mp4toannexb",  // 代替已废弃的 -vbsf
                "-f", "hls",
                "-hls_time", "30",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segTmpl,
                m3u8Path
        );

        // 方案B：回退到重编码（任何输入都能工作，但耗时更长）
        List<String> cmd = Arrays.asList(
                "ffmpeg", "-y",
                "-i", input.toAbsolutePath().toString(),
                "-map", "0:v:0",
                "-map", "0:a:0?",
                "-c:v", "h264",
                "-preset", "veryfast",
                "-profile:v", "main",
                "-level", "4.1",
                "-c:a", "aac",
                "-b:a", "128k",
                "-ac", "2",
                "-f", "hls",
                "-hls_time", "30",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segTmpl,
                m3u8Path
        );

        try {
            int code = ProcessUtils.exec(cmdCopy, tsFolder, false);
            if (code != 0) {
                // A 失败（比如源不是 H.264/AAC），尝试 B
                code = ProcessUtils.exec(cmd, tsFolder, false);
            }
            if (code != 0) {
                throw new RuntimeException("ffmpeg 执行失败，退出码: " + code);
            }
        } catch (BusinessException e) {
            // 不管是 A 还是 B 抛异常，都统一走这里
            throw new RuntimeException("ffmpeg 执行异常", e);
        }
    }
}
