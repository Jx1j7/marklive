<div align="center">

<img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17" />
<img src="https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot" />
<img src="https://img.shields.io/badge/Vue-3.4-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white" alt="Vue 3" />
<img src="https://img.shields.io/badge/Element_Plus-2.6-409EFF?style=flat-square&logo=element&logoColor=white" alt="Element Plus" />
<img src="https://img.shields.io/badge/license-MIT-green?style=flat-square" alt="License" />

</div>

<h1 align="center">📝 MarkLive</h1>

<p align="center">
  <em>Write locally. Preview instantly. Publish globally.</em>
</p>

<p align="center">
  <strong>一款专为极客和独立博客作者打造的本地 Markdown 实时预览与一键发布工具。</strong><br>
  可视化管理你的 Hexo / Hugo / VitePress 博客 —— 本地写稿，毫米级预览，一键上云。
</p>

---

<div align="center">
  <img src="screenshot/marklive-preview.png" alt="MarkLive 界面预览" width="100%" />
  <p><em>▲ MarkLive 三栏布局：文章列表 · 实时编辑 · 即时预览</em></p>
</div>

---

## 💡 这是什么？

**MarkLive** 是一个轻量级的 **Markdown 博客可视化外挂（CMS）**。

它不会帮你创建 Git 仓库，也不会重新发明静态站点生成器。它做的事情很简单：

> 在你已有的、绑定好 GitHub 远程仓库的博客目录之上，提供一套**丝滑的 GUI 操作界面**，让你告别命令行写稿的痛苦。

### ⚠️ 核心前提

本工具专用于管理 **已经初始化 Git 并成功绑定了 GitHub / Gitee 远程仓库** 的本地静态博客目录。

你的博客目录应该长这样：

```bash
my-blog/
├── .git/              # ← 必须！已经 git init 且绑定 remote
├── source/
│   └── _posts/
│       ├── hello-world.md
│       └── another-post.md
├── package.json       # 或 config.toml / _config.yml
└── ...
```

如果还没有博客，请先用 [Hexo](https://hexo.io/)、[Hugo](https://gohugo.io/) 或 [VitePress](https://vitepress.dev/) 初始化一个，并确保能成功 `git push`。

---

## ✨ 核心特性

<table>
  <tr>
    <td width="48px" align="center">📂</td>
    <td>
      <strong>智能目录识别</strong><br>
      通过后端原生弹窗（JFileChooser）一键浏览并选择本地博客目录，免去手动复制路径的痛苦。macOS 和 Windows 均适配系统原生文件对话框。
    </td>
  </tr>
  <tr>
    <td align="center">📝</td>
    <td>
      <strong>左树右编，实时预览</strong><br>
      左侧文章列表清晰展示标题、标签、时间戳；中间 Markdown 编辑器；右侧基于 <code>marked</code> 毫米级实时渲染 HTML 预览。
    </td>
  </tr>
  <tr>
    <td align="center">💾</td>
    <td>
      <strong>动静分离，安全解耦</strong><br>
      <code>保存到本地</code> 仅覆写硬盘文件（零延迟）；<code>发布到 GitHub</code> 才触发 <code>git add → commit → push</code> 管道。拒绝频繁提交废 Commit，Git 历史干净整洁。
    </td>
  </tr>
  <tr>
    <td align="center">🏷️</td>
    <td>
      <strong>YAML Front-Matter 全支持</strong><br>
      自动解析和编辑 <code>title</code>、<code>tags</code>（数组 / 字符串兼容）、<code>date</code> 等元数据，保存时按标准格式写回。
    </td>
  </tr>
  <tr>
    <td align="center">🔍</td>
    <td>
      <strong>递归深度扫描</strong><br>
      遍历文件夹及其子文件夹下所有 <code>.md</code> / <code>.markdown</code> 文件，无视嵌套层级。
    </td>
  </tr>
  <tr>
    <td align="center">🪵</td>
    <td>
      <strong>全链路 Git 日志</strong><br>
      后端控制台实时打印 <code>[Git Process] [stdout]</code> 和 <code>[stderr]</code> 日志，SSH 权限、分支问题一目了然。
    </td>
  </tr>
</table>

---

## 🏗️ 技术栈

| 层级 | 技术 | 说明 |
|:---:|------|------|
| **后端** | Java 17 + Spring Boot 3.2 | RESTful API，文件 I/O，ProcessBuilder 系统进程控制 |
| **前端** | Vue 3 + Element Plus + Axios | 三栏布局，marked 实时渲染，响应式交互 |
| **自动化** | Git CLI + Java ProcessBuilder | git add / commit / push 管道，原生系统命令执行 |
| **解析** | SnakeYAML + marked | YAML Front-Matter 解析 + Markdown → HTML 渲染 |

---

## 🚀 快速开始

### 第一步：前提准备

> **在启动 MarkLive 之前，请确保以下条件已满足：**

```bash
# 1. Git 已安装并配置好全局用户信息
git config --global user.name "Your Name"
git config --global user.email "your@email.com"

# 2. 你的博客目录已经是一个 Git 仓库，且已绑定远程仓库
cd /path/to/your-blog
git remote -v
# 应输出类似：
# origin  git@github.com:yourname/your-blog.git (fetch)
# origin  git@github.com:yourname/your-blog.git (push)

# 3. 能够成功推送（SSH Key 已配置或 Token 有效）
git push origin main
# ✅ 推送成功即可，不成功请先排查 Git/SSH 配置
```

### 第二步：启动项目

```bash
# 1. 克隆仓库
git clone https://github.com/yourname/marklive.git
cd marklive

# 2. 启动后端（默认端口 8080）
mvn spring-boot:run

# 3. 另开终端，启动前端（默认端口 3000）
cd frontend
npm install
npm run dev
```

打开浏览器访问 **`http://localhost:3000`**，即可看到 MarkLive 界面。

> 💡 前端已通过 Vite proxy 自动将 `/api` 请求转发到后端 8080 端口，无需额外跨域配置。

### 第三步：日常创作流程

> 一次典型的写作与发布流程，只需 4 步：

**1️⃣ 选择目录**

点击右上角 **「浏览...」** 按钮 → 弹出原生文件夹选择框 → 选中你的博客文章目录 → 路径自动填入输入框。

**2️⃣ 扫描 & 选稿**

点击 **「扫描」** 按钮 → 左侧列表加载所有 `.md` 文章 → 点击任意文章，内容自动加载到编辑器。

**3️⃣ 编辑 & 本地保存**

在中间编辑器自由修改 Markdown 内容（Front-Matter 元数据也可直接编辑）→ 点击 **「保存到本地」** → 文件即时写回硬盘，零延迟。

**4️⃣ 发布到云端**

确认文章内容无误后，点击 **「发布到 GitHub」** → 后端自动执行：

```bash
git add .
git commit -m "Publish via MarkLive"
git push origin main
```

> ✨ 推送成功后，你的博客 CI/CD（如 GitHub Actions、Vercel、Netlify）会自动触发部署，几秒后线上即可看到最新文章。

---

## 📸 界面预览

> 完整界面截图请参见上方项目简介后的预览图。更多操作细节请查看下方 API 文档和项目结构。

---

## 🔧 API 接口一览

| 方法 | 路径 | 说明 |
|:---:|------|------|
| `GET` | `/api/markdown/scan?folderPath=...` | 扫描文件夹下所有 `.md` 文件 |
| `POST` | `/api/articles/save` | 保存文章到本地（仅文件 I/O） |
| `POST` | `/api/articles/deploy` | 执行 Git 管道推送至 GitHub |
| `GET` | `/api/config/browse` | 弹出原生文件夹选择对话框 |
| `POST` | `/api/markdown/deploy` | （旧版）Git 部署接口 |

### 保存接口示例

```json
POST /api/articles/save
{
  "filePath": "/path/to/blog/source/_posts/my-post.md",
  "title": "我的新文章",
  "tags": "Java, Spring, 教程",
  "date": "2024-06-01",
  "content": "# 我的新文章\n\n这是正文内容..."
}
```

### 发布接口示例

```json
POST /api/articles/deploy
{
  "folderPath": "/path/to/blog"
}
```

---

## 📂 项目结构

```
marklive/
├── pom.xml                          # Maven 配置
├── src/main/java/com/marklive/
│   ├── MarkliveApplication.java     # Spring Boot 启动类
│   ├── config/CorsConfig.java       # 跨域配置
│   ├── controller/
│   │   ├── ArticlesController.java  # 保存 & 发布接口
│   │   ├── ConfigController.java    # 文件夹浏览接口
│   │   ├── DeployController.java    # 旧版部署接口
│   │   └── MarkdownController.java  # 扫描接口
│   ├── model/MarkdownArticle.java   # 文章实体
│   ├── service/
│   │   ├── GitDeployService.java    # Git 管道执行服务
│   │   └── MarkdownScannerService.java  # 文件扫描 & 写入服务
│   └── util/FrontMatterParser.java  # YAML Front-Matter 解析器
├── frontend/                        # Vue 3 前端项目
│   ├── src/
│   │   ├── App.vue                  # 主页面（三栏布局）
│   │   └── main.js                  # 入口
│   ├── index.html
│   ├── package.json
│   └── vite.config.js               # Vite 配置 + API 代理
└── test-md/                         # 测试用 Markdown 文件
```

---

## 🤝 贡献

欢迎提 Issue 和 PR！如果你觉得这个项目有用，请给一个 ⭐ Star。

---

## 📄 许可证

MIT License © 2024 MarkLive

---

<p align="center">
  <sub>Built with ❤️ for indie bloggers and terminal haters.</sub>
</p>
