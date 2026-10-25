#!/data/data/com.termux/files/usr/bin/bash
# 轻量 Android APK 构建脚本（无需 Android SDK）：aapt2 + javac + d8 + apksig 签名
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
TC="$ROOT/toolchain"
SRC="$ROOT/app/src/main"
BUILD="$ROOT/build"
OUT="$ROOT/out"

MIN_SDK=24
TARGET_SDK=33
ANDROID_JAR="$TC/android-all.jar"

if [ ! -f "$ANDROID_JAR" ]; then
    echo "缺少编译用的 android.jar：$ANDROID_JAR" >&2
    exit 1
fi

echo "==> 清理输出目录"
rm -rf "$BUILD" "$OUT"
mkdir -p "$BUILD"/{aar-res,aar-classes,compiled,gen,classes,dex} "$OUT"

echo "==> 解压 AAR 依赖"
for aar in "$TC"/aars/*.aar; do
    name="$(basename "$aar" .aar)"
    target="$BUILD/aar-res/$name"
    mkdir -p "$target"
    unzip -qo "$aar" -d "$target" 2>/dev/null || true
    if [ -f "$target/classes.jar" ]; then
        cp "$target/classes.jar" "$BUILD/aar-classes/$name.jar"
    fi
done

echo "==> 编译依赖资源"
for res in "$BUILD"/aar-res/*/res; do
    [ -d "$res" ] || continue
    name="$(basename "$(dirname "$res")")"
    aapt2 compile --dir "$res" -o "$BUILD/compiled/dep-$name.zip"
done
aapt2 compile --dir "$SRC/res" -o "$BUILD/compiled/app.zip"

echo "==> 链接资源（生成 R.java 与资源表）"
DEPS=()
for zip in "$BUILD"/compiled/dep-*.zip; do
    DEPS+=(-R "$zip")
done

# 收集依赖库包名，为它们各自生成 R.java（库代码运行时会引用自己的 R 类）
EXTRA_PACKAGES=""
for mf in "$BUILD"/aar-res/*/AndroidManifest.xml; do
    [ -f "$mf" ] || continue
    pkg="$(sed -n 's/.*package="\([^"]*\)".*/\1/p' "$mf" | head -1)"
    if [ -n "$pkg" ]; then
        EXTRA_PACKAGES="${EXTRA_PACKAGES:+$EXTRA_PACKAGES:}$pkg"
    fi
done
echo "    依赖包：$EXTRA_PACKAGES"

aapt2 link \
    -I "$ANDROID_JAR" \
    --auto-add-overlay \
    --min-sdk-version "$MIN_SDK" \
    --target-sdk-version "$TARGET_SDK" \
    --manifest "$SRC/AndroidManifest.xml" \
    --java "$BUILD/gen" \
    --extra-packages "$EXTRA_PACKAGES" \
    -o "$OUT/app-unsigned.apk" \
    "${DEPS[@]}" \
    "$BUILD/compiled/app.zip"

echo "==> 编译 Java 源码"
CP="$ANDROID_JAR"
for jar in "$BUILD"/aar-classes/*.jar "$TC"/aars/*.jar; do
    CP="$CP:$jar"
done
find "$SRC/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
javac --release 8 -nowarn -encoding UTF-8 \
    -cp "$CP" \
    -sourcepath "$SRC/java:$BUILD/gen" \
    -d "$BUILD/classes" \
    @"$BUILD/sources.txt"
(cd "$BUILD/classes" && zip -qr ../app-classes.jar .)

echo "==> 转换 dex（d8）"
java -Xmx1536m -cp "$TC/r8.jar" com.android.tools.r8.D8 \
    --lib "$ANDROID_JAR" \
    --min-api "$MIN_SDK" \
    --output "$BUILD/dex" \
    "$BUILD/app-classes.jar" "$BUILD"/aar-classes/*.jar "$TC"/aars/*.jar

echo "==> 打包 dex 进 APK"
for dex in "$BUILD"/dex/*.dex; do
    zip -j -q "$OUT/app-unsigned.apk" "$dex"
done

echo "==> 签名"
if [ ! -f "$TC/debug.keystore" ]; then
    keytool -genkeypair -v -keystore "$TC/debug.keystore" -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10950 \
        -storepass android -keypass android \
        -dname "CN=Verity Tool, OU=Debug, O=Verity, L=Unknown, ST=Unknown, C=CN" >/dev/null
fi
javac -cp "$TC/apksig.jar" -d "$TC" "$TC/SignApk.java"
java -cp "$TC/apksig.jar:$TC" SignApk "$TC/debug.keystore" android androiddebugkey android \
    "$OUT/app-unsigned.apk" "$OUT/VerityTool.apk" "$MIN_SDK"

echo "==> 完成：$OUT/VerityTool.apk"
ls -la "$OUT/VerityTool.apk"
