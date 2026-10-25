# Verity 配置器

为 Minecraft **Verity** 模组准备的安卓配置工具。不用手动翻 `config` 目录、不用记住字段名，在手机上点几下就能把 AI 接口、模组行为和外观配置好，还能一键清除 AI 记忆来修复「幻觉」。

> 适用于安装了 Verity 模组的 Minecraft Forge 1.20.1 / 1.21.1 游戏

## 功能

- **快捷配置**：选择游戏版本 → 自动定位 `verity-common.toml` → 填写模型、接口地址、密钥 → 保存
- **双格式兼容**：自动识别新版与旧版配置文件，写入时保留原有注释与排版缩进
- **模组设置**：变异天数、是否允许踢人、是否播放启动视频、极暗模式、是否允许伤害村民/动物/生物、心情条显示等开关，以及自定义名称与颜色色相
- **幻觉修复**：扫描存档中的 `verity_memory.json`，确认后删除，让 AI 重新开始记忆
- **显示设置**：深色/浅色/跟随系统、莫奈取色（关闭后为白色 + 天蓝色主题）、中英文切换
- **配置设置**：自定义启动器根目录，默认 `/storage/emulated/0/FCL`

## 环境要求

| 项目 | 要求 |
| --- | --- |
| 系统 | Android 7.0（API 24）及以上 |
| 权限 | 「所有文件访问权限」，用于读写游戏目录 |
| 启动器 | 需要有 `.minecraft/versions` 目录，默认按 FCL 的路径结构查找 |

## 使用

1. 首次打开时按提示授予「所有文件访问权限」
2. 如果游戏不在默认路径，先到 **设置 → 配置设置** 修改启动器根目录
3. 回到首页点 **快捷配置**，选择已安装的 Forge 版本，点「下一步」
4. 填写 AI 对应的模型名、接口地址与密钥，按需调整模组设置
5. 保存后**重启游戏**生效

## 从源码构建

本项目**不依赖 Android SDK / Android Studio**，只用 Termux 自带的工具链和 Gradle 完成构建，因此也可以在手机本地编译。

### 依赖

- JDK 21
- Gradle 8+（开发时使用 9.6.1）
- `aapt2`、`d8`（Android build-tools 中的工具，Termux 可通过 `pkg install aapt2` 等获取）
- 首次构建需要联网下载依赖（androidx、Material、Robolectric 的 `android-all` 作为编译期 android.jar 替代）

### 构建

```bash
gradle assemble
```

产物输出到 `out/` 目录。

构建脚本 `build.gradle` 直接用 Gradle 任务串起整条流水线：

```
unpackAars → compileRes → linkRes → compileJava → classesJar → dex → packageApk → compileSigner → signApk
```

签名使用 `toolchain/` 下的 apksig 与自写的 `SignApk.java`，容器为 debug 自签名，仅供本地安装测试。

## 项目结构

```
app/src/main/
├── AndroidManifest.xml
├── java/com/verity/tool/
│   ├── MainActivity.java            首页
│   ├── QuickConfigActivity.java     快捷配置
│   ├── MemoryFixActivity.java       幻觉修复
│   ├── SettingsActivity.java        设置
│   ├── DisplaySettingsActivity.java 显示设置
│   ├── AboutActivity.java           关于
│   ├── PathSettingsActivity.java    路径设置
│   ├── BaseActivity.java            主题 / 语言统一入口
│   └── util/
│       ├── TomlEditor.java          TOML 读写（保留注释与缩进）
│       ├── McPaths.java             游戏目录探测
│       ├── Prefs.java               本地偏好存储
│       ├── Validator.java           AI 参数校验
│       └── ThemeUtils.java
└── res/                             布局、字符串（中/英）、主题、颜色
docs/                                开发与构建笔记、历史版本说明
samples/                             新旧两版 verity-common.toml 示例
build.gradle                         无 AGP 的手工构建流水线
```

## 配置文件格式

Verity 模组的配置在不同版本间结构不同，本项目两种都支持：

**新版**

```toml
[GeneralSettings]
canCrash = false

[GeneralSettings.AISettings]
apiKey = "..."
aiEndpoint = "..."
aiModel = "..."
useCustomAiModel = true
aiProvider = "OPENAI"

[GeneralSettings.Custom]
customName = "..."
colorHue = 200
```

**旧版**

```toml
canCrash = false

[AISettings]
apiKey = "..."
use_ollama = true
ollama_url = "..."
ollama_ai_model = "..."

[Custom]
customName = "..."
colorHue = 200
```

写入旧版配置时会自动把 `use_ollama` 置为 `true`，让请求走 ollama 分支。

## 许可

[MIT License](LICENSE) © 2026 黄桃罐头吖386

## 联系

- 邮箱：3975320058@qq.com
- QQ：3975320058
- QQ 交流群：1102998288

> 配置前建议备份 `verity-common.toml` 与存档目录。
