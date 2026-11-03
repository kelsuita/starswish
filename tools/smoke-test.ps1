param(
    [string]$Serial = 'emulator-5580',
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$Output = "$env:LOCALAPPDATA\Temp\opencode\star-savings-smoke"
)
$ErrorActionPreference = 'Stop'
# This script clears only this application's data on our isolated test emulator.
if ($Serial -notin @('emulator-5580', 'emulator-5582')) { throw 'This test is restricted to the isolated savings emulators.' }
$adb = Join-Path $Sdk 'platform-tools\adb.exe'
New-Item -ItemType Directory -Force -Path $Output | Out-Null
function Adb([string[]]$Arguments) {
    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $result = & $adb -s $Serial @Arguments 2>&1
    $ErrorActionPreference = $oldPreference
    if ($LASTEXITCODE -ne 0) { throw ($result -join "`n") }
    return $result
}
function Read-Ui {
    Adb @('shell', 'uiautomator', 'dump', '/sdcard/star-test.xml') | Out-Null
    Adb @('pull', '/sdcard/star-test.xml', (Join-Path $Output 'ui.xml')) | Out-Null
    [xml]$doc = [IO.File]::ReadAllText((Join-Path $Output 'ui.xml'), [Text.Encoding]::UTF8)
    return $doc
}
function Tap-Node($node) {
    if (!$node) { throw 'UI node not found' }
    if ($node.bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') { throw 'Invalid bounds' }
    $x = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
    $y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
    Adb @('shell', 'input', 'tap', "$x", "$y") | Out-Null
}
function Tap-Text([string]$text) {
    $doc = Read-Ui
    $textNodes = @($doc.SelectNodes('//node') | Where-Object { $_.text -eq $text })
    # Navigation labels can also occur in page content. Prefer the fixed bottom bar.
    $node = if ($text -in @('首页', '账户', '设置')) {
        $textNodes | Sort-Object { if ($_.bounds -match '\[\d+,(\d+)\]') { [int]$Matches[1] } else { 0 } } | Select-Object -Last 1
    } else { $textNodes | Select-Object -First 1 }
    if (!$node) { throw "Cannot find text: $text" }
    Tap-Node $node
}
function Tap-Description([string]$description) {
    $doc = Read-Ui
    $node = $doc.SelectNodes('//node') | Where-Object { $_.'content-desc' -eq $description } | Select-Object -First 1
    if (!$node) { throw "Cannot find content description: $description" }
    Tap-Node $node
}
function Fill-Field([int]$index, [string]$text) {
    $doc = Read-Ui
    $nodes = @($doc.SelectNodes('//node') | Where-Object { $_.class -eq 'android.widget.EditText' })
    if ($index -ge $nodes.Count) { throw "Field $index not found" }
    Tap-Node $nodes[$index]
    Adb @('shell', 'input', 'text', $text) | Out-Null
    Adb @('shell', 'input', 'keyevent', '4') | Out-Null
}
function Assert-Text([string]$text) {
    $doc = Read-Ui
    if (!($doc.SelectNodes('//node') | Where-Object { $_.text -eq $text })) { throw "Expected UI text: $text" }
    Write-Output "PASS: $text"
}
function Screenshot([string]$name) {
    Adb @('shell', 'screencap', '-p', '/sdcard/star-test.png') | Out-Null
    Adb @('pull', '/sdcard/star-test.png', (Join-Path $Output "$name.png")) | Out-Null
}

$avdName = (Adb @('shell', 'getprop', 'ro.boot.qemu.avd_name') | Out-String).Trim()
if ($avdName -notin @('StarSavingsTest', 'StarSavingsUiTest')) { throw "Unexpected emulator: $avdName" }
$apk = Join-Path (Split-Path $PSScriptRoot -Parent) 'app\build\outputs\apk\debug\app-debug.apk'
Adb @('install', '-r', $apk) | Out-Null
    Adb @('shell','pm','clear','com.kelsuita.starswish') | Out-Null
    Adb @('shell','am','start','-W','-n','com.kelsuita.starswish/com.starsavings.app.MainActivity') | Out-Null
Assert-Text '第一个心愿，从这里开始'
Screenshot '01-empty'
Tap-Text '账户'
Assert-Text '我的账户'
Adb @('shell', 'input', 'swipe', '180', '1100', '900', '1100', '350') | Out-Null
Assert-Text '目标列表'
Adb @('shell', 'input', 'swipe', '900', '1100', '180', '1100', '350') | Out-Null
Assert-Text '我的账户'
Tap-Text '＋ 添加账户'
Fill-Field 0 'TestBank'
Fill-Field 1 '12500.50'
Tap-Text '保存账户'
Assert-Text 'TestBank'
Tap-Text '首页'
Tap-Text '＋ 新建目标'
Fill-Field 0 'AuroraFund'
Fill-Field 1 '25000'
Tap-Text '开启这个心愿'
Assert-Text '请选择至少一个账户'
Tap-Text 'TestBank'
Tap-Text '开启这个心愿'
Assert-Text 'AuroraFund'
Assert-Text '50%'
Assert-Text '¥ 12,500.50'
Screenshot '02-goal-progress'
    Adb @('shell','am','force-stop','com.kelsuita.starswish') | Out-Null
    Adb @('shell','am','start','-W','-n','com.kelsuita.starswish/com.starsavings.app.MainActivity') | Out-Null
Assert-Text 'AuroraFund'
Assert-Text '50%'
Write-Output 'PASS: goal and balance persist across process restart'
Tap-Text '账户'
Tap-Text '设置读取权限'
Assert-Text '读取小星记账余额'
Assert-Text '暂不开启'
Screenshot '03-permission-disclosure'
Tap-Text '暂不开启'
Tap-Text '读取小星余额'
Assert-Text '读取小星记账余额'
Tap-Text '暂不开启'
Write-Output 'PASS: reader requires explicit permission'
Tap-Text '设置'
Assert-Text '关于'
Assert-Text '外观'
Assert-Text '首页'
Assert-Text '应用'
Screenshot '06-settings'
Tap-Text '关于'
Assert-Text '星愿攒钱'
Assert-Text '使用步骤'
Screenshot '05-about'
Adb @('shell', 'input', 'keyevent', '4') | Out-Null
Assert-Text '外观'
Assert-Text '关于'
Tap-Text '关于'
Tap-Description '返回设置'
Assert-Text '外观'
Adb @('shell', 'input', 'keyevent', '4') | Out-Null
Assert-Text '目标列表'
Write-Output 'PASS: About page returns to Settings, then Home'
Tap-Text '设置'
Tap-Text '关于'
Adb @('shell', 'input', 'swipe', '2', '1000', '700', '1000', '650') | Out-Null
Assert-Text '外观'
Write-Output 'PASS: edge back gesture returns About to Settings'
Tap-Text '主题模式'
Assert-Text '跟随系统'
Assert-Text '深色'
Adb @('shell', 'input', 'keyevent', '4') | Out-Null
Assert-Text '外观'
Write-Output 'PASS: MIUIX theme dropdown opens and dismisses'
Adb @('shell', 'cmd', 'uimode', 'night', 'yes') | Out-Null
Read-Ui | Out-Null
Tap-Text '首页'
Assert-Text 'AuroraFund'
Screenshot '04-dark-mode'
Adb @('shell', 'cmd', 'uimode', 'night', 'no') | Out-Null
Write-Output "Smoke tests passed. Screenshots: $Output"
