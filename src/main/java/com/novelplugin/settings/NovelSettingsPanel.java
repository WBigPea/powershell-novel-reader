package com.novelplugin.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.novelplugin.NovelBundle;

import javax.swing.*;

/**
 * 设置面板 UI（国际化）。
 */
public class NovelSettingsPanel {

    private final JPanel mainPanel;
    private final TextFieldWithBrowseButton novelFileField;
    private final JBTextField aliasField;
    private final JBTextField nextPageKeyField;
    private final JBTextField prevPageKeyField;
    private final JBTextField linesPerPageField;
    private final JComboBox<String> disguiseModeCombo;
    private final JCheckBox cursorBlinkCheck;
    private final JCheckBox autoDisguiseCheck;
    private final JComboBox<String> autoDisguiseModeCombo;
    private final JBTextField bossKeyField;

    private static final String[] DISGUISE_OPTIONS = {
            "无伪装（纯文本）",
            "日志伪装（带时间戳和日志级别）",
            "系统输出穿插（混合编译/系统信息）",
            "Git Diff 伪装（代码审查风格）",
            "进度条伪装（底部显示处理进度）",
            "Tail -f 伪装（不清屏 + 命令上下文）"
    };

    private static final String[] DISGUISE_OPTIONS_EN = {
            "No disguise (plain text)",
            "Log disguise (timestamps + log levels)",
            "System output interleave (build/CI info)",
            "Git Diff disguise (code review style)",
            "Progress bar disguise (processing progress)",
            "Tail -f disguise (no clear + command context)"
    };

    private static final String[] AUTO_DISGUISE_OPTIONS = {
            "日志伪装",
            "系统输出穿插",
            "Git Diff 伪装",
            "进度条伪装",
            "Tail -f 伪装"
    };

    private static final String[] AUTO_DISGUISE_OPTIONS_EN = {
            "Log disguise",
            "System output interleave",
            "Git Diff disguise",
            "Progress bar disguise",
            "Tail -f disguise"
    };

    public NovelSettingsPanel() {
        novelFileField = new TextFieldWithBrowseButton();
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        descriptor.withFileFilter(file -> file.getName().endsWith(".txt"));
        descriptor.setTitle(NovelBundle.msg("settings.file.chooser.title"));
        descriptor.setDescription(NovelBundle.msg("settings.file.chooser.desc"));
        novelFileField.addBrowseFolderListener(
                NovelBundle.msg("settings.file.chooser.title"),
                NovelBundle.msg("settings.file.chooser.desc"), null, descriptor);

        aliasField = new JBTextField(20);
        nextPageKeyField = new JBTextField(10);
        prevPageKeyField = new JBTextField(10);
        linesPerPageField = new JBTextField(10);
        disguiseModeCombo = new ComboBox<>(isZh() ? DISGUISE_OPTIONS : DISGUISE_OPTIONS_EN);

        cursorBlinkCheck = new JCheckBox(NovelBundle.msg("settings.label.cursor_blink"));
        cursorBlinkCheck.setSelected(true);

        autoDisguiseCheck = new JCheckBox(NovelBundle.msg("settings.label.auto_disguise"));
        autoDisguiseCheck.setSelected(false);

        String[] autoOptions = isZh() ? AUTO_DISGUISE_OPTIONS : AUTO_DISGUISE_OPTIONS_EN;
        autoDisguiseModeCombo = new ComboBox<>(autoOptions);
        autoDisguiseModeCombo.setEnabled(false);
        autoDisguiseCheck.addActionListener(e -> autoDisguiseModeCombo.setEnabled(autoDisguiseCheck.isSelected()));

        bossKeyField = new JBTextField(10);
        bossKeyField.setText("F12");

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(NovelBundle.msg("settings.label.file"), novelFileField)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.file")))
                .addVerticalGap(8)
                .addLabeledComponent(NovelBundle.msg("settings.label.alias"), aliasField)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.alias")))
                .addVerticalGap(12)
                .addSeparator()
                .addVerticalGap(8)
                .addLabeledComponent(NovelBundle.msg("settings.label.disguise"), disguiseModeCombo)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.disguise")))
                .addVerticalGap(4)
                .addComponent(new JBLabel(NovelBundle.msg("settings.disguise.desc.0")))
                .addComponent(new JBLabel(NovelBundle.msg("settings.disguise.desc.1")))
                .addComponent(new JBLabel(NovelBundle.msg("settings.disguise.desc.2")))
                .addComponent(new JBLabel(NovelBundle.msg("settings.disguise.desc.3")))
                .addComponent(new JBLabel(NovelBundle.msg("settings.disguise.desc.4")))
                .addComponent(new JBLabel(NovelBundle.msg("settings.disguise.desc.5")))
                .addVerticalGap(12)
                .addSeparator()
                .addVerticalGap(8)
                .addComponent(cursorBlinkCheck)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.cursor_blink")))
                .addVerticalGap(8)
                .addComponent(autoDisguiseCheck)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.auto_disguise")))
                .addVerticalGap(4)
                .addLabeledComponent(NovelBundle.msg("settings.label.auto_disguise_mode"), autoDisguiseModeCombo)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.auto_disguise_mode")))
                .addVerticalGap(8)
                .addLabeledComponent(NovelBundle.msg("settings.label.boss_key"), bossKeyField)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.boss_key")))
                .addVerticalGap(12)
                .addSeparator()
                .addVerticalGap(8)
                .addLabeledComponent(NovelBundle.msg("settings.label.next_key"), nextPageKeyField)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.next_key")))
                .addVerticalGap(8)
                .addLabeledComponent(NovelBundle.msg("settings.label.prev_key"), prevPageKeyField)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.prev_key")))
                .addVerticalGap(8)
                .addLabeledComponent(NovelBundle.msg("settings.label.lines"), linesPerPageField)
                .addComponentToRightColumn(new JBLabel(NovelBundle.msg("settings.hint.lines")))
                .addVerticalGap(16)
                .addComponent(new JBLabel(NovelBundle.msg("settings.hint.progress")))
                .addComponent(new JBLabel(NovelBundle.msg("settings.hint.progress2")))
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private boolean isZh() {
        return "zh".equals(java.util.Locale.getDefault().getLanguage());
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

    public String getDisguiseMode() { return modeToKey(disguiseModeCombo.getSelectedIndex()); }
    public void setDisguiseMode(String modeKey) { disguiseModeCombo.setSelectedIndex(keyToModeIndex(modeKey)); }

    public boolean isCursorBlinkEnabled() { return cursorBlinkCheck.isSelected(); }
    public void setCursorBlinkEnabled(boolean enabled) { cursorBlinkCheck.setSelected(enabled); }

    public boolean isAutoDisguiseEnabled() { return autoDisguiseCheck.isSelected(); }
    public void setAutoDisguiseEnabled(boolean enabled) {
        autoDisguiseCheck.setSelected(enabled);
        autoDisguiseModeCombo.setEnabled(enabled);
    }

    public String getAutoDisguiseMode() {
        int idx = autoDisguiseModeCombo.getSelectedIndex();
        return (idx >= 0 && idx < AUTO_MODE_KEYS.length) ? AUTO_MODE_KEYS[idx] : NovelPluginSettings.DISGUISE_LOG;
    }
    public void setAutoDisguiseMode(String modeKey) {
        for (int i = 0; i < AUTO_MODE_KEYS.length; i++) {
            if (AUTO_MODE_KEYS[i].equals(modeKey)) { autoDisguiseModeCombo.setSelectedIndex(i); return; }
        }
        autoDisguiseModeCombo.setSelectedIndex(0);
    }

    public String getBossKey() { return bossKeyField.getText().trim(); }
    public void setBossKey(String key) { bossKeyField.setText(key); }

    private static final String[] MODE_KEYS = {
            NovelPluginSettings.DISGUISE_NONE, NovelPluginSettings.DISGUISE_LOG,
            NovelPluginSettings.DISGUISE_SYSTEM_OUTPUT, NovelPluginSettings.DISGUISE_DIFF,
            NovelPluginSettings.DISGUISE_PROGRESS_BAR, NovelPluginSettings.DISGUISE_TAIL
    };

    private static final String[] AUTO_MODE_KEYS = {
            NovelPluginSettings.DISGUISE_LOG, NovelPluginSettings.DISGUISE_SYSTEM_OUTPUT,
            NovelPluginSettings.DISGUISE_DIFF, NovelPluginSettings.DISGUISE_PROGRESS_BAR,
            NovelPluginSettings.DISGUISE_TAIL
    };

    private String modeToKey(int index) {
        return (index >= 0 && index < MODE_KEYS.length) ? MODE_KEYS[index] : NovelPluginSettings.DISGUISE_NONE;
    }

    private int keyToModeIndex(String key) {
        for (int i = 0; i < MODE_KEYS.length; i++) { if (MODE_KEYS[i].equals(key)) return i; }
        return 0;
    }
}