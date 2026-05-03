# FC 视频文字排版工具 — 中文构建说明

## 项目简介

FC 是一款 Android 视频文字排版工具，支持：
- 从相册导入或直接拍摄视频
- 一键套用**电商 / 实体门店 / 活动促销**三类预设样式
- 手指拖拽任意调整文字位置
- 使用 FFmpeg 合成并导出成品视频

---

## 环境要求

| 工具 | 最低版本 |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) 或更新 |
| JDK | 17 |
| Android SDK | compile 34 / min 24 |
| Gradle | 8.7（Wrapper 自动下载） |
| Kotlin | 1.9.23 |

---

## 本地构建步骤

### 1. 克隆仓库

```bash
git clone https://github.com/ctsunny/fc.git
cd fc
```

### 2. 用 Android Studio 打开

1. 启动 Android Studio
2. 选择 **File → Open**，选中项目根目录
3. 等待 Gradle Sync 完成（首次约 3–10 分钟，需下载依赖）

### 3. 命令行构建（可选）

```bash
# Debug APK（开发测试用）
./gradlew assembleDebug

# Release APK（未签名）
./gradlew assembleRelease
```

产出位置：
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

### 4. 安装到设备

```bash
# 通过 ADB 安装 Debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## GitHub Actions 自动构建

### 触发条件

推送到 `main` 或 `master` 分支，或创建 Pull Request 时自动触发。

### 查看构建状态

仓库主页 → **Actions** 标签页 → 选择最新的 **Android CI** 工作流。

### 下载 APK

1. 打开任意一次成功的 Actions 运行记录
2. 页面底部 **Artifacts** 区域
3. 下载 `debug-apk` 或 `release-apk-unsigned`（保留 30 天）

### 工作流文件

`.github/workflows/android.yml`，主要步骤：

```
检出代码 → 配置 JDK 17 → 赋予执行权限 → assembleDebug → 上传 → assembleRelease → 上传
```

---

## Release 签名（可选）

Release APK 默认未签名，如需发布到应用商店，需在 `app/build.gradle.kts` 中配置签名：

```kotlin
signingConfigs {
    create("release") {
        storeFile     = file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
        keyAlias      = System.getenv("KEY_ALIAS") ?: ""
        keyPassword   = System.getenv("KEY_PASSWORD") ?: ""
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

在 GitHub 仓库 **Settings → Secrets and variables → Actions** 中添加对应的 Secret。

---

## 常见问题

**Q: Gradle Sync 失败，提示无法下载依赖**  
A: 检查网络连接；在国内可在 `gradle/wrapper/gradle-wrapper.properties` 中将 distributionUrl 改为腾讯镜像或阿里镜像。

**Q: 构建报 `sdk location not found`**  
A: 在项目根目录新建 `local.properties`，内容为：
```
sdk.dir=/path/to/your/Android/sdk
```

**Q: FFmpeg 相关类找不到**  
A: 确保 `ffmpeg-kit-android-min-gpl:6.0-2` 已成功下载；可在 Android Studio 底部 **Build** 面板查看详细错误。

**Q: GitHub Actions 中 `gradlew: permission denied`**  
A: 工作流已包含 `chmod +x gradlew` 步骤；若仍报错，请确认 gradlew 文件已提交且不在 .gitignore 中。
