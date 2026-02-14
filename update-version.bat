@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
set "POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT="
for /f "delims=" %%i in ('git rev-parse --show-toplevel 2^>nul') do set "PROJECT_ROOT=%%i"
if not defined PROJECT_ROOT (
    if exist "%SCRIPT_DIR%pom.xml" (
        set "PROJECT_ROOT=%SCRIPT_DIR%"
    ) else (
        set "PROJECT_ROOT=%cd%"
    )
)

if "%~1"=="" (
    echo 错误: 请提供新版本号
    echo 用法: %~nx0 ^<new-version^>
    echo 示例: %~nx0 4.1.0
    exit /b 1
)

set "NEW_VERSION=%~1"
set "VERSION_OK="
echo %NEW_VERSION%| findstr /r "^^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" >nul && set "VERSION_OK=1"
if not defined VERSION_OK (
    echo %NEW_VERSION%| findstr /r "^^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*-[A-Za-z0-9][A-Za-z0-9]*$" >nul && set "VERSION_OK=1"
)
if not defined VERSION_OK (
    echo 错误: 版本号格式不正确
    echo 示例格式: 4.0.0, 4.1.0, 4.0.1-beta
    exit /b 1
)

cd /d "%PROJECT_ROOT%"

echo === Brick BootKit SpringBoot 版本号更新工具 ===
echo 新版本号: %NEW_VERSION%
echo 项目根目录: %PROJECT_ROOT%
echo.

git rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
    echo 警告: 当前目录不是git仓库
)

call :GetFirstVersion "pom.xml" CURRENT_VERSION
echo === 当前版本信息 ===
echo 当前版本: %CURRENT_VERSION%
echo.

for /f "delims=" %%i in ('"%POWERSHELL%" -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "TS=%%i"
echo 正在备份pom.xml文件...
copy /y "pom.xml" "pom.xml.backup.%TS%" >nul
if exist "spring-boot3-brick-bootkit-core\pom.xml" (
    copy /y "spring-boot3-brick-bootkit-core\pom.xml" "spring-boot3-brick-bootkit-core\pom.xml.backup.%TS%" >nul
)
echo 备份完成
echo.

echo === 更新根目录pom.xml ===
call :ReplaceInFile "pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
if "%ERRORLEVEL%"=="0" (
    echo 根目录pom.xml版本号已更新
) else (
    echo 在根目录pom.xml中未找到版本号 %CURRENT_VERSION%
)

echo === 更新子模块版本号 ===
if exist "spring-boot3-brick-bootkit\pom.xml" (
    echo 正在更新特殊模块: spring-boot3-brick-bootkit
    call :ReplaceInFile "spring-boot3-brick-bootkit\pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
    if "%ERRORLEVEL%"=="0" (
        echo   [OK] 模块版本号已更新
    ) else (
        echo   [WARN] 未找到模块版本号 %CURRENT_VERSION%
    )
    findstr /c:"<artifactId>spring-boot3-brick-bootkit-parent</artifactId>" "spring-boot3-brick-bootkit\pom.xml" >nul
    if not errorlevel 1 (
        call :ReplaceInFile "spring-boot3-brick-bootkit\pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
        if "%ERRORLEVEL%"=="0" (
            echo   [OK] 父模块引用版本号已更新
        ) else (
            echo   [WARN] 未找到父模块引用版本号 %CURRENT_VERSION%
        )
    )
)

for /d %%D in (spring-boot3-brick-bootkit-*) do (
    if exist "%%D\pom.xml" (
        echo 正在更新模块: %%D
        call :ReplaceInFile "%%D\pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
        if "!ERRORLEVEL!"=="0" (
            echo   [OK] 版本号已更新
        ) else (
            echo   [WARN] 未找到版本号 %CURRENT_VERSION%
        )
        findstr /c:"<artifactId>spring-boot3-brick-bootkit-parent</artifactId>" "%%D\pom.xml" >nul
        if not errorlevel 1 (
            call :ReplaceInFile "%%D\pom.xml" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
            if "!ERRORLEVEL!"=="0" (
                echo   [OK] 父模块版本号已更新
            ) else (
                echo   [WARN] 未找到父模块版本号 %CURRENT_VERSION%
            )
        )
    )
)

echo === 更新其他文件中的版本号 ===
if exist "README.md" (
    call :ReplaceInFile "README.md" "%CURRENT_VERSION%" "%NEW_VERSION%"
    if "%ERRORLEVEL%"=="0" echo README.md 版本号已更新
)

if exist "doc" (
    for %%F in (doc\*.md) do (
        call :ReplaceInFile "%%F" "%CURRENT_VERSION%" "%NEW_VERSION%"
        if "!ERRORLEVEL!"=="0" echo %%F 版本号已更新
    )
)

echo 正在更新Maven插件描述文件...
set "PLUGIN_FILE1=spring-boot3-brick-bootkit-maven-packager\src\main\resources\META-INF\maven\com.gitee.starblues.springboot-plugin-maven-packager\plugin-help.xml"
set "PLUGIN_FILE2=spring-boot3-brick-bootkit-maven-packager\src\main\resources\META-INF\maven\plugin.xml"

call :UpdatePluginFile "%PLUGIN_FILE1%"
call :UpdatePluginFile "%PLUGIN_FILE2%"

echo.
echo === 验证更新结果 ===
call :GetFirstVersion "pom.xml" NEW_ROOT_VERSION
if "%NEW_ROOT_VERSION%"=="%NEW_VERSION%" (
    echo 根目录pom.xml: [OK] 版本号正确 (%NEW_VERSION%)
) else (
    echo 根目录pom.xml: [FAIL] 版本号不正确 ^(期望: %NEW_VERSION%, 实际: %NEW_ROOT_VERSION%^) 
)

if exist "spring-boot3-brick-bootkit\pom.xml" (
    call :GetFirstVersion "spring-boot3-brick-bootkit\pom.xml" SPRING_BOOT_KIT_VERSION
    if "%SPRING_BOOT_KIT_VERSION%"=="%NEW_VERSION%" (
        echo   spring-boot3-brick-bootkit: [OK] %SPRING_BOOT_KIT_VERSION%
    ) else (
        echo   spring-boot3-brick-bootkit: [FAIL] %SPRING_BOOT_KIT_VERSION% ^(期望: %NEW_VERSION%^) 
    )
)

echo 检查子模块版本号...
for /d %%D in (spring-boot3-brick-bootkit-*) do (
    if exist "%%D\pom.xml" (
        call :GetFirstVersion "%%D\pom.xml" MODULE_VERSION
        if "!MODULE_VERSION!"=="%NEW_VERSION%" (
            echo   %%D: [OK] !MODULE_VERSION!
        ) else (
            echo   %%D: [FAIL] !MODULE_VERSION! ^(期望: %NEW_VERSION%^) 
        )
    )
)

echo 检查Maven插件描述文件版本号...
call :CheckPluginFile "%PLUGIN_FILE1%"
call :CheckPluginFile "%PLUGIN_FILE2%"

echo.
echo [OK] 版本号更新验证完成
echo.
echo === 后续操作提示 ===
echo 版本号更新已完成，如需验证构建，请手动执行：
echo   编译检查: mvn compile -o
echo   完整构建: mvn clean compile -U
echo   完整打包: mvn clean package

git rev-parse --is-inside-work-tree >nul 2>nul
if not errorlevel 1 (
    echo.
    echo === Git 状态 ===
    echo 检测到git仓库，已修改的文件:
    git status --short 2>nul
    echo.
    echo 建议的后续操作:
    echo   1. 检查所有修改: git diff
echo   2. 提交变更: git add . ^&^& git commit -m "升级版本到 %NEW_VERSION%"
    echo   3. 创建标签: git tag v%NEW_VERSION%
)

echo.
echo 备份文件列表:
dir /b "pom.xml.backup.*" 2>nul
echo 提示: 如需清理备份文件，请手动删除
echo.
echo === 版本号更新完成! ===
echo 旧版本: %CURRENT_VERSION%
echo 新版本: %NEW_VERSION%
echo 请检查所有修改，然后提交到版本控制系统。
exit /b 0

:ReplaceInFile
set "FILE=%~1"
set "OLD=%~2"
set "NEW=%~3"
"%POWERSHELL%" -NoProfile -Command "$path='%FILE%'; if (-not (Test-Path $path)) { exit 1 } $text=[IO.File]::ReadAllText($path); $pattern=[regex]::Escape('%OLD%'); $new='%NEW%'; $updated=[regex]::Replace($text,$pattern,$new); if ($updated -eq $text) { exit 2 } [IO.File]::WriteAllText($path,$updated)"
exit /b %ERRORLEVEL%

:GetFirstVersion
set "FILE=%~1"
set "OUTVAR=%~2"
set "VALUE="
for /f "delims=" %%i in ('findstr /r /c:"<version>.*</version>" "%FILE%"') do (
    set "LINE=%%i"
    set "LINE=!LINE:*<version>=!"
    set "LINE=!LINE:</version>=!"
    set "VALUE=!LINE!"
    goto :GetFirstVersionDone
)
:GetFirstVersionDone
set "%OUTVAR%=%VALUE%"
exit /b 0

:UpdatePluginFile
set "PLUGIN_FILE=%~1"
if exist "%PLUGIN_FILE%" (
    call :ReplaceInFile "%PLUGIN_FILE%" "<version>%CURRENT_VERSION%</version>" "<version>%NEW_VERSION%</version>"
    if "%ERRORLEVEL%"=="0" (
        echo   [OK] %PLUGIN_FILE% 版本号已更新
    ) else (
        echo   [WARN] %PLUGIN_FILE% 中未找到版本号 %CURRENT_VERSION%
    )
) else (
    echo   [WARN] 文件不存在: %PLUGIN_FILE%
)
exit /b 0

:CheckPluginFile
set "PLUGIN_FILE=%~1"
if exist "%PLUGIN_FILE%" (
    call :GetFirstVersion "%PLUGIN_FILE%" PLUGIN_VERSION
    if "%PLUGIN_VERSION%"=="%NEW_VERSION%" (
        echo   %~nx1: [OK] %PLUGIN_VERSION%
    ) else (
        echo   %~nx1: [FAIL] %PLUGIN_VERSION% ^(期望: %NEW_VERSION%^) 
    )
)
exit /b 0
