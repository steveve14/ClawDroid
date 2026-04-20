$root = Get-Location
$appJava = "app\src\main\java"

$modules = @("core\ai","core\data","core\model","core\ui","feature\channels","feature\chat","feature\settings","feature\tools","feature\voice")
$total = 0

foreach ($m in $modules) {
    $src = Join-Path $root "$m\src\main\java"
    if (Test-Path $src) {
        $files = Get-ChildItem -Path $src -Recurse -File -Filter "*.java"
        foreach ($f in $files) {
            $rel = $f.FullName.Substring((Get-Item $src).FullName.Length + 1)
            $dest = Join-Path (Get-Item $appJava).FullName $rel
            $destDir = Split-Path $dest -Parent
            if (!(Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
            
            $srcBytes = [System.IO.File]::ReadAllBytes($f.FullName)
            $startIdx = 0
            if ($srcBytes.Length -ge 3 -and $srcBytes[0] -eq 0xEF -and $srcBytes[1] -eq 0xBB -and $srcBytes[2] -eq 0xBF) { $startIdx = 3 }
            
            $content = [System.Text.Encoding]::UTF8.GetString($srcBytes, $startIdx, $srcBytes.Length - $startIdx)
            $content = $content -replace 'import com\.clawdroid\.feature\.[a-z]+\.databinding\.', 'import com.clawdroid.app.databinding.'
            $content = $content -replace 'import com\.clawdroid\.core\.[a-z]+\.databinding\.', 'import com.clawdroid.app.databinding.'
            $content = [regex]::Replace($content, 'import com\.clawdroid\.feature\.\w+\.R;', 'import com.clawdroid.app.R;')
            $content = [regex]::Replace($content, 'import com\.clawdroid\.core\.\w+\.R;', 'import com.clawdroid.app.R;')
            
            $destBytes = [System.Text.Encoding]::UTF8.GetBytes($content)
            [System.IO.File]::WriteAllBytes($dest, $destBytes)
            $total++
        }
        Write-Host "Done: $m"
    }
}
Write-Host "Total: $total files copied"

$checkPath = "app\src\main\java\com\clawdroid\feature\chat\ui\ChatFragment.java"
if (Test-Path $checkPath) {
    $check = [System.IO.File]::ReadAllBytes((Get-Item $checkPath).FullName)
    Write-Host "ChatFragment first bytes: $($check[0..9] | ForEach-Object { '{0:X2}' -f $_ })"
    Write-Host "ChatFragment first chars: $([System.Text.Encoding]::UTF8.GetString($check, 0, [Math]::Min(30, $check.Length)))"
}
