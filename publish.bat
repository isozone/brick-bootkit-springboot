@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem publish.bat - Publish Brick BootKit to Maven Central (Sonatype Central Portal).
rem Usage:
rem   publish.bat          Run real release deploy.
rem   publish.bat --check  Validate release profile only; no version change, no deploy.
rem   publish.bat --version 4.1.0   Pin a specific release version.

cd /d "%~dp0"
if errorlevel 1 exit /b 1

set "CHECK_ONLY=0"
set "PIN_VERSION="

:parse
if "%~1"=="" goto parsed
if /I "%~1"=="--check" (
    set "CHECK_ONLY=1"
    shift /1
    goto parse
)
if /I "%~1"=="--version" (
    set "PIN_VERSION=%~2"
    shift /1
    shift /1
    goto parse
)
shift /1
goto parse
:parsed

rem ===== Maven settings / Central account =====
rem CRITICAL: cmd must use "call mvn" - direct external exe call terminates the whole bat script.
rem settings path contains spaces (D:\Program Files\...), store in var then pass -s "%VAR%" to avoid nested quote issues.
set "MVN_SETTINGS=%MAVEN_SETTINGS%"
if not defined MVN_SETTINGS if exist "%USERPROFILE%\Desktop\settings.xml" set "MVN_SETTINGS=%USERPROFILE%\Desktop\settings.xml"

set "_U="
if defined CENTRAL_USERNAME set "_U=-Dcentral.username=%CENTRAL_USERNAME%"
set "_P="
if defined CENTRAL_PASSWORD set "_P=-Dcentral.password=%CENTRAL_PASSWORD%"
set "_K="
if defined GPG_KEYNAME set "_K=-Dgpg.keyname=%GPG_KEYNAME%"

rem ===== Tool detection =====
where mvn >nul 2>nul
if errorlevel 1 (
    echo ERROR: mvn is not in PATH.
    exit /b 1
)
where gpg >nul 2>nul
if errorlevel 1 (
    echo WARN: gpg is not in PATH. Real release signing may fail.
)

rem ===== Version management =====
set "VERSION_FILE=.publish-version"
set "POM_VERSION="
for /f "delims=" %%V in ('findstr /C:"<version>" pom.xml') do (
    if not defined POM_VERSION set "POM_VERSION=%%V"
)
if not defined POM_VERSION (
    echo ERROR: cannot read version from pom.xml
    exit /b 1
)
set "POM_VERSION=%POM_VERSION:*<version>=%"
set "POM_VERSION=%POM_VERSION:</version>=%"
set "POM_RELEASE=%POM_VERSION:-SNAPSHOT=%"

set "LAST="
if exist "%VERSION_FILE%" (
    for /f "delims=" %%L in (%VERSION_FILE%) do set "LAST=%%L"
)
if not defined LAST set "LAST=0.0.0"
set "LAST=%LAST:-SNAPSHOT=%"

call :semver_num "%LAST%" LAST_N
call :semver_num "%POM_RELEASE%" POM_N
if !LAST_N! GTR !POM_N! (
    set "BASE=%LAST%"
) else (
    set "BASE=%POM_RELEASE%"
)

if defined PIN_VERSION (
    call :validate "%PIN_VERSION%"
    if errorlevel 1 exit /b 1
    set "NEW_VERSION=%PIN_VERSION%"
) else (
    call :bump "%BASE%" NEW_VERSION
)

call :split "%NEW_VERSION%" MAJOR MINOR PATCH

echo ==================================================
echo  Brick BootKit Maven Central publish
echo  Last version : %LAST%
echo  Current POM  : %POM_RELEASE%
echo  Base version : %BASE%
echo  New version  : %NEW_VERSION%
echo ==================================================

rem All mvn calls must use "call mvn" - otherwise cmd direct external exe call terminates the whole bat.
rem Two explicit branches for settings to avoid nested if quote issues.

if "%CHECK_ONLY%"=="1" (
    echo CHECK ONLY: validating Maven release profile. No deploy will be executed.
    if defined MVN_SETTINGS (
        call mvn -s "%MVN_SETTINGS%" -B -P release -DskipTests -Dgpg.skip=true validate
    ) else (
        call mvn -B -P release -DskipTests -Dgpg.skip=true validate
    )
    if errorlevel 1 (
        echo ERROR: Maven release profile validation failed.
        exit /b 1
    )
    echo CHECK OK.
    exit /b 0
)

rem ===== Set release version across all modules =====
rem CRITICAL: cmd eats the .10 in -DnewVersion=4.0.10 as path extension, turning it into -DnewVersion=4.0.
rem Must quote the whole arg: "-DnewVersion=%NEW_VERSION%" so the dot is preserved.
rem Same for -SNAPSHOT suffix: "-DnewVersion=%NEW_VERSION%-SNAPSHOT" or -SNAPSHOT gets executed as a command.
echo Setting release version: %NEW_VERSION%
if defined MVN_SETTINGS (
    call mvn -s "%MVN_SETTINGS%" -B versions:set "-DnewVersion=%NEW_VERSION%" -DprocessAllModules=true -DgenerateBackupPoms=false
) else (
    call mvn -B versions:set "-DnewVersion=%NEW_VERSION%" -DprocessAllModules=true -DgenerateBackupPoms=false
)
if errorlevel 1 (
    echo ERROR: Failed to set release version.
    exit /b 1
)
echo Release version set OK: %NEW_VERSION%

rem ===== Build + deploy to Maven Central =====
echo Building and deploying to Central Portal...
if defined MVN_SETTINGS (
    call mvn -s "%MVN_SETTINGS%" -B clean deploy -P release -DskipTests -Dgpg.passphraseServerId=gpg %_U% %_P% %_K%
) else (
    call mvn -B clean deploy -P release -DskipTests -Dgpg.passphraseServerId=gpg %_U% %_P% %_K%
)
if errorlevel 1 (
    echo ERROR: Build or deploy failed. POM version is still %NEW_VERSION%; fix the error before retrying or rollback manually.
    exit /b 1
)

echo %NEW_VERSION%> "%VERSION_FILE%"

echo ==================================================
echo  Publish completed. Version: %NEW_VERSION%
echo  Maven Central sync usually takes 30 minutes to 2 hours.
echo ==================================================

rem ===== Roll POM version back to -SNAPSHOT =====
echo Rolling POM version back to development snapshot: %NEW_VERSION%-SNAPSHOT
if defined MVN_SETTINGS (
    call mvn -s "%MVN_SETTINGS%" -B versions:set "-DnewVersion=%NEW_VERSION%-SNAPSHOT" -DprocessAllModules=true -DgenerateBackupPoms=false
) else (
    call mvn -B versions:set "-DnewVersion=%NEW_VERSION%-SNAPSHOT" -DprocessAllModules=true -DgenerateBackupPoms=false
)
if errorlevel 1 (
    echo WARN: Failed to roll POM version back to snapshot. Please run versions:set manually.
    exit /b 1
)

echo POM version rolled back to %NEW_VERSION%-SNAPSHOT.
endlocal
exit /b 0

rem ===== helpers =====
:semver_num
set "_v=%~1"
set "_v=!_v:-SNAPSHOT=!"
for /f "tokens=1,2,3 delims=." %%A in ("!_v!") do (
    set /a "_n=%%A*1000000+%%B*1000+%%C"
)
set "%~2=!_n!"
goto :eof

:bump
set "_v=%~1"
for /f "tokens=1,2,3 delims=." %%A in ("!_v!") do (
    set /a "_p=%%C+1"
    set "%~2=%%A.%%B.!_p!"
)
goto :eof

:split
set "_v=%~1"
for /f "tokens=1,2,3 delims=." %%A in ("!_v!") do (
    set "%~2=%%A"
    set "%~3=%%B"
    set "%~4=%%C"
)
goto :eof

:validate
echo %~1| findstr /R "^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo ERROR: Invalid version: %~1
    exit /b 1
)
exit /b 0
