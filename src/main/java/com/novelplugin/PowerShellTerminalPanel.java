package com.novelplugin;

import com.intellij.openapi.project.Project;
import com.novelplugin.settings.NovelPluginSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 伪装成 PowerShell 终端的小说阅读器面板。
 * 支持六种显示模式：纯文本、日志伪装、系统输出穿插、Git Diff、进度条、Tail -f。
 */
public class PowerShellTerminalPanel extends JPanel {

    private static final Color BG_COLOR = new Color(0x0B, 0x0E, 0x14);
    private static final Color PROMPT_COLOR = new Color(0x56, 0x9C, 0xD6);
    private static final Color OUTPUT_COLOR = new Color(0xD4, 0xD4, 0xD4);
    private static final Color COMMAND_COLOR = new Color(0xE8, 0xE8, 0xE8);
    private static final Color ERROR_COLOR = new Color(0xF4, 0x47, 0x47);
    private static final Color SUCCESS_COLOR = new Color(0x4E, 0xC9, 0xB0);
    private static final Color WARN_COLOR = new Color(0xDC, 0xDC, 0x42);
    private static final Color DIM_COLOR = new Color(0x80, 0x80, 0x80);
    private static final Color DIFF_ADD_COLOR = new Color(0x4E, 0xC9, 0x4E);
    private static final Color DIFF_DEL_COLOR = new Color(0xF4, 0x47, 0x47);
    private static final Color DIFF_HUNK_COLOR = new Color(0x56, 0x9C, 0xD6);
    private static final Color TITLE_BG_START = new Color(0x01, 0x2B, 0x56);
    private static final Color TITLE_BG_END = new Color(0x00, 0x1B, 0x3E);
    private static final Color TITLE_TEXT_COLOR = new Color(0xE0, 0xE0, 0xE0);
    private static final Color SCROLLBAR_BG = new Color(0x1E, 0x1E, 0x1E);
    private static final Color SCROLLBAR_THUMB = new Color(0x42, 0x42, 0x42);

    private static final String FONT_NAME = "Consolas";
    private static final int FONT_SIZE = 14;
    private int linesPerPage = 28;

    // 伪装用的假数据
    private static final String[] FAKE_SYSTEM_LINES = {
            "[OK] Build completed in 3.2s",
            "> Compiling module: auth-service...",
            "$ git pull origin main -- Already up to date.",
            "[System] CPU: 23% | Mem: 4.2GB/16GB",
            "> Running tests... 142 passed, 0 failed.",
            "[INFO] Connecting to database... success",
            "> Deploying to staging... done",
            "$ docker ps -- 3 containers running",
            "[Watch] File changed: src/main.ts (recompiling)",
            "> Bundle size: 234.5 kB (gzipped: 67.2 kB)",
            "[OK] Linting passed. No issues found.",
            "$ kubectl get pods -- All pods healthy",
            "> Generating API docs... finished",
            "[Cache] Hit rate: 94.2% | Miss: 5.8%",
            "$ npm audit -- 0 vulnerabilities found",
            "> Type checking... no errors",
            "[CI] Pipeline #1847 passed (2m 14s)",
            "$ git status -- nothing to commit, working tree clean",
            "> Optimizing assets... 12 files processed",
            "[Gradle] Task ':app:build' finished in 4.1s",
            "$ ssh deploy@prod -- Connection established",
            "> Indexing project... 1,247 files",
            "[JVM] GC pause (young): 12ms | Heap: 256MB/512MB",
            "$ curl -s http://localhost:8080/health -- 200 OK",
            "> Migrating database... 0 pending migrations",
            "[Nginx] 200 192.168.1.50 GET /api/users 0.003s",
    };

    private final Project project;
    private final JTextPane outputPane;
    private final StyledDocument doc;
    private final JTextField commandField;
    private final JLabel promptLabel;
    private final JLabel cursorLabel;
    private javax.swing.Timer cursorBlinkTimer;
    private javax.swing.Timer autoDisguiseTimer;

    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    private final List<String> novelLines = new ArrayList<>();
    private int currentLine = 0;
    private String currentNovelName = "";
    private long baseTimestamp = 0;
    private Random random;

    private Style promptStyle, commandStyle, outputStyle, errorStyle, successStyle;
    private Style warnStyle, dimStyle, diffAddStyle, diffDelStyle, diffHunkStyle;

    // 自动伪装状态
    private boolean isAutoDisguised = false;
    private int savedCurrentLine = 0;
    private String savedNovelName = "";

    // 老板键状态
    private boolean bossKeyActive = false;

    public PowerShellTerminalPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);

//        add(createTitleBar(), BorderLayout.NORTH);

        outputPane = new JTextPane();
        doc = outputPane.getStyledDocument();
        setupStyles();
        outputPane.setEditable(false);
        outputPane.setBackground(BG_COLOR);
        outputPane.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));
        outputPane.setCaretColor(new Color(0xD4, 0xD4, 0xD4));

        SimpleAttributeSet leftAlign = new SimpleAttributeSet();
        StyleConstants.setAlignment(leftAlign, StyleConstants.ALIGN_LEFT);
        doc.setParagraphAttributes(0, 0, leftAlign, false);

        JScrollPane scrollPane = new JScrollPane(outputPane);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setBackground(SCROLLBAR_BG);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(BG_COLOR);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));

        promptLabel = new JLabel(NovelBundle.msg("prompt"));
        promptLabel.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));
        promptLabel.setForeground(PROMPT_COLOR);
        promptLabel.setBackground(BG_COLOR);

        commandField = new JTextField();
        commandField.setBackground(BG_COLOR);
        commandField.setForeground(COMMAND_COLOR);
        commandField.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));
        commandField.setCaretColor(new Color(0xD4, 0xD4, 0xD4));
        commandField.setBorder(null);
        commandField.setFocusTraversalKeysEnabled(false);

        cursorLabel = new JLabel("_");
        cursorLabel.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));
        cursorLabel.setForeground(COMMAND_COLOR);
        cursorLabel.setBackground(BG_COLOR);
        cursorLabel.setOpaque(true);
        cursorLabel.setVisible(false);

        inputPanel.add(promptLabel, BorderLayout.WEST);
        inputPanel.add(commandField, BorderLayout.CENTER);
        inputPanel.add(cursorLabel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        setupCursorBlink();
        setupEventHandlers();
        setupAutoDisguise();
        SwingUtilities.invokeLater(this::showWelcome);
    }

    // ==================== 标题栏 ====================

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                GradientPaint gradient = new GradientPaint(0, 0, TITLE_BG_START, 0, getHeight(), TITLE_BG_END);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        titleBar.setPreferredSize(new Dimension(0, 28));
        titleBar.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JLabel iconLabel = new JLabel(">_");
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        iconLabel.setForeground(new Color(0x00, 0xAD, 0xEF));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 6));

        JLabel titleLabel = new JLabel("Windows PowerShell");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(TITLE_TEXT_COLOR);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        controls.setOpaque(false);
        controls.add(createTitleButton("─", false, null));
        controls.add(createTitleButton("□", false, null));
        controls.add(createTitleButton("✕", true, () -> {
            if (project != null)
                com.intellij.openapi.wm.ToolWindowManager.getInstance(project).getToolWindow("PowerShell").hide();
        }));

        titleBar.add(iconLabel, BorderLayout.WEST);
        titleBar.add(titleLabel, BorderLayout.CENTER);
        titleBar.add(controls, BorderLayout.EAST);
        return titleBar;
    }

    private JLabel createTitleButton(String text, boolean isClose, Runnable onClick) {
        JLabel btn = new JLabel(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setForeground(new Color(0xCC, 0xCC, 0xCC));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setForeground(isClose ? new Color(0xE8, 0x48, 0x48) : Color.WHITE); }
            @Override public void mouseExited(MouseEvent e) { btn.setForeground(new Color(0xCC, 0xCC, 0xCC)); }
            @Override public void mouseClicked(MouseEvent e) { if (onClick != null) onClick.run(); }
        });
        return btn;
    }

    // ==================== 文本样式 ====================

    private void setupStyles() {
        promptStyle = makeStyle("prompt", PROMPT_COLOR);
        commandStyle = makeStyle("command", COMMAND_COLOR);
        outputStyle = makeStyle("output", OUTPUT_COLOR);
        errorStyle = makeStyle("error", ERROR_COLOR);
        successStyle = makeStyle("success", SUCCESS_COLOR);
        warnStyle = makeStyle("warn", WARN_COLOR);
        dimStyle = makeStyle("dim", DIM_COLOR);
        diffAddStyle = makeStyle("diffAdd", DIFF_ADD_COLOR);
        diffDelStyle = makeStyle("diffDel", DIFF_DEL_COLOR);
        diffHunkStyle = makeStyle("diffHunk", DIFF_HUNK_COLOR);
    }

    private Style makeStyle(String name, Color color) {
        Style s = doc.addStyle(name, null);
        StyleConstants.setForeground(s, color);
        StyleConstants.setFontFamily(s, FONT_NAME);
        StyleConstants.setFontSize(s, FONT_SIZE);
        return s;
    }

    private Style colorStyle(Color color) {
        Style s = doc.addStyle("c_" + color.getRGB(), null);
        StyleConstants.setForeground(s, color);
        StyleConstants.setFontFamily(s, FONT_NAME);
        StyleConstants.setFontSize(s, FONT_SIZE);
        return s;
    }

    // ==================== 事件处理 ====================

    private void setupCursorBlink() {
        cursorBlinkTimer = new javax.swing.Timer(530, e -> {
            if (!NovelPluginSettings.getInstance().isCursorBlinkEnabled()) {
                cursorLabel.setVisible(false);
                return;
            }
            if (commandField.isFocusOwner()) {
                cursorLabel.setVisible(!cursorLabel.isVisible());
            } else {
                cursorLabel.setVisible(false);
            }
        });
        cursorBlinkTimer.start();

        commandField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                cursorLabel.setVisible(true);
            }
            @Override public void focusLost(FocusEvent e) {
                cursorLabel.setVisible(false);
            }
        });
    }

    private void setupEventHandlers() {
        commandField.addActionListener(e -> executeCommand());
        commandField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) { navigateHistory(-1); e.consume(); }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN) { navigateHistory(1); e.consume(); }
                else if (e.getKeyCode() == KeyEvent.VK_TAB) { completeAlias(); e.consume(); }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { commandField.requestFocusInWindow(); }
        });
        commandField.registerKeyboardAction(e -> clearScreen(),
                KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_FOCUSED);

        // 老板键快捷键（注册在面板上，全局生效）
        String bossKey = NovelPluginSettings.getInstance().getBossKey();
        int bossKeyCode = parseBossKey(bossKey);
        registerKeyboardAction(e -> toggleBossKey(),
                KeyStroke.getKeyStroke(bossKeyCode, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private int parseBossKey(String key) {
        if (key == null || key.isEmpty()) return KeyEvent.VK_F12;
        switch (key.toUpperCase()) {
            case "F1": return KeyEvent.VK_F1;
            case "F2": return KeyEvent.VK_F2;
            case "F3": return KeyEvent.VK_F3;
            case "F4": return KeyEvent.VK_F4;
            case "F5": return KeyEvent.VK_F5;
            case "F6": return KeyEvent.VK_F6;
            case "F7": return KeyEvent.VK_F7;
            case "F8": return KeyEvent.VK_F8;
            case "F9": return KeyEvent.VK_F9;
            case "F10": return KeyEvent.VK_F10;
            case "F11": return KeyEvent.VK_F11;
            case "F12": return KeyEvent.VK_F12;
            case "ESC": case "ESCAPE": return KeyEvent.VK_ESCAPE;
            case "PAUSE": case "BREAK": return KeyEvent.VK_PAUSE;
            case "SCROLL": return KeyEvent.VK_SCROLL_LOCK;
            case "INSERT": return KeyEvent.VK_INSERT;
            default: return KeyEvent.VK_F12;
        }
    }

    // ==================== 自动伪装（定时器轮询 + 焦点兜底） ====================

    private void setupAutoDisguise() {
        autoDisguiseTimer = new javax.swing.Timer(500, e -> {
            NovelPluginSettings settings = NovelPluginSettings.getInstance();
            if (!settings.isAutoDisguiseEnabled()) {
                if (isAutoDisguised && !bossKeyActive) {
                    SwingUtilities.invokeLater(this::restoreDisguiseMode);
                }
                return;
            }
            // 老板键激活时，自动伪装不干涉
            if (bossKeyActive) return;
            PointerInfo pi = MouseInfo.getPointerInfo();
            if (pi == null) return;
            Point mouseScreen = pi.getLocation();
            try {
                if (!isShowing()) return;
                Point panelScreen = getLocationOnScreen();
                Rectangle bounds = new Rectangle(panelScreen.x, panelScreen.y, getWidth(), getHeight());
                boolean inside = bounds.contains(mouseScreen);

                if (!inside && !isAutoDisguised && !novelLines.isEmpty()) {
                    SwingUtilities.invokeLater(this::triggerAutoDisguise);
                } else if (inside && isAutoDisguised) {
                    SwingUtilities.invokeLater(this::restoreDisguiseMode);
                }
            } catch (IllegalComponentStateException ignored) {
                // 面板尚未显示在屏幕上，跳过本次检查
            }
        });
        autoDisguiseTimer.start();

        // 焦点兜底：用户点击输入框时强制恢复（老板键状态不干涉）
        commandField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (isAutoDisguised && !bossKeyActive) {
                    restoreDisguiseMode();
                }
            }
        });
    }

    private void triggerAutoDisguise() {
        NovelPluginSettings settings = NovelPluginSettings.getInstance();
        if (!settings.isAutoDisguiseEnabled() || novelLines.isEmpty() || isAutoDisguised) return;
        // 立即保存当前阅读进度
        settings.saveReadingProgress(currentNovelName, currentLine);
        savedCurrentLine = currentLine;
        savedNovelName = currentNovelName;
        isAutoDisguised = true;
        SwingUtilities.invokeLater(this::renderFakeLogs);
    }

    private void restoreDisguiseMode() {
        if (!isAutoDisguised) return;
        isAutoDisguised = false;
        currentLine = savedCurrentLine;
        currentNovelName = savedNovelName;
        SwingUtilities.invokeLater(this::showNovelPage);
    }

    // ==================== 老板键（一键伪装） ====================

    private void toggleBossKey() {
        if (bossKeyActive) {
            // 关闭老板键，恢复小说
            bossKeyActive = false;
            if (isAutoDisguised) {
                isAutoDisguised = false;
            }
            if (currentNovelName != null && !currentNovelName.isEmpty() || !novelLines.isEmpty()) {
                currentLine = savedCurrentLine;
                SwingUtilities.invokeLater(this::showNovelPage);
            } else {
                SwingUtilities.invokeLater(this::showWelcome);
            }
        } else {
            // 激活老板键，保存当前状态并显示假日志
            bossKeyActive = true;
            savedCurrentLine = currentLine;
            savedNovelName = currentNovelName;
            if (!novelLines.isEmpty()) {
                NovelPluginSettings.getInstance().saveReadingProgress(currentNovelName, currentLine);
            }
            SwingUtilities.invokeLater(this::renderFakeLogs);
        }
    }

    private String getEffectiveDisguiseMode() {
        return NovelPluginSettings.getInstance().getDisguiseMode();
    }

    // ==================== 自动伪装（纯假日志） ====================

    private static final String[] FAKE_LOG_MESSAGES = {
            "Initializing Spring context...",
            "Loading configuration from application.yml",
            "Connected to database: mysql://localhost:3306/app",
            "HikariPool-1 - Starting...",
            "HikariPool-1 - Start completed.",
            "Mapped URL path [/api/users] onto handler",
            "Tomcat started on port(s): 8080 (http)",
            "Started Application in 3.241 seconds",
            "GET /api/users/123 - 200 OK 45ms",
            "POST /api/auth/login - 200 OK 120ms",
            "Cache hit for key: user:123",
            "Scheduled task 'cleanup' executed in 23ms",
            "Kafka consumer poll returned 5 records",
            "Redis PING -> PONG (latency: 2ms)",
            "File uploaded: report_20240720.pdf (2.4MB)",
            "Elasticsearch index refreshed: 1247 documents",
            "JVM heap: 256MB / 512MB (50%)",
            "GC pause (G1 Young): 18ms",
            "Thread pool: active=8, queue=0, completed=1423",
            "WebSocket connection established: session_abc123",
            "Rate limit check passed for user 123",
            "Sending email to user@example.com... sent",
            "Background job 'reportGen' completed successfully",
            "Circuit breaker 'payment-svc' state: CLOSED",
            "Feature flag 'new_ui' is enabled",
            "Audit log: user admin updated config",
            "Metrics exported to Prometheus endpoint",
            "Health check: all services UP",
            "Retry attempt 2/3 for external API call",
            "Request ID: a1b2c3d4 - processing order #88421",
    };

    private void renderFakeLogs() {
        clearScreen();
        Random rnd = new Random(System.currentTimeMillis());
        long ts = System.currentTimeMillis() - linesPerPage * 1000L;

        for (int i = 0; i < linesPerPage; i++) {
            ts += 500 + rnd.nextInt(1500);
            String time = formatTimestamp(ts);
            int r = rnd.nextInt(100);
            String level;
            Style style;
            if (r < 70) { level = "INFO "; style = dimStyle; }
            else if (r < 88) { level = "DEBUG"; style = outputStyle; }
            else if (r < 97) { level = "WARN "; style = warnStyle; }
            else { level = "ERROR"; style = errorStyle; }
            String msg = FAKE_LOG_MESSAGES[rnd.nextInt(FAKE_LOG_MESSAGES.length)];
            String thread = "http-nio-8080-exec-" + (rnd.nextInt(10) + 1);
            String cls = "c.a.a.service." + FAKE_LOG_MESSAGES[rnd.nextInt(FAKE_LOG_MESSAGES.length)].split(" ")[0].replace(":", "");
            String line = "[" + time + "] [" + level + "] [" + thread + "] " + msg;
            appendLineNoScroll(line, style);
        }

        appendLineNoScroll("", outputStyle);
//        appendLineNoScroll(NovelBundle.msg("prompt") + "_", commandStyle);
        scrollToBottom();
    }

    private void refreshLinesPerPage() {
        linesPerPage = NovelPluginSettings.getInstance().getLinesPerPage();
        if (linesPerPage < 5) linesPerPage = 5;
        if (linesPerPage > 100) linesPerPage = 100;
    }

    // ==================== 欢迎界面 ====================

    private void showWelcome() {
        appendLine("", outputStyle);
        appendLine(NovelBundle.msg("welcome.title"), successStyle);
        appendLine(NovelBundle.msg("welcome.copyright"), outputStyle);
        appendLine("", outputStyle);
        appendLine(NovelBundle.msg("welcome.readline"), outputStyle);
        appendLine("", outputStyle);
        appendLine(NovelBundle.msg("welcome.module.loaded"), successStyle);
        appendLine("", outputStyle);
        commandField.requestFocusInWindow();
    }

    // ==================== 命令路由 ====================

    private void executeCommand() {
        String input = commandField.getText().trim();
        commandField.setText("");
        if (input.isEmpty()) return;

        commandHistory.add(input);
        historyIndex = commandHistory.size();
        appendPromptWithCommand(input);

        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        NovelPluginSettings settings = NovelPluginSettings.getInstance();
        String nextKey = settings.getNextPageKey();
        String prevKey = settings.getPrevPageKey();
        String alias = settings.getNovelFileAlias();

        if (cmd.equalsIgnoreCase(nextKey)) { cmdNextPage(); return; }
        if (cmd.equalsIgnoreCase(prevKey)) { cmdPrevPage(); return; }

        switch (cmd) {
            case "help": cmdHelp(); break;
            case "clear": case "cls": clearScreen(); break;
            case "ls": case "dir": cmdListNovel(); break;
            case "cat": case "type": case "gc": case "get-content": case "open": cmdOpenNovel(args); break;
            case "n": case "next": cmdNextPage(); break;
            case "p": case "prev": case "previous": cmdPrevPage(); break;
            case "progress": cmdShowProgress(); break;
            case "boss": toggleBossKey(); break;
            case "exit":
                appendLine("", outputStyle);
                appendLine("  " + NovelBundle.msg("session.ended"), outputStyle);
                appendLine("  " + NovelBundle.msg("session.close_hint"), outputStyle);
                break;
            case "whoami":
                String username = System.getProperty("user.name", "dev");
                appendLine("  " + NovelBundle.msg("system.username_format", username), outputStyle);
                break;
            case "ipconfig": cmdIpconfig(); break;
            case "date": case "get-date":
                appendLine("", outputStyle);
                appendLine("  " + new java.util.Date().toString(), outputStyle);
                break;
            case "pwd":
                appendLine("", outputStyle);
                appendLine("  " + NovelBundle.msg("pwd.path"), outputStyle);
                break;
            case "echo": appendLine("  " + args, outputStyle); break;
            default:
                if (alias != null && !alias.isEmpty() && cmd.equalsIgnoreCase(alias)) cmdOpenNovel(alias);
                else cmdUnknown(input);
                break;
        }
    }

    // ==================== 命令实现 ====================

    private void cmdHelp() {
        NovelPluginSettings s = NovelPluginSettings.getInstance();
        appendLine("", outputStyle);
        appendLine("  " + NovelBundle.msg("cmd.help.title"), promptStyle);
        appendLine("  " + NovelBundle.msg("cmd.help.divider"), promptStyle);
        appendLine("", outputStyle);
        appendLine("  " + NovelBundle.msg("cmd.help.novel_cmds"), successStyle);
        appendLine("    " + NovelBundle.msg("cmd.help.cat"), outputStyle);
        appendLine("    " + NovelBundle.msg("cmd.help.next", s.getNextPageKey()), outputStyle);
        appendLine("    " + NovelBundle.msg("cmd.help.prev", s.getPrevPageKey()), outputStyle);
        appendLine("    " + NovelBundle.msg("cmd.help.progress"), outputStyle);
        appendLine("    " + NovelBundle.msg("cmd.help.boss", s.getBossKey()), outputStyle);
        appendLine("", outputStyle);
        appendLine("  " + NovelBundle.msg("cmd.help.terminal_cmds"), successStyle);
        appendLine("    help / clear / cls / whoami / ipconfig / date / pwd / echo", outputStyle);
        appendLine("", outputStyle);
        appendLine("  " + NovelBundle.msg("cmd.help.shortcuts"), successStyle);
        appendLine("    " + NovelBundle.msg("cmd.help.shortcuts.detail"), outputStyle);
        appendLine("", outputStyle);
    }

    private void cmdListNovel() {
        NovelPluginSettings s = NovelPluginSettings.getInstance();
        String path = s.getNovelFilePath();
        String alias = s.getNovelFileAlias();
        appendLine("", outputStyle);
        appendLine("    " + NovelBundle.msg("list.dir"), promptStyle);
        appendLine("", outputStyle);
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.exists()) {
                appendLine("    " + alias + "  ->  " + f.getName() + "    (" + formatFileSize(f.length()) + ")", outputStyle);
                int progress = s.getReadingProgress(alias);
                int totalPages = countTotalPages(path);
                if (totalPages > 0)
                    appendLine("    " + NovelBundle.msg("list.progress", progress / linesPerPage + 1, totalPages), successStyle);
            } else {
                appendLine("    " + NovelBundle.msg("list.file_not_found"), errorStyle);
            }
        } else {
            appendLine("    " + NovelBundle.msg("list.no_file"), outputStyle);
            appendLine("    " + NovelBundle.msg("list.goto_settings"), successStyle);
        }
        appendLine("", outputStyle);
    }

    private void cmdOpenNovel(String name) {
        NovelPluginSettings settings = NovelPluginSettings.getInstance();
        String alias = settings.getNovelFileAlias();
        String filePath = settings.getNovelFilePath();
        if (name == null || name.isEmpty()) name = alias;
        if (!name.equalsIgnoreCase(alias)) {
            appendLine("", outputStyle);
            appendLine("  " + NovelBundle.msg("open.not_available", name), errorStyle);
            appendLine("  " + NovelBundle.msg("open.available", alias), outputStyle);
            return;
        }
        if (filePath == null || filePath.isEmpty()) {
            appendLine("", outputStyle);
            appendLine("  " + NovelBundle.msg("open.not_configured"), errorStyle);
            appendLine("  " + NovelBundle.msg("open.not_configured_hint"), outputStyle);
            return;
        }
        File f = new File(filePath);
        if (!f.exists()) {
            appendLine("", outputStyle);
            appendLine("  " + NovelBundle.msg("open.not_found", filePath), errorStyle);
            return;
        }
        List<String> lines;
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            Charset detected = detectEncoding(fileBytes);
            String content = new String(fileBytes, detected);
            lines = Arrays.asList(content.split("\r?\n"));
        }
        catch (IOException e) {
            appendLine("", outputStyle);
            appendLine("  " + NovelBundle.msg("open.read_failed", e.getMessage()), errorStyle);
            return;
        }
        novelLines.clear();
        novelLines.addAll(lines);
        currentNovelName = alias;
        refreshLinesPerPage();
        baseTimestamp = System.currentTimeMillis() - (long) currentLine * 37;
        random = new Random(currentLine);

        int savedLine = settings.getReadingProgress(alias);
        currentLine = (savedLine > 0 && savedLine < novelLines.size()) ? savedLine : 0;

        String mode = getEffectiveDisguiseMode();
        if (NovelPluginSettings.DISGUISE_TAIL.equals(mode)) {
            // tail 模式不清屏，追加内容
            appendLine("", outputStyle);
            appendLine(NovelBundle.msg("prompt") + NovelBundle.msg("tail.command", linesPerPage), commandStyle);
        }

        showNovelPage();
    }

    // ==================== 伪装渲染核心 ====================

    private void showNovelPage() {
        if (novelLines.isEmpty()) return;
        refreshLinesPerPage();

        NovelPluginSettings settings = NovelPluginSettings.getInstance();
        String mode = getEffectiveDisguiseMode();
        String nextKey = settings.getNextPageKey();
        String prevKey = settings.getPrevPageKey();

        boolean isTail = NovelPluginSettings.DISGUISE_TAIL.equals(mode);

        // tail 模式不清屏
        if (!isTail) clearScreen();

        int pageLines = linesPerPage;
        // diff/tail 模式需要留几行给装饰
        if (NovelPluginSettings.DISGUISE_DIFF.equals(mode)) pageLines = Math.max(5, linesPerPage - 4);
        if (NovelPluginSettings.DISGUISE_TAIL.equals(mode)) pageLines = Math.max(5, linesPerPage - 3);

        int end = Math.min(currentLine + pageLines, novelLines.size());

        // 根据模式渲染
        switch (mode) {
            case NovelPluginSettings.DISGUISE_LOG:
                renderLogMode(currentLine, end);
                break;
            case NovelPluginSettings.DISGUISE_SYSTEM_OUTPUT:
                renderSystemOutputMode(currentLine, end);
                break;
            case NovelPluginSettings.DISGUISE_DIFF:
                renderDiffMode(currentLine, end);
                break;
            case NovelPluginSettings.DISGUISE_PROGRESS_BAR:
                renderProgressBarMode(currentLine, end);
                break;
            case NovelPluginSettings.DISGUISE_TAIL:
                renderTailMode(currentLine, end);
                break;
            default:
                renderPlainMode(currentLine, end);
                break;
        }

        // 页脚
        if (isTail) {
            appendLineNoScroll(NovelBundle.msg("prompt") + "_", commandStyle);
        } else if (NovelPluginSettings.DISGUISE_PROGRESS_BAR.equals(mode)) {
            int totalPages = (novelLines.size() + pageLines - 1) / pageLines;
            int currentPage = currentLine / pageLines + 1;
            double percent = (double) currentPage / totalPages * 100;
            int filled = (int) (percent / 100 * 40);
            String bar = "=".repeat(filled) + " ".repeat(40 - filled);
            appendLineNoScroll("", outputStyle);
            appendLineNoScroll("  [" + bar + "] " + String.format("%.0f%%", percent), promptStyle);
            appendLineNoScroll("  " + NovelBundle.msg("page.progress_bar", nextKey, prevKey), dimStyle);
        } else if (!NovelPluginSettings.DISGUISE_DIFF.equals(mode)) {
            int totalPages = (novelLines.size() + pageLines - 1) / pageLines;
            int currentPage = currentLine / pageLines + 1;
            appendLineNoScroll("", outputStyle);
            appendLineNoScroll("  " + NovelBundle.msg("page.info", currentPage, totalPages) + " " + NovelBundle.msg("page.nav", nextKey, prevKey), dimStyle);
        }

        settings.saveReadingProgress(currentNovelName, currentLine);

        if (isTail) {
            scrollToBottom();
        } else {
            SwingUtilities.invokeLater(() -> {
                outputPane.setCaretPosition(0);
                outputPane.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            });
        }
    }

    /** 纯文本模式 */
    private void renderPlainMode(int start, int end) {
        for (int i = start; i < end; i++) {
            String line = novelLines.get(i);
            if (line.startsWith("==") || line.startsWith("--")) appendLineNoScroll(line, promptStyle);
            else if (line.startsWith("第") && line.contains("章")) { appendLineNoScroll("", outputStyle); appendLineNoScroll(line, successStyle); }
            else appendLineNoScroll(line, outputStyle);
        }
    }

    /** 日志伪装模式：每行加时间戳 + 随机日志级别 */
    private void renderLogMode(int start, int end) {
        String[] levels = {"INFO ", "INFO ", "INFO ", "INFO ", "WARN ", "DEBUG", "ERROR"};
        Style[] levelStyles = {dimStyle, dimStyle, dimStyle, dimStyle, warnStyle, outputStyle, errorStyle};
        for (int i = start; i < end; i++) {
            String line = novelLines.get(i).trim();
            if (line.isEmpty()) { appendLineNoScroll("", outputStyle); continue; }
            long ts = baseTimestamp + (long) i * 37 + random.nextInt(20);
            String time = formatTimestamp(ts);
            int idx = random.nextInt(levels.length);
            String prefix = "[" + time + "] [" + levels[idx] + "] ";
            appendLineNoScroll(prefix + line, levelStyles[idx]);
        }
    }

    /** 系统输出穿插模式：每 5-8 行小说插入一条假系统输出 */
    private void renderSystemOutputMode(int start, int end) {
        int novelIdx = 0;
        int nextFakeLine = 4 + random.nextInt(4);
        for (int i = start; i < end; i++) {
            if (novelIdx == nextFakeLine) {
                String fake = FAKE_SYSTEM_LINES[random.nextInt(FAKE_SYSTEM_LINES.length)];
                appendLineNoScroll(fake, dimStyle);
                nextFakeLine = novelIdx + 5 + random.nextInt(4);
            }
            String line = novelLines.get(i);
            if (line.startsWith("==") || line.startsWith("--")) appendLineNoScroll(line, promptStyle);
            else if (line.startsWith("第") && line.contains("章")) { appendLineNoScroll("", outputStyle); appendLineNoScroll(line, successStyle); }
            else appendLineNoScroll(line, outputStyle);
            novelIdx++;
        }
    }

    /** Git Diff 伪装模式 */
    private void renderDiffMode(int start, int end) {
        appendLineNoScroll("diff --git a/src/main.ts b/src/main.ts", diffHunkStyle);
        appendLineNoScroll("index 3a7f2c1..8b4d9e3 100644", diffHunkStyle);
        appendLineNoScroll("--- a/src/main.ts", diffDelStyle);
        appendLineNoScroll("+++ b/src/main.ts", diffAddStyle);
        for (int i = start; i < end; i++) {
            String line = novelLines.get(i).trim();
            if (line.isEmpty()) { appendLineNoScroll("", outputStyle); continue; }
            boolean isAdd = random.nextFloat() > 0.3;
            if (isAdd) appendLineNoScroll("+ " + line, diffAddStyle);
            else appendLineNoScroll("- " + line, diffDelStyle);
        }
    }

    /** 进度条伪装模式 */
    private void renderProgressBarMode(int start, int end) {
        for (int i = start; i < end; i++) {
            String line = novelLines.get(i);
            if (line.startsWith("==") || line.startsWith("--")) appendLineNoScroll(line, promptStyle);
            else if (line.startsWith("第") && line.contains("章")) { appendLineNoScroll("", outputStyle); appendLineNoScroll(line, successStyle); }
            else appendLineNoScroll(line, outputStyle);
        }
    }

    /** Tail -f 伪装模式：不清屏，底部始终有命令上下文 */
    private void renderTailMode(int start, int end) {
        appendLineNoScroll("", outputStyle);
        for (int i = start; i < end; i++) {
            String line = novelLines.get(i);
            appendLineNoScroll(line, outputStyle);
        }
    }

    // ==================== 翻页 ====================

    private void cmdNextPage() {
        if (novelLines.isEmpty()) { appendLine("", outputStyle); appendLine("  " + NovelBundle.msg("nav.no_novel"), outputStyle); appendLine("  " + NovelBundle.msg("nav.open_hint"), outputStyle); return; }
        int nextStart = currentLine + linesPerPage;
        if (nextStart >= novelLines.size()) { appendLine("", outputStyle); appendLine("  " + NovelBundle.msg("nav.last_page"), outputStyle); return; }
        currentLine = nextStart;
        showNovelPage();
    }

    private void cmdPrevPage() {
        if (novelLines.isEmpty()) { appendLine("", outputStyle); appendLine("  " + NovelBundle.msg("nav.no_novel"), outputStyle); appendLine("  " + NovelBundle.msg("nav.open_hint"), outputStyle); return; }
        if (currentLine == 0) { appendLine("", outputStyle); appendLine("  " + NovelBundle.msg("nav.first_page"), outputStyle); return; }
        currentLine = Math.max(0, currentLine - linesPerPage);
        showNovelPage();
    }

    private void cmdShowProgress() {
        NovelPluginSettings s = NovelPluginSettings.getInstance();
        String alias = s.getNovelFileAlias();
        String path = s.getNovelFilePath();
        appendLine("", outputStyle);
        if (path == null || path.isEmpty()) { appendLine("  " + NovelBundle.msg("progress.not_configured"), outputStyle); return; }
        int savedLine = s.getReadingProgress(alias);
        int totalPages = countTotalPages(path);
        if (totalPages == 0) { appendLine("  " + NovelBundle.msg("progress.cannot_calc"), outputStyle); return; }
        int currentPage = savedLine / linesPerPage + 1;
        double percent = (double) savedLine / (totalPages * linesPerPage) * 100;
        appendLine("  " + NovelBundle.msg("progress.info", new File(path).getName()), promptStyle);
        appendLine("  " + NovelBundle.msg("progress.detail", currentPage, totalPages, String.format("%.1f%%", percent)), successStyle);
        appendLine("", outputStyle);
    }

    private void cmdIpconfig() {
        appendLine("", outputStyle);
        appendLine("  " + NovelBundle.msg("ipconfig.title"), outputStyle);
        appendLine("", outputStyle);
        appendLine("  " + NovelBundle.msg("ipconfig.adapter"), outputStyle);
        appendLine("    " + NovelBundle.msg("ipconfig.ipv4"), outputStyle);
        appendLine("    " + NovelBundle.msg("ipconfig.subnet"), outputStyle);
        appendLine("    " + NovelBundle.msg("ipconfig.gateway"), outputStyle);
        appendLine("", outputStyle);
    }

    private void cmdUnknown(String input) {
        appendLine("", outputStyle);
        appendLine("  " + NovelBundle.msg("error.unknown.prefix", input), errorStyle);
        appendLine("  " + NovelBundle.msg("error.unknown.suffix"), errorStyle);
        appendLine("", outputStyle);
        appendLine("  " + NovelBundle.msg("error.unknown.hint"), outputStyle);
    }

    // ==================== 工具方法 ====================

    private void clearScreen() {
        try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}
    }

    private void appendPromptWithCommand(String command) {
        try { doc.insertString(doc.getLength(), NovelBundle.msg("prompt") + command + "\n", commandStyle); }
        catch (BadLocationException ignored) {}
        scrollToBottom();
    }

    private void appendLine(String text, Style style) { appendLineNoScroll(text, style); scrollToBottom(); }

    private void appendLineNoScroll(String text, Style style) {
        try { doc.insertString(doc.getLength(), (text.isEmpty() ? "\n" : text + "\n"), style); }
        catch (BadLocationException ignored) {}
    }

    private void appendLine(String text, Color color) { appendLine(text, colorStyle(color)); }

    private void scrollToBottom() { SwingUtilities.invokeLater(() -> outputPane.setCaretPosition(doc.getLength())); }

    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) return;
        if (direction == -1) { if (historyIndex > 0) { historyIndex--; commandField.setText(commandHistory.get(historyIndex)); } }
        else { if (historyIndex < commandHistory.size() - 1) { historyIndex++; commandField.setText(commandHistory.get(historyIndex)); } else { historyIndex = commandHistory.size(); commandField.setText(""); } }
    }

    private void completeAlias() {
        String alias = NovelPluginSettings.getInstance().getNovelFileAlias();
        String text = commandField.getText().trim().toLowerCase();
        if (text.isEmpty() || alias == null) return;
        String[] parts = text.split("\\s+");
        String last = parts[parts.length - 1];
        if (alias.toLowerCase().startsWith(last)) {
            commandField.setText(text.substring(0, text.length() - last.length()) + alias);
            commandField.setCaretPosition(commandField.getText().length());
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    private int countTotalPages(String filePath) {
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            Charset detected = detectEncoding(fileBytes);
            String content = new String(fileBytes, detected);
            long c = content.split("\r?\n").length;
            return (int) ((c + linesPerPage - 1) / linesPerPage);
        }
        catch (IOException e) { return 0; }
    }

    private String formatTimestamp(long ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts);
        return String.format("%04d-%02d-%02d %02d:%02d:%02d.%03d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND),
                cal.get(Calendar.MILLISECOND));
    }

    // ==================== 编码自动检测 ====================

    private Charset detectEncoding(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }

        String asUtf8 = new String(bytes, StandardCharsets.UTF_8);
        byte[] reencoded = asUtf8.getBytes(StandardCharsets.UTF_8);
        if (java.util.Arrays.equals(bytes, reencoded)) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName("GBK");
        } catch (Exception e) {
            return Charset.defaultCharset();
        }
    }

    // ==================== 滚动条 ====================

    private static class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { thumbColor = SCROLLBAR_THUMB; trackColor = SCROLLBAR_BG; }
        @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
        @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
        private JButton zeroBtn() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) { g.setColor(thumbColor); g.fillRoundRect(r.x+2,r.y+2,r.width-4,r.height-4,4,4); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { g.setColor(trackColor); g.fillRect(r.x,r.y,r.width,r.height); }
    }
}