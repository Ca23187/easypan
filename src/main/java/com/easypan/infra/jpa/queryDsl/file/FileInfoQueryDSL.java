package com.easypan.infra.jpa.queryDsl.file;

import com.easypan.infra.jpa.entity.QFileInfo;
import com.easypan.web.dto.query.FileInfoQuery;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringExpression;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class FileInfoQueryDSL {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static BooleanBuilder build(FileInfoQuery query) {
        QFileInfo q = QFileInfo.fileInfo;
        BooleanBuilder b = new BooleanBuilder();

        // -------- 精确匹配 --------
        andEqText(b, q.fileId, query.getFileId());
        andEqText(b, q.userId, query.getUserId());
        andEqText(b, q.fileMd5, query.getFileMd5());
        andEqText(b, q.filePid, query.getFilePid());
        andEq(b, q.folderType, query.getFolderType());
        andEq(b, q.fileCategory, query.getFileCategory());
        andEq(b, q.fileType, query.getFileType());
        andEq(b, q.status, query.getStatus());
        andEq(b, q.delFlag, query.getDelFlag());

        // -------- 模糊匹配（视需求选择 contains/startsWith / ignoreCase） --------
        andContains(b, q.fileId, query.getFileIdFuzzy(), false);
        andContains(b, q.userId, query.getUserIdFuzzy(), false);
        andContains(b, q.fileMd5, query.getFileMd5Fuzzy(), false);
        andContains(b, q.filePid, query.getFilePidFuzzy(), false);
        andContains(b, q.fileName, query.getFileNameFuzzy(), true);  // 文件名一般不区分大小写
        andContains(b, q.fileCover, query.getFileCoverFuzzy(), false);
        andContains(b, q.filePath, query.getFilePathFuzzy(), false);

        // -------- 范围查询（闭区间处理） --------
        betweenDateTime(b, q.createTime, query.getCreateTimeStart(), query.getCreateTimeEnd());
        betweenDateTime(b, q.lastUpdateTime, query.getLastUpdateTimeStart(), query.getLastUpdateTimeEnd());
        betweenDateTime(b, q.recoveryTime, query.getRecoveryTimeStart(), query.getRecoveryTimeEnd());

        if (Boolean.TRUE.equals(query.getQueryExpire())) {
            // 10 天前之前的回收时间
            b.and(q.recoveryTime.before(LocalDateTime.now().minusDays(10)));
        }

        // -------- 数组查询 --------
        inIfNotEmpty(b, q.fileId, query.getFileIdArray());
        inIfNotEmpty(b, q.filePid, query.getFilePidArray());
        notInIfNotEmpty(b, q.fileId, query.getExcludeFileIdArray());

        return b;
    }

    /* ===================== 小工具 ===================== */

    // 非字符串通用：仅判 null
    private static <T> void andEq(BooleanBuilder b, SimpleExpression<T> path, T value) {
        if (value != null) b.and(path.eq(value));
    }

    // 字符串专用：判 hasText
    private static void andEqText(BooleanBuilder b, StringExpression path, String value) {
        if (StringUtils.hasText(value)) b.and(path.eq(value.trim()));
    }

    private static void andContains(BooleanBuilder b, StringExpression path, String raw, boolean ignoreCase) {
        if (!StringUtils.hasText(raw)) return;
        String v = escapeLike(raw.trim());
        if (ignoreCase) b.and(path.containsIgnoreCase(v));
        else b.and(path.contains(v));
    }

    /** 闭区间 [start, end]；若仅有一端，则单端过滤 */
    private static void betweenDateTime(BooleanBuilder b, DateTimePath<LocalDateTime> path,
                                        String startStr, String endStr) {
        LocalDateTime start = parseLdt(startStr);
        LocalDateTime end = parseLdt(endStr);
        if (start != null && end != null) {
            b.and(path.between(start, end));  // [start, end] 闭区间
        } else if (start != null) {
            b.and(path.goe(start));
        } else if (end != null) {
            b.and(path.loe(end.with(LocalTime.MAX)));
        }
    }

    private static LocalDateTime parseLdt(String s) {
        if (!StringUtils.hasText(s)) return null;
        return LocalDateTime.parse(s.trim(), DT_FMT);
    }

    private static String escapeLike(String s) {
        // 简单转义 % 和 _；具体实现可按方言补全
        return s.replace("%", "\\%").replace("_", "\\_");
    }

    private static <T> void inIfNotEmpty(BooleanBuilder b, SimpleExpression<T> path, T[] arr) {
        if (arr != null && arr.length > 0) b.and(path.in(Arrays.asList(arr)));
    }

    private static <T> void notInIfNotEmpty(BooleanBuilder b, SimpleExpression<T> path, T[] arr) {
        if (arr != null && arr.length > 0) b.and(path.notIn(Arrays.asList(arr)));
    }
}