package com.campus.wall.util;

import com.campus.wall.common.BusinessException;

/**
 * 密码策略工具
 */
public final class PasswordPolicyUtil {

    private PasswordPolicyUtil() {
    }

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 32;
    private static final String POLICY_MESSAGE = "密码需为8-32位，且包含大小写字母、数字和特殊字符";

    public static void validateOrThrow(String password) {
        if (!isValid(password)) {
            throw new BusinessException(POLICY_MESSAGE);
        }
    }

    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        int length = password.length();
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int i = 0; i < length; i++) {
            char ch = password.charAt(i);
            if (Character.isUpperCase(ch)) {
                hasUpper = true;
            } else if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(ch)) {
                hasSpecial = true;
            }
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
