package com.marklive.service;

import com.marklive.model.MarkdownArticle;
import com.marklive.util.FrontMatterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Markdown 文件扫描与保存服务
 */
@Service
public class MarkdownScannerService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownScannerService.class);

    /**
     * 扫描指定文件夹下所有 .md 文件，解析并返回文章列表
     *
     * @param folderPath 文件夹路径
     * @return MarkdownArticle 列表
     */
    public List<MarkdownArticle> scanFolder(String folderPath) {
        Path rootPath = Paths.get(folderPath).toAbsolutePath().normalize();

        if (!Files.exists(rootPath)) {
            throw new IllegalArgumentException("文件夹不存在: " + folderPath);
        }

        if (!Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("路径不是文件夹: " + folderPath);
        }

        List<MarkdownArticle> articles = new ArrayList<>();

        try {
            // 使用 Files.walk 递归遍历所有文件
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
                        try {
                            MarkdownArticle article = processMarkdownFile(file, rootPath);
                            if (article != null) {
                                articles.add(article);
                            }
                        } catch (IOException e) {
                            log.warn("无法读取文件: {} - {}", file, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("无法访问文件: {} - {}", file, exc != null ? exc.getMessage() : "未知错误");
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            log.error("遍历文件夹时出错: {}", e.getMessage(), e);
            throw new RuntimeException("扫描文件夹失败: " + e.getMessage(), e);
        }

        log.info("扫描完成，共找到 {} 个 Markdown 文件", articles.size());
        return articles;
    }

    /**
     * 处理单个 Markdown 文件
     */
    private MarkdownArticle processMarkdownFile(Path file, Path rootPath) throws IOException {
        String rawContent = Files.readString(file, StandardCharsets.UTF_8);

        // 解析 Front-Matter
        FrontMatterParser.ParseResult parseResult = FrontMatterParser.parse(rawContent);

        // 组装 MarkdownArticle
        MarkdownArticle article = new MarkdownArticle();
        article.setFilePath(file.toAbsolutePath().normalize().toString());
        article.setFileName(file.getFileName().toString());
        article.setTitle(parseResult.getTitle());
        article.setTags(parseResult.getTags());
        article.setDate(parseResult.getDate());
        article.setContent(parseResult.getContent());

        return article;
    }

    /**
     * 保存 Markdown 文章：将 title、tags、date、content 拼装为 YAML Front-Matter + 正文，
     * 写回到指定的 .md 文件中。仅执行文件 I/O，不执行 Git 命令。
     *
     * @param filePath 目标 .md 文件绝对路径
     * @param title    文章标题
     * @param tags     标签（逗号分隔）
     * @param date     日期
     * @param content  正文
     */
    public void saveArticle(String filePath, String title, String tags, String date, String content) {
        Path targetPath = Paths.get(filePath).toAbsolutePath().normalize();

        // 安全检查：确保目标文件是 .md 文件
        String fileName = targetPath.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".md") && !fileName.endsWith(".markdown")) {
            throw new IllegalArgumentException("目标文件不是 Markdown 文件: " + filePath);
        }

        if (!Files.exists(targetPath)) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        try {
            // 拼装完整的 Markdown 文本
            String assembled = FrontMatterParser.assemble(title, tags, date, content);

            // 写回文件
            Files.writeString(targetPath, assembled, StandardCharsets.UTF_8);

            log.info("文章已保存到本地: {}", targetPath);
        } catch (IOException e) {
            log.error("保存文章失败: {} - {}", filePath, e.getMessage(), e);
            throw new RuntimeException("保存文件失败: " + e.getMessage(), e);
        }
    }
}
