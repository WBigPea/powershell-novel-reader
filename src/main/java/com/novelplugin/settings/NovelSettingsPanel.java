package com.novelplugin.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

/**
 * 设置面板 UI。
 */
public class NovelSettingsPanel {

    private final JPanel mainPanel;
    private final TextFieldWithBrowseButton novelFileField;
    private final JBTextField aliasField;
    private final JBTextField nextPageKeyField;
    private final JBTextField prevPageKeyField;
    private final JBTextField linesPerPageField;
    private final JComboBox<String> disguiseModeCombo;

    private static final String[] DISGUISE_OPTIONS = {
            "无伪装（纯文本）",
            "日志伪装（带时间戳和日志级别）",
            "系统输出穿插（混合编译/系统信息）",
            "Git Diff 伪装（代码审查风格）",
            "进度条伪装（底部显示处理进度）",
            "Tail -f 伪装（不清屏 + 命令上下文）"
    };

    public NovelSettingsPanel() {
        novelFileField = new TextFieldWithBrowseButton();
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        descriptor.withFileFilter(file -> file.getName().endsWith(".txt"));
        descriptor.setTitle("选择小说文件");
        descriptor.setDescription("选择一个 .txt 格式的小说文件");
        novelFileField.addBrowseFolderListener("选择小说文件", "选择一个 .txt 格式的小说文件", null, descriptor);

        aliasField = new JBTextField(20);
        nextPageKeyField = new JBTextField(10);
        prevPageKeyField = new JBTextField(10);
        linesPerPageField = new JBTextField(10);
        disguiseModeCombo = new ComboBox<>(DISGUISE_OPTIONS);

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("小说文件:", novelFileField)
                .addComponentToRightColumn(new JBLabel("选择 .txt 小说文件，在终端中用 cat <别名> 打开"))
                .addVerticalGap(8)
                .addLabeledComponent("终端别名:", aliasField)
                .addComponentToRightColumn(new JBLabel("在终端中输入 cat <别名> 即可打开此小说（默认: book）"))
                .addVerticalGap(12)
                .addSeparator()
                .addVerticalGap(8)
                .addLabeledComponent("伪装模式:", disguiseModeCombo)
                .addComponentToRightColumn(new JBLabel("选择小说显示时的伪装方式，下方有各模式说明"))
                .addVerticalGap(4)
                .addComponent(new JBLabel("  ┌ 无伪装：直接显示纯文本小说内容"))
                .addComponent(new JBLabel("  ├ 日志伪装：每行加 [时间戳] [INFO/WARN/ERROR] 前缀，像应用日志"))
                .addComponent(new JBLabel("  ├ 系统输出穿插：每 5-8 行穿插假编译/CI/部署输出"))
                .addComponent(new JBLabel("  ├ Git Diff：显示 diff 文件头，每行标 +/- 号，像 code review"))
                .addComponent(new JBLabel("  ├ 进度条伪装：底部显示进度条，像在处理日志流"))
                .addComponent(new JBLabel("  └ Tail -f：不清屏，内容上滚，底部保留命令上下文"))
                .addVerticalGap(12)
                .addSeparator()
                .addVerticalGap(8)
                .addLabeledComponent("下一页快捷键:", nextPageKeyField)
                .addComponentToRightColumn(new JBLabel("默认: n"))
                .addVerticalGap(8)
                .addLabeledComponent("上一页快捷键:", prevPageKeyField)
                .addComponentToRightColumn(new JBLabel("默认: p"))
                .addVerticalGap(8)
                .addLabeledComponent("每页行数:", linesPerPageField)
                .addComponentToRightColumn(new JBLabel("每页显示行数，范围 5-100（默认: 28）"))
                .addVerticalGap(16)
                .addComponent(new JBLabel("阅读进度会自动保存，下次打开同一本书时从上次的位置继续。"))
                .addComponent(new JBLabel("修改每页行数后重新打开小说，阅读位置不会丢失（按精确行号保存）。"))
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() { return mainPanel; }

    public String getNovelFilePath() { return novelFileField.getText(); }
    public void setNovelFilePath(String path) { novelFileField.setText(path); }

    public String getAlias() { return aliasField.getText(); }
    public void setAlias(String alias) { aliasField.setText(alias); }

    public String getNextPageKey() { return nextPageKeyField.getText(); }
    public void setNextPageKey(String key) { nextPageKeyField.setText(key); }

    public String getPrevPageKey() { return prevPageKeyField.getText(); }
    public void setPrevPageKey(String key) { prevPageKeyField.setText(key); }

    public int getLinesPerPage() {
        try { return Integer.parseInt(linesPerPageField.getText().trim()); }
        catch (NumberFormatException e) { return 28; }
    }
    public void setLinesPerPage(int lines) { linesPerPageField.setText(String.valueOf(lines)); }

    /** 返回伪装模式的内部 key */
    public String getDisguiseMode() {
        return modeToKey(disguiseModeCombo.getSelectedIndex());
    }
    public void setDisguiseMode(String modeKey) {
        int idx = keyToModeIndex(modeKey);
        disguiseModeCombo.setSelectedIndex(idx);
    }

    private static final String[] MODE_KEYS = {
            NovelPluginSettings.DISGUISE_NONE,
            NovelPluginSettings.DISGUISE_LOG,
            NovelPluginSettings.DISGUISE_SYSTEM_OUTPUT,
            NovelPluginSettings.DISGUISE_DIFF,
            NovelPluginSettings.DISGUISE_PROGRESS_BAR,
            NovelPluginSettings.DISGUISE_TAIL
    };

    private String modeToKey(int index) {
        if (index >= 0 && index < MODE_KEYS.length) return MODE_KEYS[index];
        return NovelPluginSettings.DISGUISE_NONE;
    }

    private int keyToModeIndex(String key) {
        for (int i = 0; i < MODE_KEYS.length; i++) {
            if (MODE_KEYS[i].equals(key)) return i;
        }
        return 0;
    }
}