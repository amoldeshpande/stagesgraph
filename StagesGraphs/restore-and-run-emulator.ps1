$ErrorActionPreference = 'Stop'
$tools = Join-Path $PSScriptRoot '.tools'
New-Item -ItemType Directory -Force -Path $tools | Out-Null

function Restore-EmulatorPackage {
    param([string]$Url, [string]$Archive, [string]$Destination, [string]$RequiredFile)
    if (Test-Path -LiteralPath $RequiredFile) { return }
    if (!(Test-Path -LiteralPath $Archive)) {
        Write-Host "Downloading $([IO.Path]::GetFileName($Archive))"
        $partial = "$Archive.partial"
        & curl.exe -fL --retry 2 --output $partial $Url
        if ($LASTEXITCODE -ne 0) { throw "Download failed for $Archive" }
        Move-Item -LiteralPath $partial -Destination $Archive -Force
    }
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    & tar.exe -xf $Archive -C $Destination
    if ($LASTEXITCODE -ne 0 -or !(Test-Path -LiteralPath $RequiredFile)) {
        throw "Could not extract $Archive"
    }
}

Restore-EmulatorPackage 'https://dl.google.com/android/repository/emulator-windows_x64-16766954.zip' (Join-Path $tools 'emulator.zip') (Join-Path $tools 'emulator-manual') (Join-Path $tools 'emulator-manual\emulator\emulator.exe')
Restore-EmulatorPackage 'https://dl.google.com/android/repository/sys-img/android/x86_64-22_r06.zip' (Join-Path $tools 'image22.zip') (Join-Path $tools 'image22') (Join-Path $tools 'image22\x86_64\system.img')

& (Join-Path $PSScriptRoot 'run-emulator.ps1')
