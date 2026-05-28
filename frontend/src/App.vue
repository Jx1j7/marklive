<template>
  <div class="app-container">
    <!-- 顶部工具栏 -->
    <el-header class="app-header">
      <div class="header-left">
        <el-icon :size="22"><Document /></el-icon>
        <span class="header-title">Marklive</span>
      </div>
      <div class="header-right">
        <el-input
          v-model="folderPath"
          placeholder="请输入文件夹路径，如 /path/to/md/folder"
          clearable
          class="path-input"
          @keyup.enter="loadArticles"
        >
          <template #prepend>文件夹</template>
        </el-input>
        <el-button @click="browseFolder" :loading="browsing">
          <el-icon><FolderOpened /></el-icon>
          浏览...
        </el-button>
        <el-button type="primary" @click="loadArticles" :loading="loading">
          <el-icon><Search /></el-icon>
          扫描
        </el-button>
      </div>
    </el-header>

    <!-- 主体三栏布局 -->
    <div class="main-body">
      <!-- 左侧：文章列表 -->
      <div class="left-panel">
        <div class="panel-header">
          <span>文章列表</span>
          <el-tag v-if="articles.length" size="small" type="info">{{ articles.length }}</el-tag>
        </div>
        <div class="article-list">
          <div v-if="articles.length === 0 && !loading" class="empty-hint">
            <el-empty description="请扫描文件夹" :image-size="80" />
          </div>
          <div
            v-for="article in articles"
            :key="article.filePath"
            class="article-item"
            :class="{ active: currentArticle?.filePath === article.filePath }"
            @click="selectArticle(article)"
          >
            <div class="article-title">{{ article.title || article.fileName }}</div>
            <div class="article-meta">
              <span v-if="article.date" class="article-date">
                <el-icon :size="12"><Calendar /></el-icon>
                {{ article.date }}
              </span>
              <span v-if="article.tags" class="article-tags">
                <el-tag
                  v-for="tag in parseTags(article.tags)"
                  :key="tag"
                  size="small"
                  type="info"
                >{{ tag }}</el-tag>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：编辑区 + 预览区 -->
      <div class="right-panels">
        <!-- 编辑区 -->
        <div class="editor-panel">
          <div class="panel-header">
            <span>
              <el-icon><Edit /></el-icon>
              编辑
            </span>
            <div v-if="currentArticle" class="editor-actions">
              <el-button size="small" type="primary" :loading="saving" @click="saveLocal">
                <el-icon><DocumentAdd /></el-icon>
                保存到本地
              </el-button>
              <el-button size="small" type="success" :loading="deploying" @click="deployToGithub">
                <el-icon><Upload /></el-icon>
                发布到 GitHub
              </el-button>
            </div>
          </div>
          <el-input
            v-model="markdownText"
            type="textarea"
            class="editor-textarea"
            placeholder="请输入 Markdown 内容，或从左侧列表选择文章..."
            resize="none"
          />
        </div>

        <!-- 预览区 -->
        <div class="preview-panel">
          <div class="panel-header">
            <span>
              <el-icon><View /></el-icon>
              预览
            </span>
            <span v-if="previewLineCount" class="line-count">{{ previewLineCount }} 行</span>
          </div>
          <div class="preview-content" v-html="renderedHtml"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { marked } from 'marked'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// --- 状态 ---
const folderPath = ref('/Users/ljx/CodeBuddy/marklive/test-md')
const articles = ref([])
const currentArticle = ref(null)
const markdownText = ref('')
const loading = ref(false)
const saving = ref(false)
const deploying = ref(false)
const browsing = ref(false)

// --- 配置 marked ---
marked.setOptions({
  breaks: true,        // 换行转 <br>
  gfm: true,           // GitHub Flavored Markdown
  highlight: null      // 可选: 使用 highlight.js
})

// --- 计算属性 ---
const renderedHtml = computed(() => {
  if (!markdownText.value) return '<p style="color: #999;">暂无内容，请选择文章或输入 Markdown 文本</p>'
  try {
    return marked.parse(markdownText.value)
  } catch (e) {
    return `<p style="color: red;">Markdown 解析错误: ${e.message}</p>`
  }
})

const previewLineCount = computed(() => {
  if (!markdownText.value) return 0
  return markdownText.value.split('\n').length
})

// --- 方法 ---

/** 调用后端弹出原生文件夹选择框，将返回路径填入输入框 */
async function browseFolder() {
  browsing.value = true
  try {
    const res = await axios.get('/api/config/browse')
    if (res.data.success && res.data.path) {
      folderPath.value = res.data.path
      ElMessage.success('已选择: ' + res.data.path)
    } else if (!res.data.success && res.data.error) {
      // 用户取消不显示错误，只在其他异常时提示
      if (res.data.error !== '用户取消了选择') {
        ElMessage.warning(res.data.error)
      }
    }
  } catch (e) {
    ElMessage.error('浏览失败: ' + (e.response?.data?.error || e.message))
  } finally {
    browsing.value = false
  }
}

/** 加载文章列表 */
async function loadArticles() {
  if (!folderPath.value.trim()) {
    ElMessage.warning('请输入文件夹路径')
    return
  }
  loading.value = true
  articles.value = []
  currentArticle.value = null
  markdownText.value = ''
  try {
    const res = await axios.get('/api/markdown/scan', {
      params: { folderPath: folderPath.value }
    })
    if (res.data.success) {
      articles.value = res.data.articles || []
      ElMessage.success(`扫描完成，找到 ${articles.value.length} 篇文章`)
    } else {
      ElMessage.error(res.data.error || '扫描失败')
    }
  } catch (e) {
    ElMessage.error('请求失败: ' + (e.response?.data?.error || e.message))
  } finally {
    loading.value = false
  }
}

/** 选中文章 */
function selectArticle(article) {
  currentArticle.value = article
  markdownText.value = rebuildMarkdown(article)
}

/** 重建 Markdown 文本（Front-Matter + 正文） */
function rebuildMarkdown(article) {
  const lines = ['---']
  if (article.title) lines.push(`title: ${article.title}`)
  if (article.date) lines.push(`date: ${article.date}`)
  if (article.tags) lines.push(`tags: ${article.tags}`)
  lines.push('---')
  lines.push('')
  if (article.content) lines.push(article.content)
  return lines.join('\n')
}

/**
 * 按钮一：保存到本地
 * 解析编辑器文本 → POST /api/articles/save → 仅写回本地 .md 文件
 */
async function saveLocal() {
  if (!currentArticle.value) {
    ElMessage.warning('请先选择一篇文章')
    return
  }

  const { title, tags, date, content } = parseEditorText(markdownText.value)

  saving.value = true
  try {
    const res = await axios.post('/api/articles/save', {
      filePath: currentArticle.value.filePath,
      title: title,
      tags: tags,
      date: date,
      content: content
    })
    if (res.data.success) {
      syncArticleData(title, tags, date, content)
      ElMessage.success('本地保存成功')
    } else {
      ElMessage.error(res.data.error || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.error || e.message))
  } finally {
    saving.value = false
  }
}

/**
 * 按钮二：发布到 GitHub
 * 将当前输入框中的文件夹路径发给后端 → git add . → commit → push origin main
 */
async function deployToGithub() {
  if (!folderPath.value.trim()) {
    ElMessage.warning('请先输入文件夹路径')
    return
  }

  deploying.value = true
  try {
    const res = await axios.post('/api/articles/deploy', {
      folderPath: folderPath.value
    })
    if (res.data.success) {
      ElMessage.success('云端发布成功！')
    } else {
      ElMessage.error(res.data.error || '发布失败')
    }
  } catch (e) {
    ElMessage.error('发布失败: ' + (e.response?.data?.error || e.message))
  } finally {
    deploying.value = false
  }
}

/** 同步更新内存中的文章数据 */
function syncArticleData(title, tags, date, content) {
  currentArticle.value.title = title
  currentArticle.value.tags = tags
  currentArticle.value.date = date
  currentArticle.value.content = content

  const idx = articles.value.findIndex(a => a.filePath === currentArticle.value.filePath)
  if (idx !== -1) {
    articles.value[idx].title = title
    articles.value[idx].tags = tags
    articles.value[idx].date = date
    articles.value[idx].content = content
  }
}

/**
 * 从编辑器完整文本中解析出 title、tags、date、content
 * 如果编辑器内容以 --- 开头则提取 Front-Matter，否则全部视为正文
 */
function parseEditorText(text) {
  const fmMatch = text.match(/^---\s*\n([\s\S]*?)\n---\s*\n([\s\S]*)$/)
  if (!fmMatch) {
    return { title: '', tags: '', date: '', content: text }
  }

  const yamlBlock = fmMatch[1]
  const body = fmMatch[2].trim()

  let title = ''
  let tags = ''
  let date = ''

  // 逐行解析 YAML
  for (const line of yamlBlock.split('\n')) {
    const titleMatch = line.match(/^title:\s*(.+)$/)
    if (titleMatch) {
      title = titleMatch[1].trim().replace(/^["']|["']$/g, '')
      continue
    }
    const dateMatch = line.match(/^date:\s*(.+)$/)
    if (dateMatch) {
      date = dateMatch[1].trim().replace(/^["']|["']$/g, '')
      continue
    }
    const tagsMatch = line.match(/^tags:\s*(.+)$/)
    if (tagsMatch) {
      const raw = tagsMatch[1].trim()
      // 去掉 YAML 数组标记 [-] 和引号
      tags = raw.replace(/^\[|\]$/g, '').replace(/["']/g, '').trim()
      continue
    }
  }

  return { title, tags, date, content: body }
}

/** 解析 tags 字符串为数组 */
function parseTags(tags) {
  if (!tags) return []
  // 支持逗号、中文逗号、空格分隔
  return tags.split(/[,，\s]+/).filter(t => t.length > 0).slice(0, 3)
}
</script>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #f0f2f5;
}

#app {
  height: 100vh;
  overflow: hidden;
}
</style>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
}

/* --- Header --- */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  font-weight: 700;
  font-size: 18px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.path-input {
  width: 380px;
}

/* --- 主区域 --- */
.main-body {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 1px;
  background: #e4e7ed;
}

/* --- 左面板：文章列表 --- */
.left-panel {
  width: 260px;
  min-width: 200px;
  background: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  gap: 8px;
  flex-shrink: 0;
}

.article-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.article-item {
  padding: 12px 16px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: all 0.15s ease;
}

.article-item:hover {
  background: #f5f7fa;
}

.article-item.active {
  background: #ecf5ff;
  border-left-color: #409eff;
}

.article-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  flex-wrap: wrap;
}

.article-date {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.article-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

/* --- 右侧双栏 --- */
.right-panels {
  flex: 1;
  display: flex;
  gap: 1px;
  background: #e4e7ed;
  overflow: hidden;
}

.editor-panel,
.preview-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}

/* --- 编辑区 --- */
.editor-textarea {
  flex: 1;
}

.editor-textarea :deep(.el-textarea__inner) {
  height: 100% !important;
  border: none;
  border-radius: 0;
  resize: none;
  font-family: 'JetBrains Mono', 'Fira Code', 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.7;
  padding: 16px 20px;
  color: #2c3e50;
  background: #fafbfc;
}

.editor-textarea :deep(.el-textarea__inner):focus {
  background: #fff;
  box-shadow: none;
}

/* --- 预览区 --- */
.preview-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
  line-height: 1.8;
  color: #2c3e50;
  font-size: 15px;
}

/* Markdown 预览样式 */
.preview-content :deep(h1) {
  font-size: 1.8em;
  font-weight: 700;
  margin: 0.67em 0 0.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #e4e7ed;
}

.preview-content :deep(h2) {
  font-size: 1.5em;
  font-weight: 600;
  margin: 1em 0 0.5em;
  padding-bottom: 0.25em;
  border-bottom: 1px solid #ebeef5;
}

.preview-content :deep(h3) {
  font-size: 1.25em;
  font-weight: 600;
  margin: 0.8em 0 0.4em;
}

.preview-content :deep(p) {
  margin: 0.6em 0;
}

.preview-content :deep(ul),
.preview-content :deep(ol) {
  padding-left: 2em;
  margin: 0.5em 0;
}

.preview-content :deep(li) {
  margin: 0.25em 0;
}

.preview-content :deep(blockquote) {
  margin: 0.8em 0;
  padding: 8px 16px;
  border-left: 4px solid #409eff;
  background: #f5f7fa;
  color: #606266;
}

.preview-content :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', 'Menlo', 'Monaco', monospace;
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.9em;
  color: #e83e8c;
}

.preview-content :deep(pre) {
  margin: 1em 0;
  padding: 16px 20px;
  background: #282c34;
  border-radius: 8px;
  overflow-x: auto;
}

.preview-content :deep(pre code) {
  background: none;
  padding: 0;
  color: #abb2bf;
  font-size: 0.9em;
  line-height: 1.6;
}

.preview-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
}

.preview-content :deep(th),
.preview-content :deep(td) {
  border: 1px solid #e4e7ed;
  padding: 8px 12px;
  text-align: left;
}

.preview-content :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.preview-content :deep(tr:nth-child(even)) {
  background: #fafbfc;
}

.preview-content :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.preview-content :deep(a:hover) {
  text-decoration: underline;
}

.preview-content :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

.preview-content :deep(hr) {
  border: none;
  border-top: 1px solid #e4e7ed;
  margin: 1.5em 0;
}

.line-count {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
}

.editor-actions {
  display: flex;
  gap: 8px;
}
</style>
