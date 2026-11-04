$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$localTools = Join-Path $project '.tools'
New-Item -ItemType Directory -Force -Path $localTools | Out-Null
$env:GRADLE_USER_HOME = Join-Path $localTools 'gradle-home'
$env:ANDROID_USER_HOME = Join-Path $localTools 'android-home'
$env:ANDROID_SDK_ROOT = Join-Path $localTools 'sdk'
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$localJavaHome = Join-Path $localTools 'user-home'
New-Item -ItemType Directory -Force -Path $localJavaHome | Out-Null
$env:JAVA_TOOL_OPTIONS = "-Duser.home=$localJavaHome"
$env:TEMP = Join-Path $localTools 'temp'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force -Path $env:TEMP,$env:ANDROID_USER_HOME,$env:ANDROID_SDK_ROOT | Out-Null
function Fetch($url, $destination) {
    if (!(Test-Path -LiteralPath $destination)) {
        & curl.exe -fL --retry 2 --output $destination $url
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $url" }
    }
}
Fetch 'https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip' (Join-Path $localTools 'jdk.zip')
if (!(Test-Path (Join-Path $localTools 'java'))) {
    Expand-Archive -LiteralPath (Join-Path $localTools 'jdk.zip') -DestinationPath (Join-Path $localTools 'java')
}
$env:JAVA_HOME = (Get-ChildItem (Join-Path $localTools 'java') -Directory | Select-Object -First 1).FullName
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Fetch 'https://services.gradle.org/distributions/gradle-8.7-bin.zip' (Join-Path $localTools 'gradle.zip')
if (!(Test-Path (Join-Path $localTools 'gradle-8.7'))) { Expand-Archive -LiteralPath (Join-Path $localTools 'gradle.zip') -DestinationPath $localTools }
Fetch 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' (Join-Path $localTools 'android.zip')
if (!(Test-Path (Join-Path $localTools 'cmdline-tools'))) { Expand-Archive -LiteralPath (Join-Path $localTools 'android.zip') -DestinationPath $localTools }
$sdkmanager = Join-Path $localTools 'cmdline-tools\bin\sdkmanager.bat'
# Accept the SDK component licenses for this local build installation.
(1..100 | ForEach-Object { 'y' }) | & $sdkmanager "--sdk_root=$env:ANDROID_SDK_ROOT" --licenses
if ($LASTEXITCODE -ne 0) { throw 'SDK license setup failed' }
& $sdkmanager "--sdk_root=$env:ANDROID_SDK_ROOT" 'platforms;android-28' 'build-tools;35.0.0'
if ($LASTEXITCODE -ne 0) { throw 'SDK installation failed' }
& (Join-Path $env:JAVA_HOME 'bin\javac.exe') -encoding UTF-8 -d $env:TEMP (Join-Path $project 'app\src\main\java\com\stagesgraphs\Packets.java') (Join-Path $project 'checks\PacketCheck.java')
if ($LASTEXITCODE -ne 0) { throw 'Parser check compilation failed' }
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -cp $env:TEMP PacketCheck
if ($LASTEXITCODE -ne 0) { throw 'Parser checks failed' }
& (Join-Path $localTools 'gradle-8.7\bin\gradle.bat') -p $project --no-daemon --stacktrace '-Dorg.gradle.java.installations.auto-download=false' assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Output 'Using resource-free portable APK packaging after Gradle build failure.'
    & (Join-Path $project 'build-portable.ps1')
    return
}
Copy-Item -LiteralPath (Join-Path $project 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path (Split-Path $project -Parent) 'StagesGraphs.apk') -Force
Write-Output 'Built StagesGraphs.apk in the output directory.'
