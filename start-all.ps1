# start-all.ps1
# Runs setup checks, backend/frontend tests, then launches the Java backend,
# Node.js backend, and React frontend from a single command.
# Requires: Java 11+, Maven 3.6+, Node.js 16+, npm

$ErrorActionPreference = 'Stop'
$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$JavaBackendUrl = 'http://localhost:8080'
$NodeBackendUrl = 'http://localhost:3000'
$FrontendUrl = 'http://localhost:5173'
$SwaggerUiUrl = "$JavaBackendUrl/swagger-ui/index.html"
$SwaggerDocsUrl = "$JavaBackendUrl/v3/api-docs"

function Write-Heading([string]$text) {
    Write-Host "`n=== $text ===" -ForegroundColor Cyan
}

function Fail([string]$message) {
    Write-Host "ERROR: $message" -ForegroundColor Red
    Exit 1
}

function Extract-Version([string]$text) {
    if ($text -match '(\d+\.\d+(?:\.\d+)?)') {
        return $Matches[1]
    }
    return $null
}

function Compare-Version([string]$actual, [string]$minimum) {
    $actualParts = $actual -split '[^\d]+' | Where-Object { $_ -ne '' } | ForEach-Object { [int]$_ }
    $minParts = $minimum -split '[^\d]+' | Where-Object { $_ -ne '' } | ForEach-Object { [int]$_ }
    $length = [Math]::Max($actualParts.Count, $minParts.Count)
    for ($i = 0; $i -lt $length; $i++) {
        $a = if ($i -lt $actualParts.Count) { $actualParts[$i] } else { 0 }
        $m = if ($i -lt $minParts.Count) { $minParts[$i] } else { 0 }
        if ($a -lt $m) { return $false }
        if ($a -gt $m) { return $true }
    }
    return $true
}

function Check-VersionRequirement([string]$name, [string]$command, [string[]]$arguments, [string]$minVersion, [string]$pattern) {
    Write-Host "Checking $name..."
    $cmdInfo = Get-Command $command -ErrorAction SilentlyContinue
    if (-not $cmdInfo) {
        if (Test-Path $command) {
            $cmdInfo = New-Object PSObject
            $cmdInfo | Add-Member -MemberType NoteProperty -Name Path -Value $command
        } else {
            Fail "$name is not installed or not available on PATH. Please install $name and try again."
        }
    }

    $stdoutFile = [System.IO.Path]::GetTempFileName()
    $stderrFile = [System.IO.Path]::GetTempFileName()
    try {
        $process = Start-Process -FilePath $cmdInfo.Path -ArgumentList $arguments -RedirectStandardOutput $stdoutFile -RedirectStandardError $stderrFile -Wait -PassThru
        Start-Sleep -Milliseconds 100
        $output = @()
        if (Test-Path $stdoutFile) { $output += Get-Content $stdoutFile -ErrorAction SilentlyContinue }
        if (Test-Path $stderrFile) { $output += Get-Content $stderrFile -ErrorAction SilentlyContinue }
    } catch {
        Remove-Item $stdoutFile, $stderrFile -ErrorAction SilentlyContinue
        Fail "$name execution failed. Please check your installation."
    }

    Remove-Item $stdoutFile, $stderrFile -ErrorAction SilentlyContinue
    $version = Extract-Version($output -join ' ')
    if (-not $version) {
        Fail "Could not detect $name version from command output."
    }

    if (-not (Compare-Version $version $minVersion)) {
        Fail "$name version $version is less than required $minVersion. Please upgrade $name."
    }

    Write-Host "$name version $version is installed." -ForegroundColor Green
}

function Install-Dependencies([string]$projectDir) {
    Write-Host "Installing npm dependencies in $projectDir..."
    Push-Location $projectDir
    try {
        npm install
    } catch {
        Fail "npm install failed in $projectDir"
    } finally {
        Pop-Location
    }
}

function Get-ExecutablePath([string]$commandName) {
    $cmdInfo = Get-Command $commandName -ErrorAction SilentlyContinue
    if ($cmdInfo -and $cmdInfo.Path) {
        return $cmdInfo.Path
    }

    if (Test-Path $commandName) {
        return (Resolve-Path $commandName).Path
    }

    Fail "$commandName is not installed or not available on PATH."
}

function Quote-PowerShell([string]$text) {
    return "'" + ($text -replace "'", "''") + "'"
}

function Test-PortOpen([int]$port) {
    $connection = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    return $null -ne $connection
}

function Stop-PortProcess([int]$port) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if (-not $connections) {
        return
    }

    $processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $processIds) {
        try {
            $process = Get-Process -Id $processId -ErrorAction Stop
            Write-Host "Stopping existing $($process.ProcessName) process on port $port (PID $processId)..." -ForegroundColor Yellow
            Stop-Process -Id $processId -Force
        } catch {
            Write-Host "Could not stop process on port $port (PID $processId)." -ForegroundColor Yellow
        }
    }
}

function Wait-For-Port([int]$port, [int]$timeoutSeconds = 60) {
    Write-Host "Waiting for port $port to become available..."
    $start = Get-Date
    while ((Get-Date) - $start -lt [TimeSpan]::FromSeconds($timeoutSeconds)) {
        if (Test-PortOpen $port) {
            Write-Host "Port $port is listening." -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 2
    }
    Write-Host "Port $port did not become available within $timeoutSeconds seconds." -ForegroundColor Yellow
    return $false
}

function Run-Command([string]$title, [string]$workingDirectory, [scriptblock]$action) {
    Write-Host "Running $title..."
    Push-Location $workingDirectory
    try {
        & $action
    } catch {
        Fail "$title failed in $workingDirectory"
    } finally {
        Pop-Location
    }
}

function Start-ServiceProcess([string]$title, [string]$workingDirectory, [string]$command, [string[]]$serviceArguments, [string]$logPrefix) {
    $logsDirectory = Join-Path $ScriptRoot 'logs'
    if (-not (Test-Path $logsDirectory)) {
        New-Item -ItemType Directory -Path $logsDirectory | Out-Null
    }

    $stdoutLog = Join-Path $logsDirectory "$logPrefix.out.log"
    $stderrLog = Join-Path $logsDirectory "$logPrefix.err.log"
    $executablePath = Get-ExecutablePath $command
    $argumentText = ($serviceArguments | ForEach-Object { Quote-PowerShell $_ }) -join ' '
    $launcherScriptPath = Join-Path $logsDirectory "$logPrefix.launcher.ps1"
    $launcherScript = @"
Set-Location -LiteralPath $(Quote-PowerShell $workingDirectory)
& $(Quote-PowerShell $executablePath) $argumentText
"@
    Set-Content -Path $launcherScriptPath -Value $launcherScript -Encoding UTF8
    Write-Host "Starting $title..." -ForegroundColor Cyan
    Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $launcherScriptPath) -WindowStyle Hidden -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog
    Write-Host "Logging to $stdoutLog and $stderrLog"
}

function Wait-For-Url([string]$url, [int]$timeoutSeconds = 60) {
    Write-Host "Waiting for $url to become available..."
    $start = Get-Date
    while ((Get-Date) - $start -lt [TimeSpan]::FromSeconds($timeoutSeconds)) {
        try {
            $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                Write-Host "$url is reachable." -ForegroundColor Green
                return $true
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    Write-Host "$url did not become available within $timeoutSeconds seconds." -ForegroundColor Yellow
    return $false
}

function Assert-UrlOk([string]$url, [string]$label) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 15
        if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 400) {
            Fail "$label returned unexpected status code $($response.StatusCode) at $url"
        }
        Write-Host "$label is reachable." -ForegroundColor Green
    } catch {
        Fail "$label is not reachable at $url"
    }
}

function Open-Url([string]$url) {
    try {
        Start-Process $url
    } catch {
        Write-Host "Could not auto-open $url. Copy and paste it into your browser." -ForegroundColor Yellow
    }
}

Write-Heading 'Setup'

$MavenCommand = 'mvn'
if (-not (Get-Command $MavenCommand -ErrorAction SilentlyContinue)) {
    $wrapperPath = Join-Path $ScriptRoot 'java-backend\mvnw.cmd'
    if (Test-Path $wrapperPath) {
        Write-Host "Maven not found on PATH, using wrapper at $wrapperPath"
        $MavenCommand = $wrapperPath
    }
}

Check-VersionRequirement 'Java' 'java' @('-version') '11.0' 'version'
Check-VersionRequirement 'Maven' $MavenCommand @('-version') '3.6' 'Apache Maven'
Check-VersionRequirement 'Node.js' 'node' @('-v') '16.0' 'v'

# npm version check - optional, as it should come with node
if (Get-Command 'npm' -ErrorAction SilentlyContinue) {
    Write-Host "Checking npm..."
    Write-Host "npm is available (comes with Node.js)." -ForegroundColor Green
} else {
    Fail "npm is not available. Please install Node.js which includes npm."
}

Install-Dependencies "$ScriptRoot\node-backend"
Install-Dependencies "$ScriptRoot\react-frontend"

Write-Heading 'Tests'
Run-Command 'Java backend tests' "$ScriptRoot\java-backend" {
    & $MavenCommand test
}
Run-Command 'React frontend build' "$ScriptRoot\react-frontend" {
    & npm run build
}

Write-Heading 'Start Services'
if (-not (Test-PortOpen 3306)) {
    Write-Host "MySQL is not listening on port 3306. Starting the local database..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $ScriptRoot 'start-mysql.ps1') -WindowStyle Hidden
    Wait-For-Port 3306 60 | Out-Null
}

Stop-PortProcess 8080
Stop-PortProcess 3000
Stop-PortProcess 5173

$env:MYSQL_URL = 'jdbc:mysql://localhost:3306/java_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:MYSQL_USERNAME = 'root'
$env:MYSQL_PASSWORD = if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { 'rootpass' }
$env:MYSQL_DATABASE = 'java_test'

Start-ServiceProcess 'Java backend' "$ScriptRoot\java-backend" $MavenCommand @('spring-boot:run') 'java-backend'
Start-ServiceProcess 'Node.js backend' "$ScriptRoot\node-backend" 'npm' @('start') 'node-backend'
Start-ServiceProcess 'React frontend' "$ScriptRoot\react-frontend" 'npm' @('run', 'dev', '--', '--host', '0.0.0.0') 'react-frontend'

Write-Heading 'Verification'
$javaHealthy = Wait-For-Url "$JavaBackendUrl/health" 90
$nodeHealthy = Wait-For-Url "$NodeBackendUrl/health" 90
$frontendHealthy = Wait-For-Url $FrontendUrl 90
$swaggerUiReady = Wait-For-Url $SwaggerUiUrl 90
$swaggerDocsReady = Wait-For-Url $SwaggerDocsUrl 90

if ($javaHealthy -and $nodeHealthy -and $frontendHealthy -and $swaggerUiReady -and $swaggerDocsReady) {
    Write-Host "All services are running and verified." -ForegroundColor Green
    Assert-UrlOk "$JavaBackendUrl/health" 'Java backend health'
    Assert-UrlOk "$NodeBackendUrl/health" 'Node backend health'
    Assert-UrlOk "$FrontendUrl" 'React frontend'
    Assert-UrlOk $SwaggerUiUrl 'Swagger UI'
    Assert-UrlOk $SwaggerDocsUrl 'Swagger docs'
    Write-Host "Java backend: $JavaBackendUrl/health"
    Write-Host "Node backend: $NodeBackendUrl/health"
    Write-Host "React frontend: $FrontendUrl"
    Write-Host "Swagger UI: $SwaggerUiUrl"
    Write-Host "Swagger docs: $SwaggerDocsUrl"
    Open-Url $FrontendUrl
    Open-Url $SwaggerUiUrl
} else {
    Write-Host "One or more services failed to start or verify." -ForegroundColor Red
    Write-Host "Check the terminal windows for logs and errors."
    Write-Host "You can still try these links manually:"
    Write-Host "React frontend: $FrontendUrl"
    Write-Host "Swagger UI: $SwaggerUiUrl"
    Write-Host "Swagger docs: $SwaggerDocsUrl"
}

Write-Host "`nIf you need to stop the services, close the separate terminal windows opened by this script." -ForegroundColor Cyan
