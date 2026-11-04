# Called by build.ps1 when native Windows resource packaging is unavailable.
$ErrorActionPreference='Stop'
$project=$PSScriptRoot
$sdk=$env:ANDROID_SDK_ROOT
$portable=Join-Path $project 'portable-build'
$classes=Join-Path $portable 'classes'
$staging=Join-Path $portable 'apk'
New-Item -ItemType Directory -Force -Path $classes,$staging | Out-Null
$sources=Get-ChildItem -LiteralPath (Join-Path $project 'app\src\main\java\com\stagesgraphs') -Filter '*.java' | ForEach-Object {$_.FullName}
& "$env:JAVA_HOME\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -classpath "$sdk\platforms\android-28\android.jar" -d $classes @sources
if($LASTEXITCODE -ne 0){throw 'Portable Java compilation failed'}
& node "$project\checks\verify-text.js" "$classes\com\stagesgraphs\MainActivity.class"
if($LASTEXITCODE -ne 0){throw 'Compiled text encoding check failed'}
& "$env:JAVA_HOME\bin\jar.exe" cf "$portable\classes.jar" -C $classes .
& "$env:JAVA_HOME\bin\java.exe" -cp "$sdk\build-tools\35.0.0\lib\d8.jar" com.android.tools.r8.D8 --min-api 22 --lib "$sdk\platforms\android-28\android.jar" --output $staging "$portable\classes.jar"
if($LASTEXITCODE -ne 0){throw 'DEX conversion failed'}
& "$env:JAVA_HOME\bin\javap.exe" -constants -classpath "$sdk\platforms\android-28\android.jar" 'android.R$attr' > "$portable\attributes.txt"
& "$env:JAVA_HOME\bin\javap.exe" -constants -classpath "$sdk\platforms\android-28\android.jar" 'android.R$style' > "$portable\styles.txt"
[xml]$xml=Get-Content -LiteralPath "$project\app\src\main\AndroidManifest.xml"
$xml.manifest.SetAttribute('package','com.stagesgraphs')
$ns='http://schemas.android.com/apk/res/android'
$xml.manifest.SetAttribute('versionCode',$ns,'2') | Out-Null
$xml.manifest.SetAttribute('versionName',$ns,'1.1') | Out-Null
$uses=$xml.CreateElement('uses-sdk')
$uses.SetAttribute('minSdkVersion',$ns,'22') | Out-Null
$uses.SetAttribute('targetSdkVersion',$ns,'22') | Out-Null
$xml.manifest.PrependChild($uses) | Out-Null
function Convert-Element($element){
    $attributes=@($element.Attributes | Where-Object {$_.NamespaceURI -ne 'http://www.w3.org/2000/xmlns/'} | ForEach-Object {
        @{name=$_.LocalName;ns=$_.NamespaceURI;value=$_.Value}
    })
    $children=@($element.ChildNodes | Where-Object {$_.NodeType -eq 'Element'} | ForEach-Object {Convert-Element $_})
    return @{name=$element.LocalName;attributes=$attributes;children=$children}
}
Convert-Element $xml.DocumentElement | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath "$portable\manifest.json" -Encoding UTF8
& node "$project\checks\encode-manifest.js" "$portable\manifest.json" "$portable\attributes.txt" "$portable\styles.txt" "$staging\AndroidManifest.xml"
if($LASTEXITCODE -ne 0){throw 'Manifest encoding failed'}
& "$env:JAVA_HOME\bin\jar.exe" cf "$portable\unsigned.apk" -C $staging AndroidManifest.xml -C $staging classes.dex
$key="$project\.tools\android-home\debug.keystore"
if(!(Test-Path -LiteralPath $key)){
    & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -keysize 2048 -validity 10000
    if($LASTEXITCODE -ne 0){throw 'Development key generation failed'}
}
$apk=Join-Path (Split-Path $project -Parent) 'StagesGraphs.apk'
& "$sdk\build-tools\35.0.0\apksigner.bat" sign --ks $key --ks-pass pass:android --key-pass pass:android --min-sdk-version 22 --out $apk "$portable\unsigned.apk"
if($LASTEXITCODE -ne 0){throw 'APK signing failed'}
& "$sdk\build-tools\35.0.0\apksigner.bat" verify --verbose --min-sdk-version 22 $apk
if($LASTEXITCODE -ne 0){throw 'APK signature verification failed'}
$env:APKANALYZER_OPTS="-Dcom.android.sdklib.toolsdir=$sdk\cmdline-tools\latest"
New-Item -ItemType Directory -Force -Path "$sdk\cmdline-tools\latest" | Out-Null
& "$project\.tools\cmdline-tools\bin\apkanalyzer.bat" manifest print $apk > "$portable\verified-manifest.xml"
if($LASTEXITCODE -ne 0){throw 'APK manifest verification failed'}
Write-Output 'Portable APK built and verified.'
