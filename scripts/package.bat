@echo off
setlocal
cd /d "%~dp0.."
if exist ResolveIT_Submission.zip del /q ResolveIT_Submission.zip
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.IO.Compression; Add-Type -AssemblyName System.IO.Compression.FileSystem; $root=(Get-Location).Path; $zip=Join-Path $root 'ResolveIT_Submission.zip'; $include=@('backend','frontend','docker-compose.yml','.env.example','.gitignore','README.md','start-resolveit.bat','scripts','docs'); $files=foreach($item in $include){$p=Join-Path $root $item; if(Test-Path -LiteralPath $p -PathType Leaf){Get-Item -LiteralPath $p}else{Get-ChildItem -LiteralPath $p -File -Recurse}}; $archive=[System.IO.Compression.ZipFile]::Open($zip,[System.IO.Compression.ZipArchiveMode]::Create); try{foreach($f in $files){$rel=$f.FullName.Substring($root.Length+1).Replace([char]92,[char]47); if($rel -match '(^|/)(target|node_modules|dist|\.git|\.pnpm-store|data)(/|$)' -or $rel -match '(^|/)resolveit-data[^/]*$' -or $f.Name -eq '.env' -or $f.Extension -eq '.log' -or $f.Name -match '\.db$'){continue}; [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$f.FullName,$rel,[System.IO.Compression.CompressionLevel]::Optimal) | Out-Null}}finally{$archive.Dispose()}"
if errorlevel 1 exit /b 1
echo Created ResolveIT_Submission.zip
