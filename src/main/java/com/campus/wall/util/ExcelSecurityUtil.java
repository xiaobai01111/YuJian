package com.campus.wall.util;

/**
 * Excel 导出安全工具，防止公式注入。
 */
public final class ExcelSecurityUtil {

    private ExcelSecurityUtil() {
    }

    public static String escapeFormula(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (hasDangerousFormulaPrefix(value)) {
            return "'" + value;
        }
        return value;
    }

    private static boolean hasDangerousFormulaPrefix(String value) {
        int length = value.length();
        int index = 0;
        while (index < length) {
            char ch = value.charAt(index);
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == '\f') {
                index++;
                continue;
            }
            return ch == '=' || ch == '+' || ch == '-' || ch == '@';
        }
        return false;
    }
}
