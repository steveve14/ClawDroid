$bytes = [System.IO.File]::ReadAllBytes("app\src\main\java\com\clawdroid\feature\chat\ui\ChatFragment.java")
Write-Host "First 10 bytes: $($bytes[0..9] | ForEach-Object { '{0:X2}' -f $_ })"
Write-Host "File length: $($bytes.Length)"

$raw = [System.IO.File]::ReadAllText("app\src\main\java\com\clawdroid\feature\chat\ui\ChatFragment.java")
$lines = $raw -split "`n"
$lines[0..4] | ForEach-Object { Write-Host "LINE: $_" }

$bytes2 = [System.IO.File]::ReadAllBytes("app\src\main\java\com\clawdroid\app\di\AiModule.java")
Write-Host "AiModule first bytes: $($bytes2[0..9] | ForEach-Object { '{0:X2}' -f $_ })"
$raw2 = [System.IO.File]::ReadAllText("app\src\main\java\com\clawdroid\app\di\AiModule.java")
($raw2 -split "`n")[0..3] | ForEach-Object { Write-Host "LINE: $_" }
