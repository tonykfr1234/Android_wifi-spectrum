$ErrorActionPreference = 'Stop'
try {
    Set-Location (Split-Path $PSScriptRoot -Parent)
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Install JDK 17 and Android Studio / Android SDK 36 first. See INSTALL.zh-TW.md.' }
    & .\gradlew.bat :app:assembleDebug
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. Fix the Gradle error above; nothing was installed.' }
    $apk = Join-Path (Get-Location) 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $apk)) { throw 'Build did not produce app-debug.apk.' }
    $adb = $null
    foreach ($sdk in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "$env:LOCALAPPDATA\Android\Sdk")) {
        if ($sdk -and (Test-Path -LiteralPath (Join-Path $sdk 'platform-tools\adb.exe'))) {
            $adb = Join-Path $sdk 'platform-tools\adb.exe'
            break
        }
    }
    if (-not $adb) {
        $command = Get-Command adb -ErrorAction SilentlyContinue
        if ($command) { $adb = $command.Source }
    }
    if (-not $adb) { throw "APK ready at $apk. Install Android SDK Platform-Tools and set ANDROID_HOME, or copy this APK to your phone." }
    $listing = & $adb devices
    if ($LASTEXITCODE -ne 0) { throw 'ADB failed. Check the USB connection and SDK Platform-Tools.' }
    $devices = @($listing | ForEach-Object { if ($_ -match '^(\S+)\s+device\s*$') { $Matches[1] } })
    if ($devices.Count -ne 1) { throw "APK ready at $apk. Connect exactly ONE device, enable USB debugging and approve the phone prompt. Disconnect other devices/emulators, then run INSTALL.bat again." }
    Write-Host "Installing on device: $($devices[0])"
    & $adb -s $devices[0] install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK installation failed. Read the ADB error; no automatic uninstall was attempted.' }
    Write-Host 'Installed. Open the app on your phone and grant precise location / nearby Wi-Fi permissions.'
    exit 0
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
