# PowerShell Novel Reader

> A novel reader disguised as a Windows PowerShell terminal — the ultimate slacking-off tool for JetBrains IDEs.

![Platform](https://img.shields.io/badge/platform-IntelliJ%20%7C%20WebStorm%20%7C%20PyCharm%20%7C%20GoLand-blue)
![Version](https://img.shields.io/badge/version-1.0.0-green)

## Overview

PowerShell Novel Reader is a JetBrains plugin that displays a fully functional-looking Windows PowerShell terminal at the bottom of your IDE. Behind the terminal facade, it's actually a novel reader — perfect for reading .txt novels at work without raising any suspicions.

## Features

### Authentic Terminal Look
- Dark theme matching the real Windows PowerShell
- Blue gradient title bar with window control buttons (minimize, maximize, close)
- `PS C:\Users\dev\Documents>` prompt with Consolas monospace font
- Custom dark scrollbar

### Six Disguise Modes
| Mode | Description |
|------|-------------|
| **Plain Text** | Displays novel content directly |
| **Log Disguise** | Prepends `[timestamp] [INFO/WARN/ERROR]` to each line — looks like reviewing application logs |
| **System Output Interleave** | Inserts fake build/CI/deploy output every 5-8 lines, e.g., `[OK] Build completed in 3.2s` |
| **Git Diff** | Shows `diff --git` header, random `+`/`-` prefixes per line — looks like code review |
| **Progress Bar** | Footer displays `[========    ] 45%` progress bar with `> Analyzing log stream...` |
| **Tail -f** | No screen clear, content scrolls up, bottom always shows `Get-Content .\app.log -Wait -Tail 28` command context |

### Smart Reading
- **Reading progress auto-saved** — per-book position persisted across IDE restarts
- **Configurable page size** — set lines per page (5-100), progress stays accurate when changed
- **Custom page keys** — define your own next/prev page commands
- **Tab completion** — auto-complete novel alias in terminal
- **Command history** — Up/Down arrow to navigate previous commands

### Built-in Terminal Commands
`help` · `ls`/`dir` · `cat`/`type`/`open` · `n`/`next` · `p`/`prev` · `clear`/`cls` · `whoami` · `ipconfig` · `date`/`get-date` · `pwd` · `echo` · `progress` · `exit`

### Quick Toggle
Press `Ctrl+Alt+N` to instantly show/hide the PowerShell window.

## Installation

### From Disk (Recommended)
1. Build the plugin or obtain `powershell-novel-reader-1.0.0.zip`
2. Open your JetBrains IDE
3. Go to **Settings → Plugins → ⚙️ → Install Plugin from Disk**
4. Select the `.zip` file
5. Restart the IDE

### From Source
1. Clone this repository
2. Open the project in IntelliJ IDEA
3. Wait for Gradle sync to complete
4. Run **Tasks → intellij → buildPlugin**
5. Find the output zip in `build/distributions/`

## Quick Start

1. Open **Settings → Tools → PowerShell Novel Reader**
2. Click the browse button next to **Novel File** and select a `.txt` file
3. (Optional) Set an **Alias** (default: `book`), choose a **Disguise Mode**, adjust **Lines Per Page**
4. Click **Apply**
5. Open the **PowerShell** tool window at the bottom of the IDE (or press `Ctrl+Alt+N`)
6. Type `cat book` and press Enter
7. Use `n` for next page, `p` for previous page

## Configuration

All settings are available at **Settings → Tools → PowerShell Novel Reader**:

| Setting | Description | Default |
|---------|-------------|---------|
| Novel File | Path to the .txt novel file | (empty) |
| Terminal Alias | Command alias to open the novel | `book` |
| Disguise Mode | How novel text is displayed | Plain Text |
| Next Page Key | Command to go to next page | `n` |
| Prev Page Key | Command to go to previous page | `p` |
| Lines Per Page | Number of lines displayed per page (5-100) | `28` |

## Supported IDEs

- IntelliJ IDEA (2023.2+)
- WebStorm (2023.2+)
- PyCharm (2023.2+)
- GoLand (2023.2+)
- CLion (2023.2+)
- PhpStorm (2023.2+)
- Any other JetBrains IDE built on the IntelliJ Platform

## Tech Stack

- **Language**: Java 17
- **Build System**: Gradle 8.7 + IntelliJ Platform Gradle Plugin 1.17.4
- **Platform**: IntelliJ Platform (OpenAPI)
- **UI**: Swing (JTextPane, StyledDocument)

## Project Structure

```
novel-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/main/
│   ├── java/com/novelplugin/
│   │   ├── PowerShellTerminalPanel.java          # Core terminal panel + all disguise renderers
│   │   ├── PowerShellNovelWindowFactory.java     # ToolWindow factory
│   │   ├── actions/
│   │   │   └── ToggleNovelAction.java            # Ctrl+Alt+N toggle action
│   │   └── settings/
│   │       ├── NovelPluginSettings.java          # Persistent state storage
│   │       ├── NovelSettingsPanel.java           # Settings UI panel
│   │       └── NovelSettingsConfigurable.java    # Settings registration
│   └── resources/
│       ├── META-INF/plugin.xml
│       └── novels/sample.txt                     # Bundled sample novel
├── README.md
└── README_CN.md
```

## License

MIT License