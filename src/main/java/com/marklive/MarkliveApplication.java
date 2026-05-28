package com.marklive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MarkliveApplication {

    public static void main(String[] args) {
        // 关闭 headless 模式，允许弹出原生 GUI 文件夹选择框
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(MarkliveApplication.class, args);
    }
}
