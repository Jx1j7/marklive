package com.marklive.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 配置相关接口（文件夹浏览等）
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    /**
     * 弹出原生文件夹选择框，返回用户选择的目录绝对路径
     * GET /api/config/browse
     */
    @GetMapping("/browse")
    public ResponseEntity<Map<String, Object>> browse() {
        Map<String, Object> response = new HashMap<>();

        try {
            String selectedPath = openFolderChooser();
            if (selectedPath != null && !selectedPath.isBlank()) {
                response.put("success", true);
                response.put("path", selectedPath);
            } else {
                response.put("success", false);
                response.put("error", "用户取消了选择");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("文件夹选择弹窗失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "弹窗失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 使用 JFileChooser 弹出原生文件夹选择对话框
     * 由于 JFileChooser 必须在 AWT 事件线程中调用，
     * 这里使用 CompletableFuture + SwingUtilities.invokeAndWait 来桥接。
     */
    private String openFolderChooser() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();

        SwingUtilities.invokeAndWait(() -> {
            try {
                // 设置系统原生 LookAndFeel 以获得最佳体验
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("选择 Markdown 文件夹");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);

                // 默认打开用户主目录
                chooser.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));

                int result = chooser.showOpenDialog(null);

                if (result == JFileChooser.APPROVE_OPTION) {
                    java.io.File selected = chooser.getSelectedFile();
                    if (selected != null) {
                        future.complete(selected.getAbsolutePath());
                    } else {
                        future.complete(null);
                    }
                } else {
                    future.complete(null);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future.get(120, TimeUnit.SECONDS);
    }
}
