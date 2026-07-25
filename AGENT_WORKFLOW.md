# Agent 工作流：PWA → APK 打包指南

## 概述

本文档记录了将"逆向 Prompt 翻译器"PWA 应用打包成 Android APK 的完整工作流，包括环境准备、工具链配置、代码实现、问题排查和经验总结。

---

## 一、环境要求

### 1.1 必需工具

| 工具 | 版本要求 | 用途 | 安装方式 |
|------|---------|------|---------|
| **Java JDK** | 17+ | Android 编译 | `E:\jdk17` |
| **Android SDK** | API 34 | Android 平台 | `E:\Android\Sdk\sdk` |
| **Build Tools** | 34.0.0+ | APK 构建 | SDK Manager 安装 |
| **ADB** | 37.0.0+ | 设备调试/安装 | `C:\platform-tools\adb.exe` |
| **Gradle** | 8.4+ | 构建系统 | 自动下载 |

### 1.2 环境变量

```powershell
# PowerShell 检查命令
$env:ANDROID_HOME    # 应输出: E:\Android\Sdk\sdk
$env:JAVA_HOME       # 应输出: E:\jdk17
```

### 1.3 手机准备

- [ ] 启用 **开发者选项**（设置 → 关于手机 → 连点版本号 7 次）
- [ ] 启用 **USB 调试**
- [ ] USB 连接电脑，授权调试

---

## 二、项目结构

```
ReversePromptTranslator/
├── app/
│   ├── src/main/
│   │   ├── java/com/reversetool/prompttranslator/
│   │   │   └── MainActivity.java          # WebView 主入口
│   │   ├── assets/
│   │   │   └── index.html                 # PWA 资源（~955KB）
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   │   ├── ic_launcher_background.xml
│   │   │   │   └── ic_launcher_foreground.xml
│   │   │   ├── mipmap-anydpi-v26/
│   │   │   │   └── ic_launcher.xml        # 自适应图标
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       └── styles.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle                        # App 级配置
│   └── proguard-rules.pro
├── build.gradle                            # 项目级配置
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
└── gradlew.bat                             # Windows 构建脚本
```

---

## 三、核心代码

### 3.1 MainActivity.java

```java
package com.reversetool.prompttranslator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // 隐藏系统栏（沉浸式）
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
        // 创建 WebView
        webView = new WebView(this);
        setContentView(webView);
        
        // 配置 WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setDefaultTextEncodingName("UTF-8");
        
        // WebViewClient：所有链接在应用内打开
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient());
        
        // 加载本地 HTML
        webView.loadUrl("file:///android_asset/index.html");
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 返回键处理
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
```

### 3.2 app/build.gradle

```groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.reversetool.prompttranslator'
    compileSdk 34

    defaultConfig {
        applicationId "com.reversetool.prompttranslator"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.webkit:webkit:1.8.0'
}
```

### 3.3 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.reversetool.prompttranslator">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="逆向 Prompt 翻译器"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## 四、构建流程

### 4.1 命令行构建

```powershell
# 进入项目目录
cd C:\Users\Administrator\Documents\Default Project\ReversePromptTranslator

# 构建 Debug APK
& "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.4-bin\...\gradle.bat" assembleDebug

# 或使用 gradlew（如果 gradle-wrapper.jar 正确）
.\gradlew.bat assembleDebug
```

### 4.2 安装到手机

```powershell
# 安装 APK
adb install app\build\outputs\apk\debug\app-debug.apk

# 强制重新安装
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 卸载
adb uninstall com.reversetool.prompttranslator
```

### 4.3 构建产物

| 文件 | 大小 | 说明 |
|------|------|------|
| `app-debug.apk` | ~3.4MB | Debug 版本（已签名） |
| `app-release.apk` | ~2-3MB | Release 版本（需签名） |

---

## 五、遇到的问题与解决方案

### 5.1 Gradle Wrapper 缺失

**问题**: 项目没有 `gradlew.bat` 和 `gradle-wrapper.jar`

**解决方案**:
1. 手动创建 `gradlew.bat`（从其他项目复制或编写）
2. 从 `.gradle` 缓存中找到 `gradle-wrapper.jar`
3. 或直接使用系统 Gradle：`C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.4-bin\...\bin\gradle.bat`

**经验**: 新建 Android 项目时，务必包含完整的 Gradle Wrapper 文件。

### 5.2 PowerShell 命令语法

**问题**: PowerShell 不支持 `&&` 操作符

```powershell
# ❌ 错误
java -version && gradle assembleDebug

# ✅ 正确
java -version; if ($?) { gradle assembleDebug }
```

**解决方案**: 使用分号 `;` 分隔命令，或使用 `cmd /c` 执行

### 5.3 文件路径编码

**问题**: 中文路径导致 PowerShell 报错

**解决方案**: 使用英文路径或将项目放在英文目录下

### 5.4 WebView 本地文件加载

**问题**: `file:///android_asset/` 路径找不到文件

**解决方案**: 
- 确保 `index.html` 在 `app/src/main/assets/` 目录
- 检查 `AndroidManifest.xml` 中的 `usesCleartextTraffic="true"`

### 5.5 图标显示问题

**问题**: 自适应图标在旧设备上不显示

**解决方案**: 
- 创建 `mipmap-anydpi-v26/ic_launcher.xml`
- 同时提供传统 PNG 图标作为后备

---

## 六、经验与教训

### 6.1 环境准备

✅ **正确做法**:
- 先检查 Java、Android SDK、ADB 是否可用
- 确认环境变量设置正确
- 测试 ADB 连接：`adb devices`

❌ **错误做法**:
- 直接开始写代码，遇到问题再排查
- 忽略环境差异

### 6.2 项目结构

✅ **正确做法**:
- 使用标准的 Android 项目结构
- 将资源文件放在正确的位置
- 遵循命名规范

❌ **错误做法**:
- 混淆项目结构
- 硬编码路径

### 6.3 构建配置

✅ **正确做法**:
- 设置合理的 `minSdk`（24 支持 99% 设备）
- 使用 `compileOptions` 指定 Java 版本
- 配置 `proguard-rules.pro`

❌ **错误做法**:
- 使用过低的 `minSdk`
- 忽略 Java 版本兼容性

### 6.4 测试验证

✅ **正确做法**:
- 先构建 Debug 版本测试
- 使用 `adb install` 安装到真机
- 检查应用名称、图标、功能

❌ **错误做法**:
- 直接构建 Release 版本
- 只在模拟器上测试

---

## 七、不会重复犯的错误

### 7.1 Gradle Wrapper 问题

**原因**: 没有完整的 Gradle Wrapper 文件

**预防措施**:
1. 使用 `gradle init` 初始化项目
2. 或从模板项目复制 Wrapper 文件
3. 记录 Gradle 版本要求

### 7.2 命令行语法

**原因**: 混淆 PowerShell 和 Bash 语法

**预防措施**:
1. PowerShell 使用 `;` 分隔命令
2. 使用 `cmd /c` 执行复杂命令
3. 创建构建脚本（`.bat` 或 `.ps1`）

### 7.3 文件路径问题

**原因**: 中文路径、空格、特殊字符

**预防措施**:
1. 项目路径使用英文
2. 避免空格和特殊字符
3. 使用引号包裹路径

### 7.4 签名问题

**原因**: Debug 和 Release 签名混淆

**预防措施**:
1. Debug 自动使用 debug.keystore
2. Release 需要手动配置签名
3. 区分 `assembleDebug` 和 `assembleRelease`

### 7.5 测试遗漏

**原因**: 只在开发环境测试

**预防措施**:
1. 在真机上测试
2. 测试不同 Android 版本
3. 检查应用权限和功能

---

## 八、Agent 工作流模板

### 8.1 任务分解

```yaml
task: PWA → APK 打包
steps:
  - name: 环境检查
    actions:
      - 检查 Java 版本
      - 检查 Android SDK
      - 检查 ADB 连接
    output: 环境报告
    
  - name: 项目结构
    actions:
      - 创建目录结构
      - 配置 Gradle 文件
      - 复制资源文件
    output: 项目骨架
    
  - name: 代码实现
    actions:
      - 编写 MainActivity.java
      - 配置 AndroidManifest.xml
      - 创建资源文件
    output: 源代码
    
  - name: 构建 APK
    actions:
      - 执行 gradle assembleDebug
      - 检查构建产物
      - 验证 APK 内容
    output: app-debug.apk
    
  - name: 安装测试
    actions:
      - adb install
      - 启动应用
      - 验证功能
    output: 测试报告
```

### 8.2 检查清单

```markdown
## 构建前检查
- [ ] Java JDK 17+ 已安装
- [ ] Android SDK 已配置
- [ ] ADB 可用
- [ ] 手机已连接并授权

## 代码检查
- [ ] MainActivity.java 编译通过
- [ ] AndroidManifest.xml 配置正确
- [ ] index.html 已复制到 assets
- [ ] 图标资源已创建

## 构建检查
- [ ] Gradle 构建成功
- [ ] APK 文件已生成
- [ ] APK 大小合理（< 10MB）

## 测试检查
- [ ] 应用可安装
- [ ] 应用可启动
- [ ] 功能正常
- [ ] 离线可用
```

### 8.3 错误处理

| 错误类型 | 可能原因 | 解决方案 |
|---------|---------|---------|
| `JAVA_HOME not set` | 环境变量未配置 | 设置 `JAVA_HOME` 或使用绝对路径 |
| `SDK not found` | Android SDK 路径错误 | 检查 `ANDROID_HOME` 环境变量 |
| `Build failed` | 代码或配置错误 | 检查错误信息，修复后重试 |
| `INSTALL_FAILED` | 签名冲突或设备问题 | 使用 `adb install -r` 或卸载重装 |

---

## 九、扩展：Release 版本签名

### 9.1 生成签名密钥

```powershell
keytool -genkey -v -keystore release.keystore -alias app -keyalg RSA -keysize 2048 -validity 10000
```

### 9.2 配置签名

在 `app/build.gradle` 中添加：

```groovy
android {
    signingConfigs {
        release {
            storeFile file('release.keystore')
            storePassword 'your-password'
            keyAlias 'app'
            keyPassword 'your-password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### 9.3 构建 Release

```powershell
.\gradlew.bat assembleRelease
```

---

## 十、总结

### 成功要素

1. **环境完备**: Java、Android SDK、ADB 都已正确配置
2. **结构清晰**: 标准的 Android 项目结构
3. **代码简洁**: WebView 加载本地 HTML，无复杂逻辑
4. **测试充分**: 真机验证，确保功能正常

### 关键经验

1. **先检查环境**: 不要假设环境已配置好
2. **使用标准结构**: 遵循 Android 项目规范
3. **逐步验证**: 每步都检查结果
4. **记录问题**: 遇到问题及时记录，方便复盘

### 未来改进

1. **自动化脚本**: 创建一键构建脚本
2. **CI/CD 集成**: 使用 GitHub Actions 自动构建
3. **版本管理**: 使用语义化版本号
4. **签名管理**: 使用环境变量存储签名信息

---

**最后更新**: 2026-07-25  
**适用场景**: PWA → Android APK 打包  
**维护者**: Agent Workflow
