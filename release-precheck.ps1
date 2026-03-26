$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$failed = $false
$warned = $false

function Write-Step($message) {
    Write-Host "== $message ==" -ForegroundColor Cyan
}

function Write-Pass($message) {
    Write-Host "[OK] $message" -ForegroundColor Green
}

function Write-Warn($message) {
    $script:warned = $true
    Write-Host "[WARN] $message" -ForegroundColor Yellow
}

function Write-Fail($message) {
    $script:failed = $true
    Write-Host "[FAIL] $message" -ForegroundColor Red
}

Set-Location $root

Write-Host "Brick BootKit Release Precheck" -ForegroundColor Cyan
Write-Host "Project root: $root"
Write-Host ""

Write-Step "git diff --check"
if (git diff --check) {
    Write-Pass "git diff --check"
}

Write-Step "Tracked workspace noise"
$ideaChanges = git status --short | Where-Object { $_ -match '^\s*[MADRCU?]{1,2}\s+\.idea/' }
if ($ideaChanges) {
    Write-Warn "Detected .idea changes. These are usually local IDE files and should not enter a release commit."
} else {
    Write-Pass "No tracked .idea workspace noise detected"
}

Write-Step "Template presence"
$templateFiles = @(
    'templates/README.md',
    'templates/host-minimal/pom.xml',
    'templates/plugin-minimal/pom.xml',
    'templates/host-broken-main-package/README.md',
    'templates/host-broken-plugin-path/README.md',
    'templates/plugin-broken-packaging/README.md'
)
foreach ($path in $templateFiles) {
    if (-not (Test-Path $path)) {
        Write-Fail "Missing required template file: $path"
    }
}
if (-not $failed) {
    Write-Pass "Template files are present"
}

Write-Step "Key docs presence"
$docFiles = @(
    'readme.md',
    'CONTRIBUTING.md',
    'CODE_OF_CONDUCT.md',
    'SECURITY.md',
    'ROADMAP.md',
    'doc/2.SpringBoot项目快速接入指南.md',
    'doc/3.兼容与支持矩阵.md',
    'doc/4.发布与验收清单.md',
    'doc/updates/4.0.6.md'
)
foreach ($path in $docFiles) {
    if (-not (Test-Path $path)) {
        Write-Fail "Missing required documentation file: $path"
    }
}
if (-not $failed) {
    Write-Pass "Key docs are present"
}

Write-Step "Stable Java tests"
if ($env:JAVA_HOME) {
    try {
        .\mvnw.cmd -B -ntp `
          -pl spring-boot3-brick-bootkit-loader,spring-boot3-brick-bootkit-core,spring-boot3-brick-bootkit `
          -am test
        Write-Pass "Stable Java tests"
    } catch {
        Write-Fail "Stable Java tests"
    }
} else {
    Write-Warn "JAVA_HOME is not set. Skipping Maven test execution."
}

Write-Step "Docs verification"
if (Get-Command npm -ErrorAction SilentlyContinue) {
    if (Test-Path 'docs-website/node_modules') {
        try {
            Push-Location docs-website
            npm run verify-docs
            Pop-Location
            Write-Pass "Docs verification"
        } catch {
            Pop-Location
            Write-Fail "Docs verification"
        }
    } else {
        Write-Warn "docs-website/node_modules is missing. Run 'cd docs-website; npm ci' before release."
    }
} else {
    Write-Warn "npm is not available. Skipping docs verification."
}

Write-Step "Web console front-end build"
if (Get-Command npm -ErrorAction SilentlyContinue) {
    if (Test-Path 'spring-boot3-brick-bootkit-web/vue3/node_modules') {
        try {
            Push-Location 'spring-boot3-brick-bootkit-web/vue3'
            npm run build
            Pop-Location
            Write-Pass "Web console front-end build"
        } catch {
            Pop-Location
            Write-Warn "Front-end build did not complete. Check vue3 dependency state before release."
        }
    } else {
        Write-Warn "spring-boot3-brick-bootkit-web/vue3/node_modules is missing. Run 'cd spring-boot3-brick-bootkit-web/vue3; npm install'."
    }
} else {
    Write-Warn "npm is not available. Skipping web front-end build."
}

Write-Host ""
if ($failed) {
    Write-Host "Release precheck finished with failures." -ForegroundColor Red
    exit 1
}

if ($warned) {
    Write-Host "Release precheck finished with warnings." -ForegroundColor Yellow
    exit 0
}

Write-Host "Release precheck finished successfully." -ForegroundColor Green
