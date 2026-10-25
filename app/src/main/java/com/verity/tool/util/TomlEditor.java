package com.verity.tool.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 在保留注释与格式的前提下修改 verity-common.toml，兼容新版与旧版两种结构。 */
public final class TomlEditor {

    public enum Format {
        /** 新版：[GeneralSettings]，使用 aiEndpoint / aiModel / aiProvider。 */
        MODERN,
        /** 旧版：主设置平铺在文件顶部，使用 use_ollama + ollama_url / ollama_ai_model。 */
        LEGACY
    }

    /** 读取到的一组 AI 配置值，字段可能为 null。 */
    public static final class AiConfig {
        public final String model;
        public final String url;
        public final String key;

        AiConfig(String model, String url, String key) {
            this.model = model;
            this.url = url;
            this.key = key;
        }
    }

    /** 文件顶部、第一个段名之前的区域，旧版本的主设置都写在这里。 */
    private static final String ROOT_SECTION = "";
    private static final String LEGACY_AI_SECTION = "[AISettings]";
    private static final String LEGACY_CUSTOM_SECTION = "[Custom]";
    private static final String MODERN_AI_SECTION = "[GeneralSettings.AISettings]";
    private static final String MODERN_TOGGLES_SECTION = "[GeneralSettings]";
    private static final String MODERN_CUSTOM_SECTION = "[GeneralSettings.Custom]";

    private TomlEditor() {
    }

    /** 依据文件中的段名判断配置格式；没有 GeneralSettings 段的即为旧版。 */
    public static Format detectFormat(File file) {
        for (String line : readLines(file)) {
            if (line.trim().startsWith("[GeneralSettings")) {
                return Format.MODERN;
            }
        }
        return Format.LEGACY;
    }

    /** 按格式读取当前 AI 配置。 */
    public static AiConfig readAi(File file, Format format) {
        String section = aiSection(format);
        if (format == Format.LEGACY) {
            return new AiConfig(readValue(file, format, section, "ollama_ai_model"),
                    readValue(file, format, section, "ollama_url"),
                    readValue(file, format, section, "apiKey"));
        }
        return new AiConfig(readValue(file, format, section, "aiModel"),
                readValue(file, format, section, "aiEndpoint"),
                readValue(file, format, section, "apiKey"));
    }

    /** 模型 / 地址 / 密钥：旧版走 ollama 通道，新版走 OPENAI 兼容接口。 */
    public static boolean writeAi(File file, Format format, String model, String url, String key) {
        List<String> lines = readLines(file);
        if (lines.isEmpty()) {
            return false;
        }
        int[] range = sectionRange(lines, aiSection(format), false);
        if (range == null) {
            return false;
        }
        String indent = detectIndent(lines, range[0], range[1]);
        int end = range[1];
        if (format == Format.LEGACY) {
            end = setValue(lines, range[0], end, indent, "apiKey", quote(key));
            end = setValue(lines, range[0], end, indent, "use_ollama", "true");
            end = setValue(lines, range[0], end, indent, "ollama_url", quote(url));
            end = setValue(lines, range[0], end, indent, "ollama_ai_model", quote(model));
        } else {
            end = setValue(lines, range[0], end, indent, "apiKey", quote(key));
            end = setValue(lines, range[0], end, indent, "aiEndpoint", quote(url));
            end = setValue(lines, range[0], end, indent, "aiModel", quote(model));
            end = setValue(lines, range[0], end, indent, "useCustomAiModel", "true");
            end = setValue(lines, range[0], end, indent, "aiProvider", quote("OPENAI"));
        }
        return writeLines(file, lines);
    }

    /** 模组主设置所在的段：旧版在文件顶部，新版在 [GeneralSettings]。 */
    public static String togglesSection(Format format) {
        return format == Format.LEGACY ? ROOT_SECTION : MODERN_TOGGLES_SECTION;
    }

    /** 名称与色相所在的段。 */
    public static String customSection(Format format) {
        return format == Format.LEGACY ? LEGACY_CUSTOM_SECTION : MODERN_CUSTOM_SECTION;
    }

    /** 读取指定段中的键值（去除引号）；section 为空字符串表示文件顶部。 */
    public static String readValue(File file, Format format, String section, String key) {
        return readValue(readLines(file), section, key);
    }

    /** 批量写入原始值（true / false / 数字 / 已加引号的字符串）。 */
    public static boolean writeValues(File file, Format format, String section,
                                      Map<String, String> values) {
        List<String> lines = readLines(file);
        if (lines.isEmpty() || values.isEmpty()) {
            return false;
        }
        int[] range = sectionRange(lines, section, false);
        if (range == null) {
            return false;
        }
        String indent = detectIndent(lines, range[0], range[1]);
        int end = range[1];
        for (Map.Entry<String, String> entry : values.entrySet()) {
            end = setValue(lines, range[0], end, indent, entry.getKey(), entry.getValue());
        }
        return writeLines(file, lines);
    }

    /** 按给定顺序读取若干设置项，缺失的项不放入结果。 */
    public static Map<String, String> readValues(File file, Format format, String section,
                                                 String[] keys) {
        Map<String, String> values = new LinkedHashMap<>();
        List<String> lines = readLines(file);
        for (String key : keys) {
            String value = readValue(lines, section, key);
            if (value != null) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static String readValue(List<String> lines, String section, String key) {
        int[] range = sectionRange(lines, section, true);
        if (range == null) {
            return null;
        }
        for (int i = range[0]; i < range[1]; i++) {
            String line = lines.get(i);
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            if (line.substring(0, eq).trim().equals(key)) {
                return unquote(line.substring(eq + 1).trim());
            }
        }
        return null;
    }

    private static String aiSection(Format format) {
        return format == Format.LEGACY ? LEGACY_AI_SECTION : MODERN_AI_SECTION;
    }

    /** 返回段内内容范围 [start, end)；readOnly 为 true 时段缺失返回 null。 */
    private static int[] sectionRange(List<String> lines, String section, boolean readOnly) {
        if (ROOT_SECTION.equals(section)) {
            int end = lines.size();
            for (int i = 0; i < lines.size(); i++) {
                if (isSectionHeader(lines.get(i))) {
                    end = i;
                    break;
                }
            }
            return new int[]{0, end};
        }

        int start = findSection(lines, section);
        if (start < 0) {
            if (readOnly) {
                return null;
            }
            lines.add("");
            lines.add(section);
            start = lines.size() - 1;
        }
        int end = start + 1;
        while (end < lines.size() && !isSectionHeader(lines.get(end))) {
            end++;
        }
        return new int[]{start + 1, end};
    }

    /** 更新或追加一行，返回新的段尾下标。 */
    private static int setValue(List<String> lines, int start, int end, String indent,
                                String key, String value) {
        for (int i = start; i < end && i < lines.size(); i++) {
            String line = lines.get(i);
            int eq = line.indexOf('=');
            if (eq >= 0 && line.substring(0, eq).trim().equals(key)) {
                lines.set(i, indent + key + " = " + value);
                return end;
            }
        }
        lines.add(end, indent + key + " = " + value);
        return end + 1;
    }

    private static int findSection(List<String> lines, String section) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals(section)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isSectionHeader(String line) {
        return line.trim().startsWith("[");
    }

    private static String detectIndent(List<String> lines, int start, int end) {
        for (int i = start; i < end && i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            int index = 0;
            while (index < line.length() && (line.charAt(index) == ' ' || line.charAt(index) == '\t')) {
                index++;
            }
            return line.substring(0, index);
        }
        return "\t";
    }

    /** 转成 TOML 字符串字面量，去掉会破坏格式的引号与反斜杠。 */
    public static String quote(String value) {
        return "\"" + value.replace("\\", "").replace("\"", "") + "\"";
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static List<String> readLines(File file) {
        List<String> lines = new ArrayList<>();
        if (!file.exists()) {
            return lines;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            lines.clear();
        } finally {
            close(reader);
        }
        return lines;
    }

    private static boolean writeLines(File file, List<String> lines) {
        File dir = file.getParentFile();
        File tmp = new File(dir, file.getName() + ".tmp");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(tmp), StandardCharsets.UTF_8));
            for (int i = 0; i < lines.size(); i++) {
                writer.write(lines.get(i));
                if (i < lines.size() - 1) {
                    writer.newLine();
                }
            }
            writer.flush();
        } catch (IOException e) {
            return false;
        } finally {
            close(writer);
        }
        return tmp.renameTo(file);
    }

    private static void close(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }
}
