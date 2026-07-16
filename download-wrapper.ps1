# 下载 Gradle Wrapper JAR（解决 gradlew.bat 找不到主类的问题）
$wrapperUrl = "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
$wrapperJar = "$PSScriptRoot\gradle\wrapper\gradle-wrapper.jar"

Write-Host "正在下载 gradle-wrapper.jar..." -ForegroundColor Cyan

# 创建目录
$wrapperDir = Split-Path $wrapperJar -Parent
if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
}

try {
    Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar -UseBasicParsing -TimeoutSec 60
    Write-Host "下载成功: $wrapperJar" -ForegroundColor Green
} catch {
    Write-Host "GitHub 下载失败，尝试国内镜像..." -ForegroundColor Yellow
    $mirrorUrl = "https://mirrors.cloud.tencent.com/gradle/v8.7.0/gradle-8.7-bin.zip"
    try {
        # 如果 wrapper jar 下载不了，先下载完整 gradle zip
        $zipFile = "$PSScriptRoot\gradle-8.7-bin.zip"
        Invoke-WebRequest -Uri $mirrorUrl -OutFile $zipFile -UseBasicParsing -TimeoutSec 120
        Write-Host "Gradle 分发包下载成功，请解压后配置 PATH 环境变量使用 'gradle' 命令" -ForegroundColor Green
    } catch {
        Write-Host "下载失败，请手动安装 Gradle 或打开 IDEA 让其自动处理" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "按任意键继续..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")