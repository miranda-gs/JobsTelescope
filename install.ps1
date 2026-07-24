#!/usr/bin/env pwsh
param(
  [string]$InstallDir = "$env:USERPROFILE\JobsTelescope"
)

$RepoUrl = "https://github.com/miranda-gs/JobsTelescope.git"
$ReleasesUrl = "https://github.com/miranda-gs/JobsTelescope/releases"
$JarVersion = "0.0.1-SNAPSHOT"
$JarFile = "JobsTelescope-${JarVersion}.jar"
$ErrorActionPreference = "Stop"

function Write-Log   { Write-Host "==> $($args[0])" -ForegroundColor Green }
function Write-Warn  { Write-Host "==> $($args[0])" -ForegroundColor Yellow }
function Write-Info  { Write-Host "  -> $($args[0])" -ForegroundColor Cyan }
function Write-Error { Write-Host "==> $($args[0])" -ForegroundColor Red }

function Test-Command {
  param([string]$Command)
  return [bool](Get-Command $Command -ErrorAction SilentlyContinue)
}

function Find-PackageManager {
  if (Test-Command "winget") { return "winget" }
  if (Test-Command "choco") { return "choco" }
  return $null
}

function Check-Java {
  if (-not (Test-Command "java")) { return $false }

  $raw = java -version 2>&1
  if ($raw -match "GraalVM") { Write-Info "GraalVM detected" }

  $version = $raw | Select-String "version" | ForEach-Object { $_ -replace '\D', '' }
  $major = [int]$version.Substring(0, [Math]::Min(2, $version.Length))

  if ($major -lt 25) {
    Write-Info "Java version $major is too old. Need Java 25+ (GraalVM recommended)."
    return $false
  }

  Write-Log "Java $major found (JRE is sufficient)"
  return $true
}

function Install-GraalVM {
  Write-Log "Installing GraalVM JDK 25 (includes JRE)..."

  $arch = if ([Environment]::Is64BitOperatingSystem) { "x64" } else { "x86" }
  $url = "https://download.oracle.com/graalvm/25/latest/graalvm-jdk-25_windows-${arch}_bin.zip"
  $zip = "$env:TEMP\graalvm.zip"
  $graalDir = "$env:USERPROFILE\graalvm-jdk-25"

  Write-Info "Downloading GraalVM 25..."
  Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing

  Write-Info "Extracting..."
  Expand-Archive -Path $zip -DestinationPath $graalDir -Force

  $javaBin = Get-ChildItem -Path $graalDir -Recurse -Filter "java.exe" | Select-Object -First 1
  if (-not $javaBin) {
    Write-Error "GraalVM extraction failed."
    exit 1
  }

  $javaHome = $javaBin.Directory.Parent.FullName

  [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "User")
  [Environment]::SetEnvironmentVariable("Path", "$javaHome\bin;$env:Path", "User")
  $env:JAVA_HOME = $javaHome
  $env:Path = "$javaHome\bin;$env:Path"

  & "$javaHome\bin\java.exe" -version 2>&1 | Select-Object -First 1

  Write-Log "GraalVM 25 installed and added to PATH"
}

function Install-Java {
  if (Check-Java) { return }

  Write-Log "Installing Java 25+ JRE..."
  $pm = Find-PackageManager

  if ($pm -eq "winget") {
    winget install --id EclipseAdoptium.Temurin.21.JRE -e --source winget --accept-package-agreements
  } elseif ($pm -eq "choco") {
    choco install temurin21jre -y
  } else {
    Install-GraalVM
  }

  if (-not (Check-Java)) {
    Install-GraalVM
  }

  Write-Log "Java JRE installed successfully"
}

function Install-Node {
  if (Test-Command "node") {
    $version = node -v
    $major = [int]($version -replace 'v','' -replace '\..*','')
    if ($major -ge 18) {
      Write-Log "Node.js $version found"
      return
    }
    Write-Info "Node.js version too old. Need 18+."
  }

  Write-Log "Installing Node.js 22..."
  $pm = Find-PackageManager

  if ($pm -eq "winget") {
    winget install --id OpenJS.NodeJS.LTS -e --source winget --accept-package-agreements
  } elseif ($pm -eq "choco") {
    choco install nodejs-lts -y
  } else {
    $url = "https://nodejs.org/dist/v22.14.0/node-v22.14.0-x64.msi"
    $msi = "$env:TEMP\node-install.msi"
    Invoke-WebRequest -Uri $url -OutFile $msi
    Start-Process -Wait -FilePath "msiexec.exe" -ArgumentList "/i `"$msi`" /quiet"
  }

  $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
  if (-not (Test-Command "node")) {
    Write-Error "Node.js installation failed. Install manually: https://nodejs.org/"
    exit 1
  }
  Write-Log "Node.js installed"
}

function Install-Git {
  if (Test-Command "git") {
    Write-Log "Git found"
    return
  }

  Write-Log "Installing Git..."
  $pm = Find-PackageManager

  if ($pm -eq "winget") {
    winget install --id Git.Git -e --source winget --accept-package-agreements
  } elseif ($pm -eq "choco") {
    choco install git -y
  } else {
    $url = "https://github.com/git-for-windows/git/releases/download/v2.45.0.windows.1/Git-2.45.0-64-bit.exe"
    $exe = "$env:TEMP\git-install.exe"
    Invoke-WebRequest -Uri $url -OutFile $exe
    Start-Process -Wait -FilePath $exe -ArgumentList "/SILENT"
  }

  $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
  if (-not (Test-Command "git")) {
    Write-Error "Git installation failed. Install manually: https://git-scm.com/"
    exit 1
  }
  Write-Log "Git installed"
}

function Download-Jar {
  $jarDir = "$InstallDir\core\target"
  $jarPath = "$jarDir\$JarFile"

  New-Item -ItemType Directory -Path $jarDir -Force | Out-Null

  if (Test-Path $jarPath) {
    Write-Log "JAR already exists at $jarPath"
    return
  }

  $jarUrl = "$ReleasesUrl/download/v${JarVersion}/${JarFile}"
  Write-Log "Downloading pre-built JAR from GitHub releases..."

  try {
    Invoke-WebRequest -Uri $jarUrl -OutFile $jarPath -UseBasicParsing
    Write-Log "JAR downloaded successfully"
  } catch {
    Remove-Item $jarPath -ErrorAction SilentlyContinue
    Write-Warn "Pre-built JAR not found at $jarUrl"
    Write-Warn "The release may not be published yet."
    Write-Info "Build the backend yourself with JDK:"
    Write-Info "  cd $InstallDir && .\mvnw.cmd package -DskipTests"
    Write-Info "Or place your JAR at: $jarPath"
    exit 1
  }
}

function Setup-Repo {
  if (Test-Path "$InstallDir\ui\package.json") {
    Write-Log "Repository already exists at $InstallDir"
    Set-Location $InstallDir
    return
  }

  Write-Log "Cloning repository..."
  git clone $RepoUrl $InstallDir
  Set-Location $InstallDir
}

function Install-UIDeps {
  Write-Log "Installing UI dependencies..."
  Set-Location "$InstallDir\ui"
  npm install
  if ($LASTEXITCODE -ne 0) {
    Write-Error "UI dependencies installation failed"
    exit 1
  }
  Write-Log "UI dependencies installed"
}

function Show-Completion {
  Write-Log "Installation complete!"
  Write-Host ""
  Write-Info "To run Jobs Telescope:"
  Write-Info "  cd $InstallDir\ui && npm start"
  Write-Host ""
  Write-Info "Or with a custom JAR path:"
  Write-Info "  cd $InstallDir\ui && npm start -- C:\path\to\jar"
  Write-Host ""
  Write-Info "Required: Java 25+ JRE (any distribution)"
  Write-Info "Optional for building from source: JDK 25+ with javac"
}

function Main {
  Write-Host ""
  Write-Info "Jobs Telescope - Cross-Platform Installer (Windows)"
  Write-Info "====================================================="
  Write-Host ""

  Install-Git
  Install-Java
  Install-Node

  Setup-Repo
  Download-Jar
  Install-UIDeps

  Show-Completion
}

Main
