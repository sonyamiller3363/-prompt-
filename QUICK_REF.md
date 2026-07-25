# 快速参考卡：PWA → APK

## 环境检查（30秒）

```powershell
# Java
java -version

# Android SDK
$env:ANDROID_HOME

# ADB 连接
adb devices
```

## 项目结构（2分钟）

```
ReversePromptTranslator/
├── app/src/main/
│   ├── java/.../MainActivity.java
│   ├── assets/index.html
│   ├── res/values/strings.xml, styles.xml
│   └── AndroidManifest.xml
├── app/build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 核心文件（5分钟）

| 文件 | 关键配置 |
|------|---------|
| `MainActivity.java` | WebView 全屏加载 `file:///android_asset/index.html` |
| `app/build.gradle` | `minSdk 24`, `targetSdk 34`, Java 17 |
| `AndroidManifest.xml` | 包名、权限、Activity 配置 |

## 构建命令（1分钟）

```powershell
# 方式1：使用脚本
.\build.bat

# 方式2：手动构建
.\gradlew.bat assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
```

## 常见问题

| 问题 | 解决方案 |
|------|---------|
| `JAVA_HOME not set` | 设置环境变量或使用绝对路径 |
| `SDK not found` | 检查 `ANDROID_HOME` 配置 |
| `Build failed` | 查看错误信息，修复后重试 |
| `INSTALL_FAILED` | `adb install -r` 或卸载重装 |

## 关键路径

| 资源 | 路径 |
|------|------|
| Java | `E:\jdk17` |
| Android SDK | `E:\Android\Sdk\sdk` |
| ADB | `C:\platform-tools\adb.exe` |
| Gradle | `C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.4-bin\...` |
| APK 输出 | `app\build\outputs\apk\debug\app-debug.apk` |

## 一键构建

```powershell
cd "C:\Users\Administrator\Documents\Default Project\ReversePromptTranslator"
.\build.bat
```

## 验证清单

- [ ] Java 17+ 已安装
- [ ] Android SDK 已配置
- [ ] 手机已连接并授权
- [ ] 构建成功（BUILD SUCCESSFUL）
- [ ] APK 已生成（~3.4MB）
- [ ] 应用已安装到手机
- [ ] 应用可正常启动
