param([Parameter(Mandatory=$true)][string]$NotesPath)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $project
$configuration = Get-Content -LiteralPath (Join-Path $project 'app\build.gradle.kts') -Raw
$version = [regex]::Match($configuration, 'versionName\s*=\s*"(\d+\.\d+\.\d+)"').Groups[1].Value
if ([string]::IsNullOrWhiteSpace($version)) { throw 'A stable semantic version is required' }
$apk = Join-Path $project "dist\今日紫微-v$version.apk"
if (!(Test-Path -LiteralPath $apk)) { throw 'Build the APK first with build.ps1' }
$notes = (Resolve-Path -LiteralPath $NotesPath).Path
$metadata = Get-Content -LiteralPath (Join-Path $project 'app\build\outputs\apk\release\output-metadata.json') -Raw | ConvertFrom-Json
if ($metadata.elements[0].versionName -ne $version) { throw 'APK metadata version does not match source' }
$builtApk = Join-Path $project "app\build\outputs\apk\release\今日紫微-v$version.apk"
if ((Get-FileHash -LiteralPath $apk).Hash -ne (Get-FileHash -LiteralPath $builtApk).Hash) { throw 'Dist APK does not match the build output' }
$releaseApk = Join-Path $project "dist\ZiweiToday-v$version.apk"
Copy-Item -LiteralPath $apk -Destination $releaseApk
$targetCommit = (& git rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve the release source commit' }
& gh release create "v$version" $releaseApk --target $targetCommit --repo 'littleblackmann/ziwei-today' --title "今日紫微 v$version" --notes-file $notes
if ($LASTEXITCODE -ne 0) { throw 'Release publication failed' }
