package com.marklive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Git 部署服务：在指定目录下执行 git add / commit / push
 * 每行 Git 日志都会打印到控制台，方便调试 SSH 权限、分支等问题。
 */
@Service
public class GitDeployService {

    private static final Logger log = LoggerFactory.getLogger(GitDeployService.class);

    /** 命令执行超时时间（秒） */
    private static final long CMD_TIMEOUT_SECONDS = 60;

    /**
     * 部署结果封装
     */
    public static class DeployResult {
        private final boolean success;
        private final List<Map<String, String>> steps;
        private final String errorMessage;

        public DeployResult(boolean success, List<Map<String, String>> steps, String errorMessage) {
            this.success = success;
            this.steps = steps;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public List<Map<String, String>> getSteps() {
            return steps;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 在指定博客目录下依次执行 git add . → git commit → git push origin main
     *
     * @param repoPath 本地 Git 仓库路径
     * @return DeployResult 包含每步执行结果
     */
    public DeployResult deploy(String repoPath) {
        Path path = Paths.get(repoPath).toAbsolutePath().normalize();

        // 校验目录
        if (!Files.exists(path)) {
            return new DeployResult(false, List.of(), "目录不存在: " + repoPath);
        }
        if (!Files.isDirectory(path)) {
            return new DeployResult(false, List.of(), "路径不是目录: " + repoPath);
        }

        Path gitDir = path.resolve(".git");
        if (!Files.exists(gitDir) || !Files.isDirectory(gitDir)) {
            return new DeployResult(false, List.of(), "该目录不是 Git 仓库（缺少 .git）: " + repoPath);
        }

        List<Map<String, String>> steps = new ArrayList<>();

        // 步骤 1: git add .
        System.out.println("[Git Process] ========== 步骤 1: git add . ==========");
        Map<String, String> addResult = execCommand(repoPath, "git", "add", ".");
        steps.add(Map.of("step", "git add .", "output", addResult.get("output"), "success", addResult.get("success")));
        if ("false".equals(addResult.get("success"))) {
            return new DeployResult(false, steps, "git add . 执行失败: " + addResult.get("output"));
        }

        // 步骤 2: git commit -m 'Publish via MarkLive'
        System.out.println("[Git Process] ========== 步骤 2: git commit -m 'Publish via MarkLive' ==========");
        Map<String, String> commitResult = execCommand(repoPath, "git", "commit", "-m", "Publish via MarkLive");
        steps.add(Map.of("step", "git commit -m 'Publish via MarkLive'", "output", commitResult.get("output"), "success", commitResult.get("success")));

        String commitOutput = commitResult.get("output");
        boolean nothingToCommit = commitOutput != null && commitOutput.contains("nothing to commit");

        if ("false".equals(commitResult.get("success")) && !nothingToCommit) {
            return new DeployResult(false, steps, "git commit 执行失败: " + commitOutput);
        }

        // 步骤 3: git push origin main
        System.out.println("[Git Process] ========== 步骤 3: git push origin main ==========");
        Map<String, String> pushResult = execCommand(repoPath, "git", "push", "origin", "main");
        steps.add(Map.of("step", "git push origin main", "output", pushResult.get("output"), "success", pushResult.get("success")));
        if ("false".equals(pushResult.get("success"))) {
            return new DeployResult(false, steps, "git push origin main 执行失败: " + pushResult.get("output"));
        }

        log.info("Git 部署成功: {}", repoPath);
        System.out.println("[Git Process] ========== Git 部署全部完成 ==========");
        return new DeployResult(true, steps, null);
    }

    /**
     * 执行系统命令，将 stdout 和 stderr 逐行打印到控制台，并返回完整输出。
     *
     * @param workingDir 工作目录
     * @param command    命令及其参数
     * @return Map 包含 "success" ("true"/"false") 和 "output"
     */
    private Map<String, String> execCommand(String workingDir, String... command) {
        Map<String, String> result = new LinkedHashMap<>();
        StringBuilder output = new StringBuilder();

        String cmdStr = String.join(" ", command);
        System.out.println("[Git Process] 执行命令: cd " + workingDir + " && " + cmdStr);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new java.io.File(workingDir));

            Process process = pb.start();

            // 分别读取 stdout 和 stderr，逐行打印到控制台
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Git Process] [stdout] " + line);
                        synchronized (output) {
                            output.append(line).append("\n");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("[Git Process] [stdout] 读取异常: " + e.getMessage());
                }
            }, "git-stdout-reader");

            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Git Process] [stderr] " + line);
                        synchronized (output) {
                            output.append(line).append("\n");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("[Git Process] [stderr] 读取异常: " + e.getMessage());
                }
            }, "git-stderr-reader");

            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(CMD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.out.println("[Git Process] 命令执行超时（" + CMD_TIMEOUT_SECONDS + "秒），已强制终止");
                result.put("success", "false");
                result.put("output", "命令执行超时（" + CMD_TIMEOUT_SECONDS + "秒）");
                return result;
            }

            // 等待读取线程结束
            stdoutThread.join(5000);
            stderrThread.join(5000);

            int exitCode = process.exitValue();
            System.out.println("[Git Process] 命令退出码: " + exitCode);

            String out = output.toString().trim();
            result.put("success", exitCode == 0 ? "true" : "false");
            result.put("output", out.isEmpty() ? "(无输出)" : out);

        } catch (IOException e) {
            System.out.println("[Git Process] IO异常: " + e.getMessage());
            log.error("执行命令失败: {} - {}", cmdStr, e.getMessage());
            result.put("success", "false");
            result.put("output", "IO异常: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[Git Process] 命令被中断");
            result.put("success", "false");
            result.put("output", "命令被中断");
        }

        return result;
    }
}
