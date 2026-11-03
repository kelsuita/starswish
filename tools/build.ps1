param([switch]$SkipLint)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (!(Test-Path $sdk)) { throw 'Android SDK not found. Set ANDROID_HOME or open the project in Android Studio.' }
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
Push-Location $projectRoot
try {
    $tasks = @('testDebugUnitTest', 'assembleDebug')
    if (!$SkipLint) { $tasks += 'lintDebug' }
    & .\gradlew.bat @tasks
    if ($LASTEXITCODE -ne 0) { throw 'Build or checks failed.' }
    Write-Output "APK: $projectRoot\app\build\outputs\apk\debug\app-debug.apk"
} finally { Pop-Location }
