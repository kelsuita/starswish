param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$TestRoot = "$env:LOCALAPPDATA\Temp\opencode\star-savings-emulator"
)
$ErrorActionPreference = 'Stop'
$image = Join-Path $Sdk 'system-images\android-35\google_apis_ps16k\x86_64'
if (!(Test-Path (Join-Path $image 'system.img'))) { throw 'Missing Android 35 emulator image' }
$avdHome = Join-Path $TestRoot 'avd'
$avd = Join-Path $avdHome 'StarSavingsTest.avd'
New-Item -ItemType Directory -Force -Path $avd | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText((Join-Path $avdHome 'StarSavingsTest.ini'), "avd.ini.encoding=UTF-8`npath=$avd`ntarget=android-35`n", $utf8)
$config = @"
avd.ini.encoding=UTF-8
AvdId=StarSavingsTest
avd.ini.displayname=Star Savings Test
hw.cpu.arch=x86_64
hw.cpu.ncore=2
hw.ramSize=2048
hw.lcd.width=1080
hw.lcd.height=2400
hw.lcd.density=420
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader
hw.keyboard=yes
hw.mainKeys=no
hw.initialOrientation=portrait
disk.dataPartition.size=2G
image.sysdir.1=$image
tag.id=google_apis_ps16k
abi.type=x86_64
PlayStore.enabled=false
showDeviceFrame=no
"@
[IO.File]::WriteAllText((Join-Path $avd 'config.ini'), $config, $utf8)
$env:ANDROID_AVD_HOME = $avdHome
$env:ANDROID_SDK_ROOT = $Sdk
$process = Start-Process (Join-Path $Sdk 'emulator\emulator.exe') -ArgumentList @('-avd','StarSavingsTest','-no-window','-no-audio','-no-snapshot','-no-boot-anim','-gpu','swiftshader','-port','5580') -PassThru -RedirectStandardOutput (Join-Path $TestRoot 'stdout.log') -RedirectStandardError (Join-Path $TestRoot 'stderr.log')
Write-Output "Started test emulator PID $($process.Id), logs: $TestRoot"
