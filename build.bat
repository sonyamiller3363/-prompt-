@echo off
REM ============================================
REM 逆向 Prompt 翻译器 - 一键构建脚本
REM ============================================

echo.
echo ========================================
echo  逆向 Prompt 翻译器 APK 构建工具
echo ========================================
echo.

REM 检查 Java
echo [1/5] 检查 Java 环境...
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java 未安装或未配置
    echo 请安装 JDK 17+ 并配置 JAVA_HOME
    pause
    exit /b 1
)
java -version 2>&1 | findstr "version" >nul
echo ✅ Java 已就绪

REM 检查 ADB
echo.
echo [2/5] 检查 ADB 连接...
where adb >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ ADB 未安装
    echo 请安装 Android SDK Platform Tools
    pause
    exit /b 1
)
adb devices | findstr /R "device$" >nul
if %errorlevel% neq 0 (
    echo ⚠️  未检测到设备，请连接手机并启用 USB 调试
    echo 继续构建 APK，稍后手动安装...
) else (
    echo ✅ 设备已连接
)

REM 构建 APK
echo.
echo [3/5] 构建 APK...
echo 使用 Gradle 构建中...

REM 尝试使用 gradlew
if exist "gradlew.bat" (
    call gradlew.bat assembleDebug
) else (
    REM 使用系统 Gradle
    for /f "tokens=*" %%i in ('dir /b /s "C:\Users\Administrator\.gradle\wrapper\dists\gradle-*\*\*\bin\gradle.bat" 2^>nul') do (
        "%%i" assembleDebug
        goto :build_done
    )
    echo ❌ 未找到 Gradle，请手动构建
    pause
    exit /b 1
)

:build_done
if %errorlevel% neq 0 (
    echo ❌ 构建失败
    pause
    exit /b 1
)
echo ✅ 构建成功

REM 检查 APK
echo.
echo [4/5] 检查 APK 文件...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    for %%A in (app\build\outputs\apk\debug\app-debug.apk) do (
        echo ✅ APK 已生成: %%~fA
        echo    大小: %%~zA 字节
    )
) else (
    echo ❌ APK 文件未找到
    pause
    exit /b 1
)

REM 安装到手机
echo.
echo [5/5] 安装到手机...
adb devices | findstr /R "device$" >nul
if %errorlevel% equ 0 (
    echo 正在安装...
    adb install -r app\build\outputs\apk\debug\app-debug.apk
    if %errorlevel% equ 0 (
        echo ✅ 安装成功！
        echo.
        echo 请在手机上找到"逆向 Prompt 翻译器"图标启动应用
    ) else (
        echo ❌ 安装失败
        echo 请手动安装: adb install app\build\outputs\apk\debug\app-debug.apk
    )
) else (
    echo ⚠️  未检测到设备，跳过自动安装
    echo 请手动安装: adb install app\build\outputs\apk\debug\app-debug.apk
)

echo.
echo ========================================
echo  构建完成！
echo ========================================
echo.
pause
