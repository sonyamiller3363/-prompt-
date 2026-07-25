# GitHub Secrets 配置助手
# 运行此脚本获取需要配置的 Secrets 值

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  GitHub Secrets 配置助手" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 keystore 文件
$keystorePath = "C:\Users\Administrator\Documents\Default Project\ReversePromptTranslator\release.keystore"

if (Test-Path $keystorePath) {
    Write-Host "[1/4] 编码 keystore 文件..." -ForegroundColor Yellow
    
    $keystoreBytes = [IO.File]::ReadAllBytes($keystorePath)
    $keystoreBase64 = [Convert]::ToBase64String($keystoreBytes)
    
    Write-Host ""
    Write-Host "RELEASE_STORE_FILE" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Gray
    Write-Host $keystoreBase64
    Write-Host ""
} else {
    Write-Host "❌ 未找到 release.keystore 文件" -ForegroundColor Red
    Write-Host "请先运行: keytool -genkey -v -keystore release.keystore -alias reverse-prompt -keyalg RSA -keysize 2048 -validity 10000" -ForegroundColor Yellow
    exit 1
}

Write-Host "[2/4] 其他 Secrets..." -ForegroundColor Yellow
Write-Host ""

Write-Host "RELEASE_STORE_PASSWORD" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Gray
Write-Host "reverse123"
Write-Host ""

Write-Host "RELEASE_KEY_ALIAS" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Gray
Write-Host "reverse-prompt"
Write-Host ""

Write-Host "RELEASE_KEY_PASSWORD" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Gray
Write-Host "reverse123"
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  配置步骤" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 打开你的 GitHub 仓库页面" -ForegroundColor White
Write-Host "2. 点击 Settings 标签" -ForegroundColor White
Write-Host "3. 左侧菜单: Secrets and variables → Actions" -ForegroundColor White
Write-Host "4. 点击 New repository secret" -ForegroundColor White
Write-Host "5. 依次添加上面的 4 个 Secrets" -ForegroundColor White
Write-Host ""

# 复制到剪贴板选项
$copy = Read-Host "是否复制 RELEASE_STORE_FILE 到剪贴板? (y/n)"
if ($copy -eq "y") {
    $keystoreBase64 | Set-Clipboard
    Write-Host "✅ 已复制到剪贴板" -ForegroundColor Green
}

Write-Host ""
Write-Host "配置完成后，推送代码和 tag 即可触发自动构建:" -ForegroundColor Yellow
Write-Host 'git remote add origin https://github.com/your-username/reverse-prompt-translator.git' -ForegroundColor Gray
Write-Host 'git branch -M main' -ForegroundColor Gray
Write-Host 'git push -u origin main' -ForegroundColor Gray
Write-Host 'git tag -a v1.0.0 -m "v1.0.0: 首次发布"' -ForegroundColor Gray
Write-Host 'git push origin v1.0.0' -ForegroundColor Gray
