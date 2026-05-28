package com.marklive.controller;

import com.marklive.model.MarkdownArticle;
import com.marklive.service.MarkdownScannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Markdown 扫描 API 控制器
 */
@RestController
@RequestMapping("/api/markdown")
public class MarkdownController {

    private final MarkdownScannerService scannerService;

    public MarkdownController(MarkdownScannerService scannerService) {
        this.scannerService = scannerService;
    }

    /**
     * 扫描指定文件夹下的所有 .md 文件
     * GET /api/markdown/scan?folderPath=/path/to/folder
     */
    @GetMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanFolder(@RequestParam("folderPath") String folderPath) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<MarkdownArticle> articles = scannerService.scanFolder(folderPath);
            response.put("success", true);
            response.put("count", articles.size());
            response.put("articles", articles);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "扫描失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 保存 Markdown 文章到本地文件（仅文件 I/O，不执行 Git）
     * POST /api/markdown/save
     * Body: { "filePath": "...", "title": "...", "tags": "...", "date": "...", "content": "..." }
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveArticle(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();

        String filePath = body.get("filePath");
        String title = body.get("title");
        String tags = body.get("tags");
        String date = body.get("date");
        String content = body.get("content");

        if (filePath == null || filePath.isBlank()) {
            response.put("success", false);
            response.put("error", "filePath 不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            scannerService.saveArticle(filePath, title, tags, date, content);
            response.put("success", true);
            response.put("message", "文章保存成功");
            response.put("filePath", filePath);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "保存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
