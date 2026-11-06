param([string]$NodePath)
$ErrorActionPreference='Stop'
$project=$PSScriptRoot
$tools=Join-Path $project '.tools'
$helper=Join-Path $project 'checks\emulator-device.js'
$preview=Join-Path $project 'portable-preview\StagesGraphs-preview.apk'
if(!$NodePath){
    $nodeCommand=Get-Command node -ErrorAction SilentlyContinue
    if($nodeCommand){$NodePath=$nodeCommand.Source}
    else{$NodePath=Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'}
}
if(!(Test-Path -LiteralPath $NodePath)){throw 'Pass -NodePath with the full path to node.exe.'}
if(!(Test-Path -LiteralPath $preview)){throw 'Build the emulator preview with build-portable.ps1 -Preview first.'}
$env:TEMP=Join-Path $tools 'temp';$env:TMP=$env:TEMP
function Device([string[]]$DeviceArgs){
    $output=& $NodePath $helper @DeviceArgs
    if($LASTEXITCODE -ne 0){throw "Emulator command failed: $($DeviceArgs -join ' ')"}
    return $output
}
function DevicePortOpen {
    $socket=New-Object Net.Sockets.TcpClient
    try{return $socket.ConnectAsync('127.0.0.1',5557).Wait(1000) -and $socket.Connected}
    catch{return $false}
    finally{$socket.Dispose()}
}
if(!(DevicePortOpen)){
    Write-Output 'Checking Windows hardware acceleration...'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$project\run-emulator.ps1" -CheckAcceleration
    if($LASTEXITCODE -ne 0){throw 'Hardware acceleration is unavailable in this process. Check Windows Hypervisor Platform and restart Windows.'}
    Write-Output 'Opening the Android 5.1 emulator...'
    Start-Process -FilePath powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$project\run-emulator.ps1`"") -WindowStyle Hidden -RedirectStandardOutput "$project\emulator-desktop.log" -RedirectStandardError "$project\emulator-desktop-errors.log" | Out-Null
}
Write-Output 'Waiting for Android to boot...'
$deadline=[DateTime]::UtcNow.AddMinutes(5)
$ready=$false
while([DateTime]::UtcNow -lt $deadline){
    if(DevicePortOpen){
        $boot=Device @('shell','getprop','sys.boot_completed')
        if(($boot -join '').Trim() -eq '1'){$ready=$true;break}
    }
    Start-Sleep -Seconds 2
}
if(!$ready){throw "Android did not finish booting. See $project\emulator-desktop.log"}
Write-Output 'Installing the emulator-only preview...'
Device @('push',$preview,'/data/local/tmp/stagesgraphs-preview.apk')
$install=Device @('shell','pm','install','-r','/data/local/tmp/stagesgraphs-preview.apk')
Write-Output $install
if(($install -join '') -notmatch 'Success'){throw 'APK installation failed.'}
Device @('shell','am','force-stop','com.stagesgraphs')
Device @('shell','am','instrument','com.stagesgraphs/com.stagesgraphs.EmulatorPreview')
Start-Sleep -Seconds 3
$screenshot=Join-Path (Split-Path (Split-Path $project -Parent) -Parent) 'emulator-preview.png'
Device @('capture',$screenshot)
$bytes=[IO.File]::ReadAllBytes($screenshot)
if($bytes.Length -lt 8 -or $bytes[0] -ne 137 -or $bytes[1] -ne 80){throw 'Screenshot capture failed.'}
Write-Output "Preview ready. Screenshot: $screenshot"
