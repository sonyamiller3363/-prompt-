# GitHub 发布指南

## 步骤 1: 创建 GitHub 仓库

### 方式 A: 在 GitHub 网站创建（推荐）

1. 打开 https://github.com/new
2. 填写仓库信息：
   - **Repository name**: `reverse-prompt-translator`
   - **Description**: `逆向 Prompt 翻译器 - PWA 转 APK，支持离线使用的逆向工程提示词工具`
   - **Public**: 选择 Public（免费 GitHub Actions）
   - **Initialize**: 不要勾选任何选项（已有代码）
3. 点击 **Create repository**

### 方式 B: 使用 GitHub CLI（如果已安装）

```bash
gh repo create reverse-prompt-translator --public --description "逆向 Prompt 翻译器"
```

## 步骤 2: 添加远程仓库并推送

创建仓库后，在终端执行：

```bash
cd "C:\Users\Administrator\Documents\Default Project\ReversePromptTranslator"

# 添加远程仓库（替换 your-username 为你的 GitHub 用户名）
git remote add origin https://github.com/your-username/reverse-prompt-translator.git

# 推送代码
git branch -M main
git push -u origin main
```

## 步骤 3: 配置 GitHub Secrets

### 3.1 编码签名文件

在 PowerShell 中执行以下命令，获取 Base64 编码的签名文件：

```powershell
# 编码 keystore 文件
$keystorePath = "C:\Users\Administrator\Documents\Default Project\ReversePromptTranslator\release.keystore"
$keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
Write-Host "RELEASE_STORE_FILE:" -ForegroundColor Green
Write-Host $keystoreBase64
```

### 3.2 在 GitHub 配置 Secrets

1. 打开你的仓库页面
2. 点击 **Settings** 标签
3. 在左侧菜单找到 **Secrets and variables** → **Actions**
4. 点击 **New repository secret**
5. 添加以下 4 个 Secrets：

| Secret 名称 | 值 |
|------------|---|
| `RELEASE_STORE_FILE` | 上面命令输出的 Base64 字符串 |
| `RELEASE_STORE_PASSWORD` | `reverse123` |
| `RELEASE_KEY_ALIAS` | `reverse-prompt` |
| `RELEASE_KEY_PASSWORD` | `reverse123` |

### 3.3 更新 GitHub Actions 工作流

在配置 Secrets 之前，需要先更新工作流文件以正确处理 Base64 编码的 keystore：

打开 `.github/workflows/build.yml`，修改 Release 构建步骤：

```yaml
    - name: Decode Keystore
      run: echo "${{ secrets.RELEASE_STORE_FILE }}" | base64 -d > release.keystore

    - name: Build Release APK
      run: ./gradlew assembleRelease
      env:
        RELEASE_STORE_FILE: release.keystore
        RELEASE_STORE_PASSWORD: ${{ secrets.RELEASE_STORE_PASSWORD }}
        RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }}
        RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
```

## 步骤 4: 创建并推送 v1.0.0 Tag

```bash
cd "C:\Users\Administrator\Documents\Default Project\ReversePromptTranslator"

# 创建 tag
git tag -a v1.0.0 -m "v1.0.0: 首次发布

功能特性:
- PWA 转 APK (WebView 嵌入)
- Kotlin + Android
- Release 签名配置
- ProGuard/R8 优化
- 应用内自动更新
- GitHub Actions CI/CD
- 158 个内嵌资源
- 完全离线可用"

# 推送 tag
git push origin v1.0.0
```

## 步骤 5: 验证 GitHub Actions

1. 打开你的仓库页面
2. 点击 **Actions** 标签
3. 应该能看到正在运行的构建任务
4. 构建完成后，在 **Releases** 页面可以看到 v1.0.0 Release

## 步骤 6: 下载测试

1. 打开 https://github.com/your-username/reverse-prompt-translator/releases
2. 下载 `app-release.apk`
3. 安装到手机测试

---

## 快速命令（一键执行）

如果你已经创建了 GitHub 仓库，可以执行以下脚本：

```powershell
# 在 PowerShell 中执行
cd "C:\Users\Administrator\Documents\Default Project\ReversePromptTranslator"

# 替换为你的 GitHub 用户名
$GITHUB_USERNAME = "your-username"

# 添加远程仓库
git remote add origin "https://github.com/$GITHUB_USERNAME/reverse-prompt-translator.git"

# 推送代码
git branch -M main
git push -u origin main

# 创建并推送 tag
git tag -a v1.0.0 -m "v1.0.0: 首次发布"
git push origin v1.0.0
```

---

## 常见问题

### Q: 推送时提示 "remote: Repository not found"
A: 确认仓库名和用户名正确，检查是否已创建仓库。

### Q: 推送时提示 "permission denied"
A: 需要配置 GitHub 认证（SSH key 或 Personal Access Token）。

### Q: GitHub Actions 构建失败
A: 检查 Secrets 是否正确配置，查看 Actions 日志排查问题。

### Q: 如何获取 Personal Access Token？
1. 打开 https://github.com/settings/tokens
2. 点击 **Generate new token**
3. 选择 **repo** 权限
4. 复制 token 使用

---

**最后更新**: 2026-07-26
