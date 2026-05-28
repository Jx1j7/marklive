package com.marklive.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Markdown 文章实体类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarkdownArticle {

    /** 文件路径 */
    private String filePath;

    /** 文件名 */
    private String fileName;

    /** 文章标题 */
    private String title;

    /** 文章标签 */
    private String tags;

    /** 文章日期 */
    private String date;

    /** 文章正文（去除 Front-Matter 后的内容） */
    private String content;

    public MarkdownArticle() {
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
