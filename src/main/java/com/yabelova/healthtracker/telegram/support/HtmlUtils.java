package com.yabelova.healthtracker.telegram.support;

public final class HtmlUtils {

    private HtmlUtils() {
    }

    public static String bold(String s) {
        return "<b>" + escape(s) + "</b>";
    }

    public static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
