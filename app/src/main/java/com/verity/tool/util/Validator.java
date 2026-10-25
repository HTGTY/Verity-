package com.verity.tool.util;

/** 配置输入校验。返回 null 表示通过，否则返回错误提示。 */
public final class Validator {

    /** 允许的字符：字母、数字及常见 URL / 密钥符号，不含空格与特殊符号。 */
    private static final String SAFE_PATTERN = "[A-Za-z0-9._:/=?&@%+~\\-]+";

    private Validator() {
    }

    public static String validate(String model, String url, String key) {
        String error = checkNotEmpty("模型名称 Model", model);
        if (error != null) {
            return error;
        }
        error = checkNotEmpty("调用地址 URL", url);
        if (error != null) {
            return error;
        }
        error = checkNotEmpty("密钥 Key", key);
        if (error != null) {
            return error;
        }

        error = checkSafe("模型名称 Model", model);
        if (error != null) {
            return error;
        }
        error = checkSafe("调用地址 URL", url);
        if (error != null) {
            return error;
        }
        error = checkSafe("密钥 Key", key);
        if (error != null) {
            return error;
        }

        String lower = url.toLowerCase();
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
            return "调用地址 URL 必须以 http:// 或 https:// 开头";
        }
        return null;
    }

    private static String checkNotEmpty(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return label + " 不能为空";
        }
        return null;
    }

    private static String checkSafe(String label, String value) {
        if (value.contains(" ") || value.contains("\t") || value.contains("\u3000")) {
            return label + " 不能包含空格";
        }
        if (!value.matches(SAFE_PATTERN)) {
            return label + " 包含不被允许的特殊符号";
        }
        return null;
    }
}
