param(
    [Parameter(Mandatory = $false)]
    [string]$LanIp
)

$ErrorActionPreference = 'Stop'

$webRoot = Split-Path -Parent $PSScriptRoot
$toolDirectory = Join-Path $webRoot '.tools'
$localMkcert = Join-Path $toolDirectory 'mkcert.exe'
$systemMkcert = Get-Command mkcert -ErrorAction SilentlyContinue

if ($null -ne $systemMkcert) {
    $mkcertCommand = $systemMkcert.Source
} elseif (Test-Path -LiteralPath $localMkcert) {
    $mkcertCommand = $localMkcert
} else {
    $architecture = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString()
    $assetName = switch ($architecture) {
        'X64' { 'mkcert-v1.4.4-windows-amd64.exe' }
        'Arm64' { 'mkcert-v1.4.4-windows-arm64.exe' }
        default { throw "Unsupported Windows architecture: $architecture. Install mkcert manually from its official GitHub release." }
    }
    $downloadUrl = "https://github.com/FiloSottile/mkcert/releases/download/v1.4.4/$assetName"

    New-Item -ItemType Directory -Force -Path $toolDirectory | Out-Null
    Write-Host "System mkcert was not found. Downloading official release: $assetName"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $downloadUrl -OutFile $localMkcert -UseBasicParsing
    $mkcertCommand = $localMkcert
}

Write-Host "Using mkcert: $mkcertCommand"

if ([string]::IsNullOrWhiteSpace($LanIp)) {
    $ipconfigText = ipconfig | Out-String
    $matches = [regex]::Matches(
        $ipconfigText,
        '(?im)IPv4[^:]*:\s*(?<ip>(?!127\.)(?!169\.254\.)\d{1,3}(?:\.\d{1,3}){3})'
    )
    $LanIp = $matches | ForEach-Object { $_.Groups['ip'].Value } | Select-Object -First 1
}

if ([string]::IsNullOrWhiteSpace($LanIp)) {
    throw 'Unable to detect a LAN IPv4 address. Run: pnpm https:setup -- -LanIp 192.168.x.x'
}

$certificateDirectory = Join-Path $webRoot '.certs'
$certificatePath = Join-Path $certificateDirectory 'dev-cert.pem'
$privateKeyPath = Join-Path $certificateDirectory 'dev-key.pem'

New-Item -ItemType Directory -Force -Path $certificateDirectory | Out-Null

Write-Host 'Installing or verifying the local development CA. Accept the Windows trust prompt if shown.'
& $mkcertCommand -install
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to install the mkcert local CA.'
}

Write-Host "Generating a certificate for localhost and LAN address $LanIp."
& $mkcertCommand `
    -cert-file $certificatePath `
    -key-file $privateKeyPath `
    localhost 127.0.0.1 ::1 $LanIp
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to generate the development certificate.'
}

$caRoot = & $mkcertCommand -CAROOT
Write-Host ''
Write-Host 'HTTPS development certificate created:'
Write-Host "  Certificate: $certificatePath"
Write-Host "  Private key: $privateKeyPath"
Write-Host '  Local URL: https://localhost:5173'
Write-Host "  LAN URL: https://${LanIp}:5173"
Write-Host ''
Write-Host 'For another LAN computer, import this CA certificate into Trusted Root Certification Authorities:'
Write-Host "  $(Join-Path $caRoot 'rootCA.pem')"
Write-Warning 'Never copy or disclose rootCA-key.pem or dev-key.pem.'
