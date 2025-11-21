package com.easypan.common.util;

import com.easypan.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ProcessUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

    // 默认超时时间（分钟）
    private static final long DEFAULT_TIMEOUT_MINUTES = 30L;

    private ProcessUtils() {}

    // ================== 对外方法 ==================

    /** 执行命令（参数列表方式，推荐） */
    public static int exec(List<String> cmd, File workDir, boolean printOutput) {
        return exec(cmd, workDir, DEFAULT_TIMEOUT_MINUTES, printOutput);
    }

    /** 执行命令（参数列表方式，带超时） */
    public static int exec(List<String> cmd, File workDir, long timeoutMinutes, boolean printOutput) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workDir != null) {
            pb.directory(workDir);
        }
        return doExec(pb, timeoutMinutes, printOutput, cmd.toString());
    }

    /** 执行命令（字符串方式，通过 shell，适用于包含管道/重定向的复杂命令） */
    public static int exec(String cmd, File workDir, boolean printOutput) {
        return exec(cmd, workDir, DEFAULT_TIMEOUT_MINUTES, printOutput);
    }

    public static int exec(String cmd, File workDir, long timeoutMinutes, boolean printOutput) {
        ProcessBuilder pb = buildShellProcess(cmd);
        if (workDir != null) {
            pb.directory(workDir);
        }
        return doExec(pb, timeoutMinutes, printOutput, cmd);
    }

    // ================== 内部公共实现 ==================

    /** 根据当前 OS 选择合适的 shell 命令封装 String cmd */
    private static ProcessBuilder buildShellProcess(String cmd) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: 使用 cmd.exe
            return new ProcessBuilder("cmd.exe", "/c", cmd);
        } else {
            // Linux / macOS / Unix
            return new ProcessBuilder("/bin/sh", "-c", cmd);
        }
    }

    /** 真正执行进程 + 超时控制 + 吃输出 */
    private static int doExec(ProcessBuilder pb,
                              long timeoutMinutes,
                              boolean printOutput,
                              String cmdForLog) {

        try {
            Process process = pb.start();

            StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream());
            StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream());

            stdoutGobbler.start();
            stderrGobbler.start();

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                logger.error("命令执行超时: {}", cmdForLog);
                throw new BusinessException("执行命令超时");
            }

            stdoutGobbler.join();
            stderrGobbler.join();

            int exitCode = process.exitValue();
            String stdout = stdoutGobbler.getContent();
            String stderr = stderrGobbler.getContent();

            if (printOutput) {
                logger.info("命令执行完毕: {}\n退出码: {}\nstdout:\n{}\nstderr:\n{}",
                        cmdForLog, exitCode, stdout, stderr);
            } else {
                logger.info("命令执行完毕: {}, 退出码: {}", cmdForLog, exitCode);
            }

            // 你可以根据业务情况把 stdout/derr 也一并 return/封装，这里简单只返回 exitCode
            return exitCode;
        } catch (IOException e) {
            logger.error("执行命令 IO 异常, cmd={}", cmdForLog, e);
            throw new BusinessException("执行命令 IO 异常");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("执行命令被中断, cmd={}", cmdForLog, e);
            throw new BusinessException("执行命令被中断");
        }
    }

    /** 读取子进程输出流的线程 */
    private static class StreamGobbler extends Thread {
        private final InputStream inputStream;
        private final StringBuilder content = new StringBuilder();

        StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
            setDaemon(true);
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
            } catch (IOException e) {
                logger.warn("读取子进程输出时出错: {}", e.getMessage());
            }
        }

        String getContent() {
            return content.toString();
        }
    }
}

