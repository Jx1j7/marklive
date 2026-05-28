package com.marklive.controller;

import com.marklive.service.GitDeployService;
import com.marklive.service.MarkdownScannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 文章操作 API 控制器
 * - 保存到本地
 * - 发布到 GitHub
 */
@RestController
@RequestMapping("/api/articles")
public class ArticlesController {

    private final MarkdownScannerService scannerService;
    private final GitDeployService gitDeployService;

    public ArticlesController(MarkdownScannerService scannerService, GitDeployService gitDeployService) {
        this.scannerService = scannerService;
        this.gitDeployService = gitDeployService;
    }

    /**
     * 接口一：仅保存到本地（不执行 Git）
     * POST /api/articles/save
     * Body: { "filePath": "...", "title": "...", "tags": "...", "date": "...", "content": "..." }
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveLocal(@RequestBody Map<String, String> body) {
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
            response.put("message", "本地保存成功");
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

    /**
     * 接口二：仅发布到 GitHub（执行 git add / commit / push）
     * POST /api/articles/deploy
     * Body: { "folderPath": "/path/to/repo" }
     */
    @PostMapping("/deploy")
    public ResponseEntity<Map<String, Object>> deployToGithub(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();

        String folderPath = body.get("folderPath");

        if (folderPath == null || folderPath.isBlank()) {
            response.put("success", false);
            response.put("error", "folderPath 不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            GitDeployService.DeployResult result = gitDeployService.deploy(folderPath);
            response.put("success", result.isSuccess());
            response.put("steps", result.getSteps());
            if (result.isSuccess()) {
                response.put("message", "云端发布成功！");
            } else {
                response.put("error", result.getErrorMessage());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "发布失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
