package com.marklive.controller;

import com.marklive.service.GitDeployService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Git 部署 API 控制器
 */
@RestController
@RequestMapping("/api")
public class DeployController {

    private final GitDeployService gitDeployService;

    public DeployController(GitDeployService gitDeployService) {
        this.gitDeployService = gitDeployService;
    }

    /**
     * Git 部署：在本地博客目录下执行 git add . → git commit -m 'update' → git push
     * POST /api/deploy
     * Body: { "repoPath": "/path/to/blog/repo" }
     */
    @PostMapping("/deploy")
    public ResponseEntity<Map<String, Object>> deploy(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();

        String repoPath = body.get("repoPath");
        if (repoPath == null || repoPath.isBlank()) {
            response.put("success", false);
            response.put("error", "repoPath 不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        GitDeployService.DeployResult result = gitDeployService.deploy(repoPath);
        response.put("success", result.isSuccess());
        response.put("steps", result.getSteps());
        if (!result.isSuccess()) {
            response.put("error", result.getErrorMessage());
        } else {
            response.put("message", "Git 部署成功");
        }
        return ResponseEntity.ok(response);
    }
}
