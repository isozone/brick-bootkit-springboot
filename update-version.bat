@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

set "POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT="

for /f "delims=" %%i in ('git rev-parse --show-toplevel 2^>nul') do set "PROJECT_ROOT=%%i"
if defined PROJECT_ROOT set "PROJECT_ROOT=%PROJECT_ROOT:/=\%"

if not defined PROJECT_ROOT (
    if exist "%SCRIPT_DIR%pom.xml" (
        set "PROJECT_ROOT=%SCRIPT_DIR%"
    ) else (
        set "PROJECT_ROOT=%CD%"
    )
)

if "%~1"=="" goto :Usage
if not "%~2"=="" goto :Usage
set "NEW_VERSION=%~1"

call :ValidateVersion "%NEW_VERSION%"
if errorlevel 1 (
    echo Error: invalid version format.
    echo Example: 4.0.0, 4.1.0, 4.0.1-beta
    exit /b 1
)

pushd "%PROJECT_ROOT%" 2>nul
if errorlevel 1 (
    if exist "%SCRIPT_DIR%pom.xml" (
        set "PROJECT_ROOT=%SCRIPT_DIR%"
    ) else (
        set "PROJECT_ROOT=%CD%"
    )
    pushd "%PROJECT_ROOT%" 2>nul
    if errorlevel 1 (
        echo Error: failed to switch to project root: %PROJECT_ROOT%
        exit /b 1
    )
)

echo === Brick BootKit SpringBoot Version Update Tool ===
echo New version: %NEW_VERSION%
echo Project root: %PROJECT_ROOT%
echo.

git rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
    echo Warning: current directory is not a git repository.
)

call :GetFirstVersion "pom.xml" CURRENT_VERSION
if not defined CURRENT_VERSION (
    echo Error: failed to read current version from pom.xml
    popd >nul 2>nul
    exit /b 1
)

echo === Current Version ===
echo Current version: %CURRENT_VERSION%
echo.

for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "TS=%%i"
if not defined TS set "TS=backup"

echo Backing up pom files...
copy /y "pom.xml" "pom.xml.backup.%TS%" >nul
if exist "spring-boot3-brick-bootkit-core\pom.xml" (
    copy /y "spring-boot3-brick-bootkit-core\pom.xml" "spring-boot3-brick-bootkit-core\pom.xml.backup.%TS%" >nul
)
echo Backup complete.
echo.

echo === Update Root pom.xml ===
call :ReplaceInFile "pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
if !ERRORLEVEL! EQU 0 (
    echo Root pom.xml version updated.
) else (
    echo Warning: version %CURRENT_VERSION% not found in root pom.xml
)

echo === Update Module Versions ===
if exist "spring-boot3-brick-bootkit\pom.xml" (
    echo Updating module: spring-boot3-brick-bootkit
    call :ReplaceInFile "spring-boot3-brick-bootkit\pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
    if !ERRORLEVEL! EQU 0 (
        echo   [OK] module version updated
    ) else (
        echo   [WARN] module version %CURRENT_VERSION% not found
    )

    findstr /c:"<artifactId>spring-boot3-brick-bootkit-parent</artifactId>" "spring-boot3-brick-bootkit\pom.xml" >nul
    if not errorlevel 1 (
        call :ReplaceInFile "spring-boot3-brick-bootkit\pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
        if !ERRORLEVEL! EQU 0 (
            echo   [OK] parent version updated
        ) else (
            echo   [WARN] parent version %CURRENT_VERSION% not found
        )
    )
)

for /d %%D in (spring-boot3-brick-bootkit-*) do (
    if exist "%%D\pom.xml" (
        echo Updating module: %%D
        call :ReplaceInFile "%%D\pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
        if !ERRORLEVEL! EQU 0 (
            echo   [OK] module version updated
        ) else (
            echo   [WARN] module version %CURRENT_VERSION% not found
        )

        findstr /c:"<artifactId>spring-boot3-brick-bootkit-parent</artifactId>" "%%D\pom.xml" >nul
        if not errorlevel 1 (
            call :ReplaceInFile "%%D\pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
            if !ERRORLEVEL! EQU 0 (
                echo   [OK] parent version updated
            ) else (
                echo   [WARN] parent version %CURRENT_VERSION% not found
            )
        )
    )
)

echo === Update Other Files ===
if exist "README.md" (
    call :ReplaceInFile "README.md" "%CURRENT_VERSION%" "%NEW_VERSION%"
    if !ERRORLEVEL! EQU 0 echo README.md updated.
)

if exist "doc" (
    for %%F in (doc\*.md) do (
        call :ReplaceInFile "%%F" "%CURRENT_VERSION%" "%NEW_VERSION%"
        if !ERRORLEVEL! EQU 0 echo %%F updated.
    )
)

echo Updating Maven plugin descriptor files...
set "PLUGIN_FILE1=spring-boot3-brick-bootkit-maven-packager\src\main\resources\META-INF\maven\com.gitee.starblues.springboot-plugin-maven-packager\plugin-help.xml"
set "PLUGIN_FILE2=spring-boot3-brick-bootkit-maven-packager\src\main\resources\META-INF\maven\plugin.xml"

call :UpdatePluginFile "%PLUGIN_FILE1%"
call :UpdatePluginFile "%PLUGIN_FILE2%"

echo.
echo === Verify Results ===
call :GetFirstVersion "pom.xml" NEW_ROOT_VERSION
if "%NEW_ROOT_VERSION%"=="%NEW_VERSION%" (
    echo Root pom.xml: [OK] %NEW_VERSION%
) else (
    echo Root pom.xml: [FAIL] expected %NEW_VERSION%, got %NEW_ROOT_VERSION%
)

if exist "spring-boot3-brick-bootkit\pom.xml" (
    call :GetFirstVersion "spring-boot3-brick-bootkit\pom.xml" SPRING_BOOT_KIT_VERSION
    if "%SPRING_BOOT_KIT_VERSION%"=="%NEW_VERSION%" (
        echo   spring-boot3-brick-bootkit: [OK] %SPRING_BOOT_KIT_VERSION%
    ) else (
        echo   spring-boot3-brick-bootkit: [FAIL] %SPRING_BOOT_KIT_VERSION%, expected %NEW_VERSION%
    )
)

echo Checking module versions...
for /d %%D in (spring-boot3-brick-bootkit-*) do (
    if exist "%%D\pom.xml" (
        call :GetFirstVersion "%%D\pom.xml" MODULE_VERSION
        if "!MODULE_VERSION!"=="%NEW_VERSION%" (
            echo   %%D: [OK] !MODULE_VERSION!
        ) else (
            echo   %%D: [FAIL] !MODULE_VERSION!, expected %NEW_VERSION%
        )
    )
)

echo Checking Maven plugin descriptor versions...
call :CheckPluginFile "%PLUGIN_FILE1%"
call :CheckPluginFile "%PLUGIN_FILE2%"

echo.
echo [OK] Version update verification completed.
echo.
echo === Next Steps ===
echo Version update is done. Run one of the following manually:
echo   Compile check: mvn compile -o
echo   Full compile:  mvn clean compile -U
echo   Full package:  mvn clean package

git rev-parse --is-inside-work-tree >nul 2>nul
if not errorlevel 1 (
    echo.
    echo === Git Status ===
    git status --short 2>nul
    echo.
    echo Suggested next commands:
    echo   1. git diff
    echo   2. git add . ^&^& git commit -m "Bump version to %NEW_VERSION%"
    echo   3. git tag v%NEW_VERSION%
)

echo.
echo Backup files:
dir /b "pom.xml.backup.*" 2>nul
if errorlevel 1 echo   ^(none in repository root^)

echo.
echo === Version Update Complete ===
echo Old version: %CURRENT_VERSION%
echo New version: %NEW_VERSION%
popd >nul 2>nul
exit /b 0

:Usage
echo Error: missing version argument.
echo Usage: %~nx0 ^<new-version^>
echo Example: %~nx0 4.1.0
exit /b 1

:ValidateVersion
set "VALUE=%~1"
"%POWERSHELL%" -NoProfile -Command "$v='%VALUE%'; if ($v -match '^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9]+)?$') { exit 0 } else { exit 1 }"
exit /b %ERRORLEVEL%

:ReplaceInFile
set "FILE=%~1"
set "OLD=%~2"
set "NEW=%~3"
"%POWERSHELL%" -NoProfile -Command "$path='%FILE%'; if (-not (Test-Path $path)) { exit 1 }; $text=[IO.File]::ReadAllText($path); $pattern=[regex]::Escape('%OLD%'); $updated=[regex]::Replace($text, $pattern, '%NEW%'); if ($updated -eq $text) { exit 2 }; [IO.File]::WriteAllText($path, $updated); exit 0"
exit /b %ERRORLEVEL%

:GetFirstVersion
set "FILE=%~1"
set "OUTVAR=%~2"
set "VALUE="
for /f "usebackq tokens=3 delims=<>" %%i in (`findstr /r /c:"<version>[^<][^<]*</version>" "%FILE%"`) do (
    set "VALUE=%%i"
    goto :GetFirstVersionDone
)
:GetFirstVersionDone
set "%OUTVAR%=%VALUE%"
exit /b 0

:UpdatePluginFile
set "PLUGIN_FILE=%~1"
if exist "%PLUGIN_FILE%" (
    call :ReplaceInFile "%PLUGIN_FILE%" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
    if !ERRORLEVEL! EQU 0 (
        echo   [OK] %PLUGIN_FILE% updated
    ) else (
        echo   [WARN] %PLUGIN_FILE% does not contain %CURRENT_VERSION%
    )
) else (
    echo   [WARN] file not found: %PLUGIN_FILE%
)
exit /b 0

:CheckPluginFile
set "PLUGIN_FILE=%~1"
if exist "%PLUGIN_FILE%" (
    call :GetFirstVersion "%PLUGIN_FILE%" PLUGIN_VERSION
    if "%PLUGIN_VERSION%"=="%NEW_VERSION%" (
        echo   %~nx1: [OK] %PLUGIN_VERSION%
    ) else (
        echo   %~nx1: [FAIL] %PLUGIN_VERSION%, expected %NEW_VERSION%
    )
)
exit /b 0
