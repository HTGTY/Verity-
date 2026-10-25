import com.verity.tool.util.TomlEditor;
import com.verity.tool.util.Validator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class TestToml {

    public static void main(String[] args) throws Exception {
        File src = new File("/storage/emulated/0/FCL/.minecraft/versions/1.20.1-Forge/config/verity-common.toml");
        File dir = new File("/data/data/com.termux/files/home/work/work1/test/tmp");
        dir.mkdirs();
        File target = new File(dir, "verity-common.toml");
        Files.copy(src.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("原始 aiModel    = " + TomlEditor.readValue(target, "aiModel"));
        System.out.println("原始 aiEndpoint = " + TomlEditor.readValue(target, "aiEndpoint"));
        System.out.println("原始 apiKey     = " + TomlEditor.readValue(target, "apiKey"));

        boolean ok = TomlEditor.write(target, "glm-5.2",
                "https://open.bigmodel.cn/api/paas/v4", "sk-test-1234");
        System.out.println("写入结果 = " + ok);
        System.out.println("写入后 aiModel    = " + TomlEditor.readValue(target, "aiModel"));
        System.out.println("写入后 aiEndpoint = " + TomlEditor.readValue(target, "aiEndpoint"));
        System.out.println("写入后 apiKey     = " + TomlEditor.readValue(target, "apiKey"));
        System.out.println("写入后 provider   = " + TomlEditor.readValue(target, "aiProvider"));
        System.out.println("写入后 custom     = " + TomlEditor.readValue(target, "useCustomAiModel"));

        List<String> lines = Files.readAllLines(target.toPath(), StandardCharsets.UTF_8);
        int shown = 0;
        for (int i = 0; i < lines.size() && shown < 40; i++) {
            String line = lines.get(i);
            if (line.contains("apiKey") || line.contains("aiEndpoint") || line.contains("aiModel")
                    || line.contains("aiProvider") || line.contains("useCustomAiModel")
                    || line.contains("AISettings")) {
                System.out.println("  | " + line);
                shown++;
            }
        }

        System.out.println("校验 空值      = " + Validator.validate("", "https://a.com", "k"));
        System.out.println("校验 含空格    = " + Validator.validate("glm 5.2", "https://a.com", "k"));
        System.out.println("校验 http      = " + Validator.validate("m", "http://a.com", "k"));
        System.out.println("校验 特殊符号  = " + Validator.validate("m", "https://a.com", "sk\"x"));
        System.out.println("校验 通过      = " + Validator.validate("glm-5.2", "https://api.deepseek.com", "sk-abc_123"));
    }
}
