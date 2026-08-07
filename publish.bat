@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem publish.bat - Publish Brick BootKit to Maven Central (Sonatype Central Portal).
rem Usage:
rem   publish.bat          Run real release deploy.
rem   publish.bat --check  Validate release profile only; no version change, no deploy.
rem   publish.bat --version 4.1.0   Pin a specific release version.

cd /d "%~dp0" || exit /b 1

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

rem ===== Maven settings / Central 账号 =====
rem central-publishing-maven-plugin 使用 settings.xml 中 serverId=central 的 username/password。
rem 可通过环境变量 MAVEN_SETTINGS / CENTRAL_USERNAME / CENTRAL_PASSWORD / GPG_KEYNAME 注入。
set "MAVEN_SETTINGS=%MAVEN_SETTINGS%"
if not defined MAVEN_SETTINGS if exist "%USERPROFILE%\Desktop\settings.xml" set "MAVEN_SETTINGS=%USERPROFILE%\Desktop\settings.xml"

set "MVN_BASE=mvn"
if defined MAVEN_SETTINGS (
    if not exist "%MAVEN_SETTINGS%" (
        echo ERROR: MAVEN_SETTINGS 指定的文件不存在: %MAVEN_SETTINGS%
        exit /b 1
    )
    set "MVN_BASE=mvn -s "%MAVEN_SETTINGS%""
)

set "_U="
if defined CENTRAL_USERNAME set "_U=-Dcentral.username=%CENTRAL_USERNAME%"
set "_P="
if defined CENTRAL_PASSWORD set "_P=-Dcentral.password=%CENTRAL_PASSWORD%"
set "_K="
if defined GPG_KEYNAME set "_K=-Dgpg.keyname=%GPG_KEYNAME%"

rem ===== 工具检测 =====
where mvn >nul 2>nul
if errorlevel 1 (
    echo ERROR: mvn 不在 PATH.
    exit /b 1
)
where gpg >nul 2>nul
if errorlevel 1 (
    echo WARN: gpg 不在 PATH, 真实发布签名会失败.
)

rem ===== 版本号管理 =====
rem 优先级: --version > 根 pom 当前版本 bump > .publish-version bump
rem 本项目根 pom 使用固定版本号 (非 ${revision}), 直接读取 <version> 行。
set "VERSION_FILE=.publish-version"
set "LAST="
for /f "tokens=* delims=" %%V in ('grep -m1 "<version>" pom.xml') do (
    for /f "tokens=2 delims=<>" %%A in ("%%V") do set "POM_VERSION=%%A"
)
if not defined POM_VERSION set "POM_VERSION=0.0.0"
set "POM_RELEASE=%POM_VERSION:-SNAPSHOT=%"

if exist "%VERSION_FILE%" set /p "LAST=" < "%VERSION_FILE%"
if not defined LAST set "LAST=0.0.0"
set "LAST=%LAST:-SNAPSHOT=%"

rem 取 LAST 与 POM_RELEASE 中较大者作为基准
call :semver_num "%LAST%" LAST_N
call :semver_num "%POM_RELEASE%" POM_N
if !LAST_N! GTR !POM_N! (
    set "BASE=%LAST%"
) else (
    set "BASE=%POM_RELEASE%"
)

if defined PIN_VERSION (
    call :validate "%PIN_VERSION%" || exit /b 1
    set "NEW_VERSION=%PIN_VERSION%"
) else (
    call :bump "%BASE%" NEW_VERSION
)

for /f "tokens=1,2,3 delims=." %%A in ("%NEW_VERSION%") do (
    set "MAJOR=%%A"
    set "MINOR=%%B"
    set "PATCH=%%C"
)

echo ==================================================
echo  Brick BootKit Maven Central publish
echo  Last version : %LAST%
echo  Current POM  : %POM_RELEASE%
echo  Base version : %BASE%
echo  New version  : %NEW_VERSION%
echo ==================================================

if "%CHECK_ONLY%"=="1" (
    echo CHECK ONLY: validating Maven release profile. No deploy will be executed.
    call %MVN_BASE% -q -P release -DskipTests -Dgpg.skip=true validate
    if errorlevel 1 (
        echo ERROR: Maven release profile validation failed.
        exit /b 1
    )
    echo CHECK OK.
    exit /b 0
)

rem ===== 设置本次发布版本号 (跨模块同步) =====
echo Setting release version: %NEW_VERSION%
call %MVN_BASE% -q versions:set -DnewVersion=%NEW_VERSION% -DprocessAllModules=true -DgenerateBackupPoms=false
if errorlevel 1 (
    echo ERROR: Failed to set release version.
    exit /b 1
)

rem ===== 构建 + 发布到 Maven Central =====
echo Building and deploying to Central Portal...
call %MVN_BASE% clean deploy -P release -DskipTests -Dgpg.passphraseServerId=gpg %_U% %_P% %_K%
if errorlevel 1 (
    echo ERROR: Build or deploy failed. POM version is still %NEW_VERSION%; fix the error before retrying or rollback manually.
    exit /b 1
)

echo %NEW_VERSION%> "%VERSION_FILE%"

echo ==================================================
echo  Publish completed. Version: %NEW_VERSION%
echo  Maven Central sync usually takes 30 minutes to 2 hours.
echo ==================================================

rem ===== 回滚 pom 版本为 -SNAPSHOT =====
echo Rolling POM version back to development snapshot: %NEW_VERSION%-SNAPSHOT
call %MVN_BASE% -q versions:set -DnewVersion=%NEW_VERSION%-SNAPSHOT -DprocessAllModules=true -DgenerateBackupPoms=false
if errorlevel 1 (
    echo WARN: Failed to roll POM version back to snapshot. Please run versions:set manually.
    exit /b 1
)

echo POM version rolled back to %NEW_VERSION%-SNAPSHOT.
endlocal
exit /b 0

rem ===== helpers =====
:semver_num
rem %1 = x.y.z, 返回 %2 = xNNNNNN (大整数便于比较)
setlocal EnableDelayedExpansion
set "_v=%~1"
set "_v=!_v:-SNAPSHOT=!"
for /f "tokens=1,2,3 delims=." %%A in ("!_v!") do (
    set /a "_n=%%A*1000000+%%B*1000+%%C"
)
endlocal & set "%~2=%_n%"
goto :eof

:bump
rem %1 = x.y.z, 返回 %2 = x.y.(z+1)
setlocal EnableDelayedExpansion
set "_v=%~1"
for /f "tokens=1,2,3 delims=." %%A in ("!_v!") do (
    set /a "_p=%%C+1"
    set "_out=%%A.%%B.!_p!"
)
endlocal & set "%~2=%_out%"
goto :eof

:validate
rem %1 = 待校验版本, 返回 errorlevel 0/1
echo %~1| findstr /R "^[0-9]+\.[0-9]+\.[0-9]+$" >nul
if errorlevel 1 (
    echo ERROR: 无效版本号: %~1
    exit /b 1
)
exit /b 0
