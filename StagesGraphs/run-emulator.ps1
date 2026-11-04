param([switch]$SoftwareCpu)
$ErrorActionPreference='Stop'
$localTools=Join-Path $PSScriptRoot '.tools'
$env:ANDROID_SDK_ROOT=Join-Path $localTools 'sdk'
$env:ANDROID_HOME=$env:ANDROID_SDK_ROOT
$env:ANDROID_USER_HOME=Join-Path $localTools 'android-home'
$env:ANDROID_EMULATOR_HOME=Join-Path $localTools 'emulator-home'
$env:ANDROID_AVD_HOME=Join-Path $localTools 'avd'
$env:HOME=Join-Path $localTools 'user-home'
$env:USERPROFILE=$env:HOME
$env:ANDROID_SDK_HOME=$env:HOME
$env:TEMP=Join-Path $localTools 'temp'
$env:TMP=$env:TEMP
$env:LOCALAPPDATA=Join-Path $localTools 'emulator-user\local'
$env:APPDATA=Join-Path $localTools 'emulator-user\roaming'
$env:ANDROID_ADB_SERVER_PORT='5038'
New-Item -ItemType Directory -Force -Path $env:ANDROID_EMULATOR_HOME,$env:ANDROID_AVD_HOME,$env:LOCALAPPDATA,$env:APPDATA | Out-Null
$avd=Join-Path $env:ANDROID_AVD_HOME 'StagesApi22.avd'
New-Item -ItemType Directory -Force -Path $avd | Out-Null
$image=Join-Path $localTools 'image22\x86_64'
@"
avd.ini.encoding=UTF-8
path=$avd
target=android-22
"@ | Set-Content -LiteralPath (Join-Path $env:ANDROID_AVD_HOME 'StagesApi22.ini') -Encoding ASCII
@"
AvdId=StagesApi22
avd.ini.encoding=UTF-8
abi.type=x86_64
hw.cpu.arch=x86_64
hw.cpu.ncore=1
hw.ramSize=1024
hw.lcd.width=1280
hw.lcd.height=800
hw.lcd.density=160
hw.keyboard=yes
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader_indirect
hw.mainKeys=no
disk.dataPartition.size=512M
image.sysdir.1=$image\
tag.id=default
tag.display=Default
PlayStore.enabled=false
vm.heapSize=128
showDeviceFrame=no
skin.dynamic=yes
"@ | Set-Content -LiteralPath (Join-Path $avd 'config.ini') -Encoding ASCII
$emulator=Join-Path $localTools 'emulator-manual\emulator\emulator.exe'
if(!(Test-Path -LiteralPath $emulator)){throw 'Extract .tools/emulator.zip into .tools/emulator-manual first.'}
if(!(Test-Path -LiteralPath "$image\system.img")){throw 'Extract .tools/image22.zip into .tools/image22 first.'}
$arguments=@('-avd','StagesApi22','-no-window','-no-audio','-no-snapshot','-no-boot-anim','-no-metrics','-show-kernel','-gpu','swiftshader_indirect','-feature','-Vulkan','-port','5556')
if($SoftwareCpu){$env:ANDROID_I_WANT_MY_TCG='yes';$arguments+=@('-accel','off')}
& $emulator @arguments
if($LASTEXITCODE -ne 0){throw "Emulator exited with code $LASTEXITCODE"}
