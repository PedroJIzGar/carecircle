param(
    [string] $EnvFile = ".env"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot $EnvFile

if (!(Test-Path $envPath)) {
    throw "Environment file not found: $envPath"
}

Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()

    if (!$line -or $line.StartsWith("#")) {
        return
    }

    $separatorIndex = $line.IndexOf("=")
    if ($separatorIndex -lt 1) {
        return
    }

    $key = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1).Trim()

    if ($value.StartsWith('"') -and $value.EndsWith('"')) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    [Environment]::SetEnvironmentVariable($key, $value, "Process")
}

Push-Location $projectRoot
try {
    .\mvnw.cmd spring-boot:run
}
finally {
    Pop-Location
}
