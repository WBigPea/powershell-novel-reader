# PowerShell 小说阅读器

> 伪装成 Windows PowerShell 终端的小说阅读器 —— 程序员摸鱼神器，适用于所有 JetBrains IDE。

![平台](https://img.shields.io/badge/平台-IntelliJ%20%7C%20WebStorm%20%7C%20PyCharm%20%7C%20GoLand-blue)
![版本](https://img.shields.io/badge/版本-1.0.0-green)

## 简介

PowerShell Novel Reader 是一款 JetBrains 插件，在 IDE 底部显示一个看起来完全真实的 Windows PowerShell 终端窗口。但在终端的外表下，它实际上是一个小说阅读器 —— 完美适合在上班时间偷偷看 .txt 小说而不被任何人发现。

## 功能特性

### 逼真的终端外观
- 深色主题，完全模拟真实 Windows PowerShell
- 蓝色渐变标题栏，带最小化/最大化/关闭按钮
- `PS C:\Users\dev\Documents>` 提示符 + Consolas 等宽字体
- 自定义暗色滚动条

### 六种伪装模式
| 模式 | 效果说明 |
|------|----------|
| **无伪装** | 直接显示小说纯文本 |
| **日志伪装** | 每行添加 `[时间戳] [INFO/WARN/ERROR]` 前缀，看起来像在排查应用日志 |
| **系统输出穿插** | 每 5-8 行穿插一条假的编译/CI/部署输出，如 `[OK] Build completed in 3.2s` |
| **Git Diff 伪装** | 顶部显示 `diff --git` 文件头，每行随机标 `+`（绿）/`-`（红），像在 code review |
| **进度条伪装** | 底部显示 `[========          ] 45%` 进度条 + `> Analyzing log stream...` |
| **Tail -f 伪装** | 不清屏，内容向上滚动，底部始终保留 `Get-Content .\app.log -Wait -Tail 28` 命令上下文 |

### 智能阅读
- **阅读进度自动保存** —— 每本书的阅读位置独立持久化，重启 IDE 不丢失
- **可配置每页行数** —— 设置范围 5-100，修改后重新打开小说位置不丢（按精确行号保存）
- **自定义翻页快捷键** —— 设置自己喜欢的翻页命令
- **Tab 自动补全** —— 在终端中自动补全小说别名
- **命令历史** —— 上下箭头浏览历史命令

### 内置终端命令
`help` · `ls`/`dir` · `cat`/`type`/`open` · `n`/`next` · `p`/`prev` · `clear`/`cls` · `whoami` · `ipconfig` · `date`/`get-date` · `pwd` · `echo` · `progress` · `exit`

### 一键切换
按 `Ctrl+Alt+N` 快速显示/隐藏 PowerShell 终端窗口。

## 安装方式

### 从磁盘安装（推荐）
1. 获取 `powershell-novel-reader-1.0.0.zip` 安装包
2. 打开 JetBrains IDE
3. 进入 **Settings → Plugins → ⚙️ → Install Plugin from Disk**
4. 选择 `.zip` 文件
5. 重启 IDE

### 从源码构建
1. 克隆本仓库
2. 用 IntelliJ IDEA 打开项目目录
3. 等待 Gradle 同步完成
4. 运行 **Tasks → intellij → buildPlugin**
5. 在 `build/distributions/` 目录找到输出的 `.zip` 文件

## 快速上手

1. 打开 **Settings → Tools → PowerShell Novel Reader**
2. 点击**小说文件**旁的浏览按钮，选择一个 `.txt` 小说文件
3. （可选）设置**终端别名**（默认 `book`），选择**伪装模式**，调整**每页行数**
4. 点击 **Apply**
5. 打开 IDE 底部的 **PowerShell** 工具窗口（或按 `Ctrl+Alt+N`）
6. 输入 `cat book` 回车，开始阅读
7. 输入 `n` 下一页，`p` 上一页

## 配置项说明

所有配置在 **Settings → Tools → PowerShell Novel Reader** 中：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| 小说文件 | .txt 小说文件的路径 | （空） |
| 终端别名 | 在终端中用此别名打开小说 | `book` |
| 伪装模式 | 小说文字的显示伪装方式 | 无伪装 |
| 下一页快捷键 | 翻到下一页的命令 | `n` |
| 上一页快捷键 | 翻到上一页的命令 | `p` |
| 每页行数 | 每页显示的行数（5-100） | `28` |

## 支持的 IDE

- IntelliJ IDEA（2023.2+）
- WebStorm（2023.2+）
- PyCharm（2023.2+）
- GoLand（2023.2+）
- CLion（2023.2+）
- PhpStorm（2023.2+）
- 以及所有基于 IntelliJ Platform 的 JetBrains IDE

## 技术栈

- **语言**：Java 17
- **构建**：Gradle 8.7 + IntelliJ Platform Gradle Plugin 1.17.4
- **平台**：IntelliJ Platform (OpenAPI)
- **UI**：Swing（JTextPane、StyledDocument）

## 项目结构

```
novel-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/main/
│   ├── java/com/novelplugin/
│   │   ├── PowerShellTerminalPanel.java          # 核心终端面板 + 全部伪装渲染器
│   │   ├── PowerShellNovelWindowFactory.java     # ToolWindow 工厂
│   │   ├── actions/
│   │   │   └── ToggleNovelAction.java            # Ctrl+Alt+N 切换动作
│   │   └── settings/
│   │       ├── NovelPluginSettings.java          # 持久化状态存储
│   │       ├── NovelSettingsPanel.java           # 设置界面面板
│   │       └── NovelSettingsConfigurable.java    # 设置注册入口
│   └── resources/
│       ├── META-INF/plugin.xml
│       └── novels/sample.txt                     # 内置示例小说
├── README.md                                     # 英文说明
└── README_CN.md                                  # 本文件
```

## 许可证

MIT License