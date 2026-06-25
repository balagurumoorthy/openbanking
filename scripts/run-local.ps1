# Run the platform locally in JVM mode on Windows (no minikube/Docker).
# Opens each Quarkus service in its own PowerShell window. Stop by closing the windows.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$priv = Join-Path $root "certs\jwt-signing-pkcs8.key"
$pub  = Join-Path $root "certs\jwt-signing.pub"

if (-not (Test-Path $pub)) {
    Write-Host "==> Generating PKI via Git Bash (gen-certs.sh)..."
    & bash ./scripts/gen-certs.sh
}

function Start-Service($module, $envs) {
    $setEnv = ($envs.GetEnumerator() | ForEach-Object { "`$env:$($_.Key)='$($_.Value)'" }) -join "; "
    $cmd = "$setEnv; Set-Location '$root\$module'; mvn quarkus:dev"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $cmd
    Write-Host "==> started $module"
}

Start-Service "consent-auth" @{ OB_JWT_PRIVATE_KEY=$priv; OB_JWT_PUBLIC_KEY=$pub; OB_ISSUER_BASE="http://localhost:8081" }
Start-Service "bala-bank"    @{ OB_JWT_PUBLIC_KEY=$pub }
Start-Service "mohana-tpp"   @{ AUTH_BASE="http://localhost:8081"; AUTH_PUBLIC_BASE="http://localhost:8081"; GATEWAY_BASE="http://localhost:8082"; TPP_REDIRECT_URI="http://localhost:8080/callback" }

Write-Host ""
Write-Host "Three windows launched. Once each shows 'Listening on', open:" -ForegroundColor Green
Write-Host "  http://localhost:8080  (MohanaTPP -> click 'Connect your Bala Bank account')"
