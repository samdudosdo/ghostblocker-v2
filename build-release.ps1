[CmdletBinding()]
param(
    [switch]$Install,
    [switch]$Uninstall,
    [switch]$Launch,
    [switch]$Clean,
    [switch]$SkipSdkCheck
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$AppId = 'com.ghostblocker'
$GradleVersion = '8.11.1'
$GradleDir = Join-Path $Root '.tools\gradle'
$GradleHome = Join-Path $GradleDir "gradle-$GradleVersion"
$GradleZip = Join-Path $GradleDir "gradle-$GradleVersion-bin.zip"
$LocalProperties = Join-Path $Root 'local.properties'

function Info($m) { Write-Host "[GhostBlocker] $m" -ForegroundColor Cyan }
function Ok($m) { Write-Host "[OK] $m" -ForegroundColor Green }
function Warn($m) { Write-Host "[WARN] $m" -ForegroundColor Yellow }
function Fail($m) { throw "[GhostBlocker] $m" }

Set-Location $Root

if ($Clean) {
    Info 'Cleaning build outputs...'
    if (Test-Path (Join-Path $Root 'app\build')) { Remove-Item -Recurse -Force (Join-Path $Root 'app\build') }
    if (Test-Path (Join-Path $Root 'release')) {
        Get-ChildItem (Join-Path $Root 'release') -File -ErrorAction SilentlyContinue | Remove-Item -Force
    }
}

# Find Java 17+.
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) { Fail 'Java was not found. Install JDK 17+ and make sure java.exe is on PATH.' }
$javaVersion = (& java -version 2>&1 | Select-Object -First 1)
Info "Java: $javaVersion"

# Find Android SDK.
$sdk = $env:ANDROID_SDK_ROOT
if ([string]::IsNullOrWhiteSpace($sdk)) { $sdk = $env:ANDROID_HOME }
if ([string]::IsNullOrWhiteSpace($sdk)) {
    $candidate = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $candidate) { $sdk = $candidate }
}
if ([string]::IsNullOrWhiteSpace($sdk) -or -not (Test-Path $sdk)) {
    if (-not $SkipSdkCheck) {
        Fail 'Android SDK not found. Install Android Studio/SDK and set ANDROID_SDK_ROOT, or rerun with -SkipSdkCheck if Gradle can resolve it another way.'
    }
} else {
    Set-Content -Path $LocalProperties -Value ("sdk.dir=" + ($sdk -replace '\\','\\')) -Encoding ASCII
    Ok "Android SDK: $sdk"

    if (-not $SkipSdkCheck) {
        $sdkmanager = Join-Path $sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
        if (-not (Test-Path $sdkmanager)) {
            $sdkmanager = Join-Path $sdk 'tools\bin\sdkmanager.bat'
        }
        if (Test-Path $sdkmanager) {
            Info 'Checking required Android SDK packages...'
            & $sdkmanager --licenses | Out-Null
            & $sdkmanager 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0' | Out-Host
        } else {
            Warn 'sdkmanager.bat was not found; assuming Android SDK packages are already installed.'
        }
    }
}

# Bootstrap a standalone Gradle distribution when gradle/gradlew is unavailable.
$gradleExe = Get-Command gradle -ErrorAction SilentlyContinue
if ($gradleExe) {
    $GradleCmd = $gradleExe.Source
} else {
    $candidateExe = Join-Path $GradleHome 'bin\gradle.bat'
    if (-not (Test-Path $candidateExe)) {
        New-Item -ItemType Directory -Force -Path $GradleDir | Out-Null
        Info "Downloading Gradle $GradleVersion..."
        Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $GradleZip
        Expand-Archive -Path $GradleZip -DestinationPath $GradleDir -Force
    }
    $GradleCmd = Join-Path $GradleHome 'bin\gradle.bat'
}
if (-not (Test-Path $GradleCmd)) { Fail "Gradle executable not found: $GradleCmd" }
Ok "Gradle: $GradleCmd"

Info 'Building GhostBlocker debug APK...'
& $GradleCmd --no-daemon --stacktrace ':app:assembleDebug'
if ($LASTEXITCODE -ne 0) { Fail 'Gradle build failed.' }

$apk = Join-Path $Root 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { Fail "APK was not produced: $apk" }

New-Item -ItemType Directory -Force -Path (Join-Path $Root 'release') | Out-Null
$outApk = Join-Path $Root 'release\GhostBlocker.apk'
Copy-Item $apk $outApk -Force
Ok "APK: $outApk"

$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    if ($sdk -and (Test-Path (Join-Path $sdk 'platform-tools\adb.exe'))) {
        $adb = Get-Item (Join-Path $sdk 'platform-tools\adb.exe')
    }
}

if ($Install -or $Uninstall -or $Launch) {
    if (-not $adb) { Fail 'ADB was requested but adb.exe was not found.' }
    & $adb.Source start-server | Out-Host
}

if ($Uninstall) {
    Info "Uninstalling $AppId..."
    & $adb.Source uninstall $AppId | Out-Host
}

if ($Install) {
    Info "Installing $outApk..."
    & $adb.Source install -r $outApk | Out-Host
    if ($LASTEXITCODE -ne 0) { Fail 'ADB install failed.' }
    Ok 'Installed.'
}

if ($Launch) {
    Info 'Launching GhostBlocker...'
    & $adb.Source shell am start -n "$AppId/.MainActivity" | Out-Host
    if ($LASTEXITCODE -ne 0) { Fail 'Could not launch GhostBlocker.' }
}

Write-Host ''
Ok 'Done.'
Write-Host "APK : $outApk"
Write-Host "App : $AppId"
Write-Host ''
Write-Host 'Examples:' -ForegroundColor White
Write-Host '  .\build-release.ps1'
Write-Host '  .\build-release.ps1 -Install -Launch'
Write-Host '  .\build-release.ps1 -Clean -Install -Launch'
