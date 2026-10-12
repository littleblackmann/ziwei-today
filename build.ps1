param([switch]$Test, [switch]$Install, [switch]$Debug, [string]$Device = 'emulator-5554')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$javaCommand = Get-Command java -ErrorAction Stop
$env:JAVA_HOME = Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
$env:ANDROID_USER_HOME = Join-Path $PSScriptRoot '.android-home'
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-home\user-home'
$localGradle = Join-Path $PSScriptRoot '.gradle-home\gradle-8.11.1\bin\gradle.bat'
$gradle = if (Test-Path -LiteralPath $localGradle) { $localGradle } else { Join-Path $PSScriptRoot 'gradlew.bat' }
$variant = if ($Debug) { 'debug' } else { 'release' }
$tasks = @(if ($Debug) { 'assembleDebug' } else { 'assembleRelease' })
if ($Test) { $tasks += 'writeJvmTestClasspath' }
& $gradle @tasks --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Gradle build or tests failed' }
if ($Test) {
    $classPath = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot 'app\build\jvm-test-classpath.txt'))
    $testClasses = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'app\src\test') -Recurse -Filter '*Test.kt' |
        ForEach-Object {
            $source = [System.IO.File]::ReadAllText($_.FullName)
            $package = [regex]::Match($source, '(?m)^package\s+([\w.]+)').Groups[1].Value
            "$package.$($_.BaseName)"
        }
    & $javaCommand.Source '-Dfile.encoding=UTF-8' '-cp' $classPath 'org.junit.runner.JUnitCore' @testClasses
    if ($LASTEXITCODE -ne 0) { throw 'JVM regression tests failed' }
}
$configuration = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'app\build.gradle.kts') -Raw
$version = [regex]::Match($configuration, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
$apk = Join-Path $PSScriptRoot "app\build\outputs\apk\$variant\今日紫微-v$version.apk"
$dist = Join-Path $PSScriptRoot 'dist'
[System.IO.Directory]::CreateDirectory($dist) | Out-Null
Copy-Item -LiteralPath $apk -Destination (Join-Path $dist "今日紫微-v$version.apk")
Write-Output "APK: $dist\今日紫微-v$version.apk"
if ($Install) {
    $adb = Join-Path $PSScriptRoot '.android-sdk\platform-tools\adb.exe'
    & $adb -s $Device install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'Install failed; existing app data was not cleared' }
    & $adb -s $Device shell am start -n 'com.zwds.today/.MainActivity'
}
