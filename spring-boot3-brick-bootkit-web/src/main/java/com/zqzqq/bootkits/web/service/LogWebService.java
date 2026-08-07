package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.exception.PluginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 插件日志查看 Web 服务。
 * <p>
 * 从应用日志文件尾部读取最近日志，支持按插件 ID / 关键字过滤。
 * 日志文件位置按以下顺序探测：
 * <ol>
 *   <li>系统属性 {@code logging.file.name} / {@code logging.file.path}</li>
 *   <li>工作目录下 {@code logs/spring.log}、{@code log/}、{@code *.log}</li>
 * </ol>
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class LogWebService {

    private static final int MAX_LINES = 2000;

    /**
     * 获取当前可用的日志文件路径（不存在则返回 null）
     */
    public String getLogFile() {
        return resolveLogFile() == null ? null : resolveLogFile().toString();
    }

    /**
     * 读取最近日志行，可按关键字过滤
     *
     * @param keyword 关键字（插件 ID 等），为空则返回全部
     * @param lines   需要读取的行数（从文件尾部）
     */
    public List<String> readLogs(String keyword, int lines) {
        Path logFile = resolveLogFile();
        if (logFile == null) {
            throw new PluginException("未找到日志文件，请配置 logging.file.name 或在工作目录放置日志文件");
        }

        int targetLines = Math.min(Math.max(lines, 1), MAX_LINES);
        List<String> tailLines = readTail(logFile, targetLines);
        if (!StringUtils.hasText(keyword)) {
            return tailLines;
        }
        return tailLines.stream()
                .filter(line -> line.contains(keyword))
                .collect(Collectors.toList());
    }

    /**
     * 探测日志文件位置
     */
    private Path resolveLogFile() {
        // 1. 系统属性
        String fileName = System.getProperty("logging.file.name");
        if (StringUtils.hasText(fileName) && Files.exists(Paths.get(fileName))) {
            return Paths.get(fileName);
        }
        String filePath = System.getProperty("logging.file.path");
        if (StringUtils.hasText(filePath)) {
            Path candidate = Paths.get(filePath).resolve("spring.log");
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        // 2. 工作目录常见位置
        String userDir = System.getProperty("user.dir");
        String[] candidates = {
                "logs/spring.log",
                "log/spring.log",
                "logs/brick-bootkit.log",
                "logs/app.log"
        };
        for (String candidate : candidates) {
            Path path = Paths.get(userDir, candidate);
            if (Files.exists(path)) {
                return path;
            }
        }

        // 3. 工作目录下的任意 .log 文件
        try (Stream<Path> stream = Files.list(Paths.get(userDir))) {
            List<Path> logFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".log"))
                    .collect(Collectors.toList());
            if (!logFiles.isEmpty()) {
                return logFiles.get(0);
            }
        } catch (IOException ignored) {
            // ignore
        }

        // 4. logs/ 子目录（logback 常见 LOG_HOME 位置）下的任意 .log 文件
        Path logsDir = Paths.get(userDir, "logs");
        if (Files.isDirectory(logsDir)) {
            try (Stream<Path> stream = Files.list(logsDir)) {
                List<Path> logFiles = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".log"))
                        .collect(Collectors.toList());
                if (!logFiles.isEmpty()) {
                    return logFiles.get(0);
                }
            } catch (IOException ignored) {
                // ignore
            }
        }
        return null;
    }

    /**
     * 从文件尾部读取 N 行
     */
    private List<String> readTail(Path logFile, int lines) {
        List<String> result = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength <= 0) {
                return result;
            }

            // 从尾部向前读块，直到收集到足够行数或到达文件头
            int bufferSize = 8192;
            long pos = fileLength;
            StringBuilder pending = new StringBuilder();
            int foundLines = 0;

            while (pos > 0 && foundLines < lines) {
                long readStart = Math.max(0, pos - bufferSize);
                int readLen = (int) (pos - readStart);
                byte[] buffer = new byte[readLen];
                raf.seek(readStart);
                raf.readFully(buffer);
                pos = readStart;

                String chunk = new String(buffer, StandardCharsets.UTF_8);
                pending.insert(0, chunk);

                // 统计 chunk 中的换行数
                int newlineCount = countOccurrences(pending.toString(), '\n');
                if (newlineCount >= lines) {
                    break;
                }
            }

            String content = pending.toString();
            String[] allLines = content.split("\n", -1);
            int start = Math.max(0, allLines.length - lines);
            for (int i = start; i < allLines.length; i++) {
                String line = allLines[i].replaceAll("\\r$", "");
                if (!line.isEmpty()) {
                    result.add(line);
                }
            }
            return result;
        } catch (IOException e) {
            log.warn("读取日志文件失败: {}", logFile, e);
            throw new PluginException("读取日志文件失败: " + e.getMessage());
        }
    }

    private int countOccurrences(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }
}
