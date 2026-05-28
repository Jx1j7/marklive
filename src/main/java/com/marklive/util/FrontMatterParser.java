package com.marklive.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML Front-Matter 解析工具
 * 解析 Markdown 文件顶部的 ---...--- 区块
 */
public class FrontMatterParser {

    private static final Logger log = LoggerFactory.getLogger(FrontMatterParser.class);

    // 匹配 --- 开头和结尾的 YAML Front-Matter 区块
    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile(
            "^---\\s*\\n(.*?)\\n---\\s*\\n(.*)",
            Pattern.DOTALL
    );

    private static final Yaml yaml = new Yaml();

    /**
     * 解析结果封装
     */
    public static class ParseResult {
        private String title;
        private String tags;
        private String date;
        private String content;

        public ParseResult() {
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * 解析 Markdown 文本，提取 Front-Matter 和正文
     *
     * @param rawContent 原始 Markdown 文本内容
     * @return ParseResult 包含 title, tags, date, content
     */
    public static ParseResult parse(String rawContent) {
        ParseResult result = new ParseResult();

        if (rawContent == null || rawContent.isBlank()) {
            result.setContent(rawContent != null ? rawContent : "");
            return result;
        }

        Matcher matcher = FRONT_MATTER_PATTERN.matcher(rawContent);

        if (matcher.find()) {
            String yamlBlock = matcher.group(1);
            String bodyContent = matcher.group(2) != null ? matcher.group(2).trim() : "";

            // 解析 YAML 块
            parseYamlBlock(yamlBlock, result);

            // 设置正文
            result.setContent(bodyContent);
        } else {
            // 没有 Front-Matter，整个内容作为正文
            result.setContent(rawContent.trim());
        }

        return result;
    }

    /**
     * 解析 YAML 块，提取 title, tags, date
     */
    @SuppressWarnings("unchecked")
    private static void parseYamlBlock(String yamlBlock, ParseResult result) {
        try {
            Map<String, Object> yamlMap = yaml.load(yamlBlock);

            if (yamlMap == null) {
                return;
            }

            // 提取 title
            Object titleObj = yamlMap.get("title");
            if (titleObj != null) {
                result.setTitle(titleObj.toString());
            }

            // 提取 tags（支持字符串和数组两种格式）
            Object tagsObj = yamlMap.get("tags");
            if (tagsObj != null) {
                if (tagsObj instanceof java.util.List) {
                    result.setTags(String.join(", ", (java.util.List<String>) tagsObj));
                } else {
                    result.setTags(tagsObj.toString());
                }
            }

            // 提取 date
            Object dateObj = yamlMap.get("date");
            if (dateObj != null) {
                result.setDate(dateObj.toString());
            }

        } catch (Exception e) {
            log.warn("Failed to parse YAML Front-Matter: {}", e.getMessage());
        }
    }

    /**
     * 将 title、tags、date 和正文拼装成完整的 Markdown 文件内容
     * （YAML Front-Matter + 正文）
     *
     * @param title   文章标题
     * @param tags    标签字符串（逗号分隔）
     * @param date    日期
     * @param content 正文
     * @return 完整的 Markdown 文本
     */
    public static String assemble(String title, String tags, String date, String content) {
        Map<String, Object> frontMatter = new LinkedHashMap<>();
        if (title != null && !title.isBlank()) {
            frontMatter.put("title", title);
        }
        if (date != null && !date.isBlank()) {
            frontMatter.put("date", date);
        }
        if (tags != null && !tags.isBlank()) {
            // tags 按逗号分割转为 List，保持 YAML 数组格式
            String[] tagArray = tags.split("\\s*,\\s*");
            if (tagArray.length == 1) {
                frontMatter.put("tags", tagArray[0].trim());
            } else {
                frontMatter.put("tags", java.util.Arrays.asList(tagArray));
            }
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml dumpYaml = new Yaml(options);

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");

        if (!frontMatter.isEmpty()) {
            sb.append(dumpYaml.dump(frontMatter));
        }

        sb.append("---\n");
        if (content != null && !content.isBlank()) {
            sb.append("\n").append(content.stripTrailing());
        }
        sb.append("\n");

        return sb.toString();
    }
}
