package com.novelplugin;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 国际化工具类，根据 JetBrains IDE 的语言设置自动切换中英文。
 */
public final class NovelBundle {

    private static final String BUNDLE_NAME = "messages";

    private static ResourceBundle bundle;

    static {
        init();
    }

    private NovelBundle() {}

    /** 根据 IDE 语言环境初始化资源束 */
    public static void init() {
        Locale locale = Locale.getDefault();
        if ("zh".equals(locale.getLanguage())) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.SIMPLIFIED_CHINESE);
        } else {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
        }
    }

    /** 获取文本（无参数） */
    public static String msg(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /** 获取文本（带一个参数，用 {0} 占位） */
    public static String msg(String key, Object arg) {
        return msg(key, new Object[]{arg});
    }

    /** 获取文本（带多个参数，用 {0} {1} ... 占位） */
    public static String msg(String key, Object... args) {
        String pattern = msg(key);
        if (args != null && args.length > 0) {
            try {
                return java.text.MessageFormat.format(pattern, args);
            } catch (IllegalArgumentException e) {
                return pattern;
            }
        }
        return pattern;
    }
}