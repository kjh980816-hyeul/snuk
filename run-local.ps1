# 로컬 백엔드 실행 헬퍼 — 루트 .env 의 시크릿을 환경변수로 주입 후 bootRun.
# 사용: .\run-local.ps1  (프론트는 별도로 frontend 에서 npm run dev)
$root = $PSScriptRoot
$envFile = Join-Path $root '.env'
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
            $name = $Matches[1].Trim()
            $value = $Matches[2].Trim()
            Set-Item -Path "env:$name" -Value $value
            Write-Host "[env] $name 주입" -ForegroundColor DarkGray
        }
    }
} else {
    Write-Host "[warn] .env 없음 — 더미 키로 실행됨(OAuth 실연동 불가)" -ForegroundColor Yellow
}
Set-Location (Join-Path $root 'backend')
.\gradlew.bat bootRun
