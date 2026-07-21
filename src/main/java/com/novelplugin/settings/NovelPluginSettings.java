package com.novelplugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 持久化存储 PowerShell Novel Reader 的设置数据。
 */
@State(
    name = "NovelPluginSettings",
    storages = @Storage("novelPluginSettings.xml")
)
@Service
public final class NovelPluginSettings implements PersistentStateComponent<NovelPluginSettings.State> {

    /** 伪装模式常量 */
    public static final String DISGUISE_NONE = "none";
    public static final String DISGUISE_LOG = "log";
    public static final String DISGUISE_SYSTEM_OUTPUT = "system_output";
    public static final String DISGUISE_DIFF = "diff";
    public static final String DISGUISE_PROGRESS_BAR = "progress_bar";
    public static final String DISGUISE_TAIL = "tail";

    public static class State {
        public String novelFilePath = "";
        public String novelFileAlias = "book";
        public String nextPageKey = "n";
        public String prevPageKey = "p";
        public int linesPerPage = 28;
        public String disguiseMode = DISGUISE_NONE;
        public boolean cursorBlinkEnabled = true;
        public boolean autoDisguiseEnabled = false;
        public String autoDisguiseMode = DISGUISE_LOG;
        public String bossKey = "F12";
        public Map<String, Integer> readingProgress = new HashMap<>();
    }

    private State state = new State();

    @Override
    public @Nullable NovelPluginSettings.State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull NovelPluginSettings.State state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }

    public static NovelPluginSettings getInstance() {
        return ApplicationManager.getApplication().getService(NovelPluginSettings.class);
    }

    public String getNovelFilePath() { return state.novelFilePath; }
    public void setNovelFilePath(String path) { state.novelFilePath = path; }

    public String getNovelFileAlias() { return state.novelFileAlias; }
    public void setNovelFileAlias(String alias) { state.novelFileAlias = alias; }

    public String getNextPageKey() { return state.nextPageKey; }
    public void setNextPageKey(String key) { state.nextPageKey = key; }

    public String getPrevPageKey() { return state.prevPageKey; }
    public void setPrevPageKey(String key) { state.prevPageKey = key; }

    public int getLinesPerPage() { return state.linesPerPage; }
    public void setLinesPerPage(int lines) { state.linesPerPage = lines; }

    public String getDisguiseMode() { return state.disguiseMode; }
    public void setDisguiseMode(String mode) { state.disguiseMode = mode; }

    public boolean isCursorBlinkEnabled() { return state.cursorBlinkEnabled; }
    public void setCursorBlinkEnabled(boolean enabled) { state.cursorBlinkEnabled = enabled; }

    public boolean isAutoDisguiseEnabled() { return state.autoDisguiseEnabled; }
    public void setAutoDisguiseEnabled(boolean enabled) { state.autoDisguiseEnabled = enabled; }

    public String getAutoDisguiseMode() { return state.autoDisguiseMode; }
    public void setAutoDisguiseMode(String mode) { state.autoDisguiseMode = mode; }

    public String getBossKey() { return state.bossKey; }
    public void setBossKey(String key) { state.bossKey = key; }

    public int getReadingProgress(String novelName) {
        return state.readingProgress.getOrDefault(novelName, 0);
    }

    public void saveReadingProgress(String novelName, int line) {
        state.readingProgress.put(novelName, line);
    }
}