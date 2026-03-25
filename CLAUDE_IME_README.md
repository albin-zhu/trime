# Claude 智能输入法 - Trime 二次开发

## 项目概述

基于 Trime（同文输入法）的二次开发，集成 AI 能力的智能五笔输入法。

## 已完成的工作

### 1. 核心模块
- **AI 模块** (`app/src/main/java/com/osfans/trime/ai/`)
  - `LocalAIClient.kt` - 本地 AI API 客户端（支持 Ollama）

- **记忆系统** (`app/src/main/java/com/osfans/trime/memory/`)
  - `MemoryManager.kt` - 记忆文件解析和管理

- **幽灵补全引擎** (`app/src/main/java/com/osfans/trime/ghost/`)
  - `GhostCompletionEngine.kt` - IDE 式智能续写

- **辅助功能** (`app/src/main/java/com/osfans/trime/accessibility/`)
  - `ScreenContentService.kt` - 屏幕内容获取服务

- **工具类** (`app/src/main/java/com/osfans/trime/util/`)
  - `ScreenContextHolder.kt` - 屏幕上下文管理

- **集成模块** (`app/src/main/java/com/osfans/trime/claude/`)
  - `ClaudeIntegration.kt` - 统一集成管理器

### 2. 配置文件
- `app/src/main/res/xml/accessibility_service_config.xml` - 辅助服务配置
- `app/src/main/res/values/claude_strings.xml` - 字符串资源
- `app/src/main/AndroidManifest.xml` - 已添加权限和服务声明
- `gradle/libs.versions.toml` - 已添加 OkHttp 和 Gson 依赖
- `app/build.gradle.kts` - 已添加依赖引用

### 3. 本地提交记录
```
22fdfb7 Add core AI module files
5868264 Add Claude AI integration
```

## 功能特性

### IDE 式幽灵补全
- 打开输入法即预测完整句子
- 输入匹配建议时自动续写
- Tab 键接受建议
- 继续生成后续建议

### 记忆系统
- 读取本地 memory.md
- 支持快捷输入（如 @addr）
- 支持序列续写（如 "收到" → "，我会尽快处理"）

### 纯本地 AI
- 支持 Ollama 本地部署
- 默认模型：qwen2.5:3b
- 支持自定义 API 地址和模型

### 屏幕内容感知
- 辅助功能获取当前应用
- 支持微信、QQ、邮件等场景识别
- 根据场景提供上下文模板

## 项目结构

```
trime/
├── app/src/main/java/com/osfans/trime/
│   ├── ai/                    # AI 客户端
│   ├── memory/                # 记忆系统
│   ├── ghost/                 # 幽灵补全引擎
│   ├── accessibility/         # 辅助功能服务
│   ├── util/                  # 工具类
│   ├── claude/                # 集成管理器
│   └── ime/core/              # Trime 核心（需手动集成）
├── app/src/main/res/
│   ├── xml/accessibility_service_config.xml
│   └── values/claude_strings.xml
└── gradle/libs.versions.toml  # 依赖版本管理
```

## 后续集成步骤

### 1. 在 TrimeInputMethodService 中初始化
```kotlin
override fun onCreate() {
    super.onCreate()
    // ... existing code ...

    // Initialize Claude AI
    val claude = ClaudeIntegration.init(this, rime)
    claude.setGhostCallback(object : GhostCompletionEngine.GhostCallback {
        override fun onGhostTextReady(ghost: GhostSuggestion) {
            // Update UI to show ghost text
        }
        override fun onGhostTextDismiss() {
            // Hide ghost text
        }
        override fun onSuggestionAccepted(fullText: String) {
            // Commit text to input
        }
    })
}
```

### 2. 处理按键事件
```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    return when (keyCode) {
        KeyEvent.KEYCODE_TAB -> {
            ClaudeIntegration.getInstance()?.acceptGhostSuggestion()
            true
        }
        KeyEvent.KEYCODE_ESCAPE -> {
            ClaudeIntegration.getInstance()?.dismissSuggestion()
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }
}
```

### 3. 启用辅助功能
用户需要在系统设置中手动开启：
设置 → 辅助功能 → Claude 输入法屏幕读取

## 本地模型部署

### 使用 Ollama（推荐）
```bash
# 安装 Ollama
# 下载轻量级中文模型
ollama pull qwen2.5:3b

# 启动服务（默认端口 11434）
ollama serve
```

### 配置
修改 `LocalAIClient` 构造函数参数：
```kotlin
LocalAIClient(
    baseUrl = "http://localhost:11434",
    model = "qwen2.5:3b"
)
```

## 构建说明

### 环境要求
- Android SDK 35
- JDK 17
- Gradle 9.4+

### 构建命令
```bash
./gradlew :app:assembleDebug
```

### 当前状态
- ✅ 所有核心模块代码已编写完成
- ✅ 依赖配置已添加
- ✅ 清单文件已更新
- ⚠️ 需要 Android SDK 环境才能编译
- ⚠️ 需要手动集成到 TrimeInputMethodService

## 隐私说明
- 所有数据处理均在本地完成
- AI 模型本地部署，无需联网
- 记忆文件存储在应用私有目录
- 辅助功能仅用于获取屏幕内容，不上传

## 许可证
GPL-3.0-or-later（与 Trime 保持一致）
