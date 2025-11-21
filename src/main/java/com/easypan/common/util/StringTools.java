package com.easypan.common.util;


import org.apache.commons.codec.digest.DigestUtils;

import java.util.Random;


public final class StringTools {

    private static final String LITERAL_NULL = "null";

    /** 共享 Random，避免每次 new */
    private static final Random RANDOM = new Random();

    private StringTools() {}

    // =================== 通用判空 ===================

    /**
     * 判断字符串是否“空”：
     * - null
     * - 去掉前后空白后长度为 0
     * - 字面值是 "null"（兼容一些脏数据）
     */
    public static boolean isEmpty(String str) {
        if (str == null) {
            return true;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() || LITERAL_NULL.equalsIgnoreCase(trimmed);
    }

    /**
     * 更语义化的别名（如果你以后想慢慢替换 isEmpty）
     */
    public static boolean isBlank(String str) {
        return isEmpty(str);
    }

    // =================== MD5 ===================

    /** 对字符串做 MD5 摘要（非密码场景），入参为空返回 null */
    public static String encodeByMD5(String originString) {
        return isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }

    // =================== 文件名相关 ===================

    /** 获取文件后缀（包含点），没有则返回空串 */
    public static String getFileSuffix(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index <= 0 || index == fileName.length() - 1) {
            // 形如 ".gitignore" 或 "a." 都视为无后缀
            return "";
        }
        return fileName.substring(index);
    }

    /** 获取去掉后缀的文件名 */
    public static String getFileNameNoSuffix(String fileName) {
        if (fileName == null) {
            return null;
        }
        int index = fileName.lastIndexOf('.');
        if (index <= 0) {
            return fileName;
        }
        return fileName.substring(0, index);
    }

    /** 重命名文件：name_random.ext */
    public static String rename(String fileName) {
        String base = getFileNameNoSuffix(fileName);
        String suffix = getFileSuffix(fileName);
        return base + "_" + getRandomString(5) + suffix;
    }

    // =================== 随机串 / 数字 ===================

    private static final String RANDOM_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String getRandomString(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            int index = RANDOM.nextInt(RANDOM_CHARS.length());
            builder.append(RANDOM_CHARS.charAt(index));
        }
        return builder.toString();
    }

    public static String getRandomNumber(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    // =================== HTML / 标题转义 ===================

    /**
     * 仅用于简单标题：目前只转义 & 和 &lt; / &gt;，避免最常见的注入。
     */
    public static String escapeTitle(String content) {
        if (isEmpty(content)) {
            return content;
        }
        String s = content;
        s = s.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        return s;
    }

    /**
     * 简单 HTML 转义：
     * - & -> &amp;
     * - < -> &lt; , > -> &gt;
     * - 空格 -> &nbsp;
     * - 换行 -> <br>
     * 只是轻量处理，不等价于完整的 HTML 转义。
     */
    public static String escapeHtml(String content) {
        if (isEmpty(content)) {
            return content;
        }
        String s = content;
        // 顺序很重要，先替换 &
        s = s.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        s = s.replace(" ", "&nbsp;");
        s = s.replace("\n", "<br>");
        return s;
    }

    // =================== 路径校验 ===================

    /**
     * 简单校验路径是否安全（防止 ../ 目录穿越）
     * 这里只是最基础的检查，真正的安全校验建议结合规范化路径 + 白名单根目录判断。
     */
    public static boolean pathIsOk(String path) {
        if (isEmpty(path)) {
            return false;
        }

        // 禁止路径穿越
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }

        // 禁止多级路径
        return !path.contains("/") && !path.contains("\\");
    }


    // =================== 下划线转驼峰 ===================

    public static String underlineToCamel(String param) {
        if (param == null || param.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(param.length());
        boolean nextUpper = false;
        for (int i = 0; i < param.length(); i++) {
            char c = param.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}