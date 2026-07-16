package com.novelplugin.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class NovelSettingsConfigurable implements Configurable {

    private NovelSettingsPanel panel;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "PowerShell Novel Reader";
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new NovelSettingsPanel();
        reset();
        return panel.getPanel();
    }

    @Override
    public boolean isModified() {
        NovelPluginSettings s = NovelPluginSettings.getInstance();
        return !panel.getNovelFilePath().equals(s.getNovelFilePath())
                || !panel.getAlias().equals(s.getNovelFileAlias())
                || !panel.getNextPageKey().equals(s.getNextPageKey())
                || !panel.getPrevPageKey().equals(s.getPrevPageKey())
                || panel.getLinesPerPage() != s.getLinesPerPage()
                || !panel.getDisguiseMode().equals(s.getDisguiseMode());
    }

    @Override
    public void apply() {
        NovelPluginSettings s = NovelPluginSettings.getInstance();
        s.setNovelFilePath(panel.getNovelFilePath());
        s.setNovelFileAlias(panel.getAlias());
        s.setNextPageKey(panel.getNextPageKey());
        s.setPrevPageKey(panel.getPrevPageKey());
        s.setLinesPerPage(panel.getLinesPerPage());
        s.setDisguiseMode(panel.getDisguiseMode());
    }

    @Override
    public void reset() {
        NovelPluginSettings s = NovelPluginSettings.getInstance();
        panel.setNovelFilePath(s.getNovelFilePath());
        panel.setAlias(s.getNovelFileAlias());
        panel.setNextPageKey(s.getNextPageKey());
        panel.setPrevPageKey(s.getPrevPageKey());
        panel.setLinesPerPage(s.getLinesPerPage());
        panel.setDisguiseMode(s.getDisguiseMode());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }
}