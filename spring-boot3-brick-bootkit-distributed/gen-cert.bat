@echo off
rem ===========================================================================
rem 一键生成 Brick BootKit 分布式插件模块所需的「自签名 TLS 证书」(Windows).
rem
rem 用法:
rem   gen-cert.bat                       默认输出到 cert\
rem   gen-cert.bat --out D:\certs --ip 10.0.0.21 --ip 127.0.0.1
rem
rem 产物(%OUT_DIR%):
rem   server.key   PKCS#8 私钥 -> WORKER 的 plugin.distributed.tls-private-key
rem   server.crt   自签证书     -> WORKER 的 plugin.distributed.tls-cert-chain
rem   ca.crt       同一张证书   -> HOST 的 plugin.distributed.tls-ca-cert
rem
rem 依赖: openssl 已在 PATH 中 (Windows 可用 Git Bash / Chocolatey / 官方安装包)。
rem ===========================================================================
setlocal enabledelayedexpansion
set "OUT_DIR=cert"
set "DAYS=3650"
set "KEYSIZE=2048"
set "CN=brick-bootkit"
set "IP_LIST=127.0.0.1"
set "C=CN"
set "O=Brick BootKit"

:parse
if "%~1"=="" goto done
if /i "%~1"=="-h" goto help
if /i "%~1"=="-o" set "OUT_DIR=%~2" & shift & shift & goto parse
if /i "%~1"=="-d" set "DAYS=%~2" & shift & shift & goto parse
if /i "%~1"=="-k" set "KEYSIZE=%~2" & shift & shift & goto parse
if /i "%~1"=="-c" set "CN=%~2" & shift & shift & goto parse
if /i "%~1"=="--ip" set "IP_LIST=!IP_LIST!,%~2" & shift & shift & goto parse
echo Unknown arg: %~1 >&2
goto help

:done
where openssl >nul 2>nul
if errorlevel 1 (
  echo ERROR: 未找到 openssl, 请先安装或加入 PATH. >&2
  exit /b 1
)

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

rem 聚合 SAN (脚本这里只处理 IP; DNS 请用 Linux/gen-cert.sh 版本)
set "SAN="
for %%i in (%IP_LIST%) do (
  if not "%%i"=="" set "SAN=!SAN!IP:%%i,"
)
if "%SAN%"=="" set "SAN=IP:127.0.0.1"
set "SAN=%SAN:~0,-1%"

set "SUBJ=/C=%C%/O=%O%/CN=%CN%"

echo Generate self-signed cert (days=%DAYS%, rsa=%KEYSIZE%, SAN=%SAN%)

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:%KEYSIZE% -out "%OUT_DIR%\server.key"
if errorlevel 1 exit /b 1

openssl req -new -key "%OUT_DIR%\server.key" -subj "%SUBJ%" ^
  -addext "subjectAltName=%SAN%" ^
  -addext "basicConstraints=critical,CA:TRUE" ^
  -addext "keyUsage=critical,digitalSignature,keyEncipherment,keyCertSign" ^
  -addext "extendedKeyUsage=serverAuth,clientAuth" ^
  -x509 -days %DAYS% -sha256 -out "%OUT_DIR%\server.crt"
if errorlevel 1 exit /b 1

copy /y "%OUT_DIR%\server.crt" "%OUT_DIR%\ca.crt" >nul

echo.
echo Done:
echo   private key : %OUT_DIR%\server.key   (Worker tls-private-key)
echo   cert        : %OUT_DIR%\server.crt   (Worker tls-cert-chain)
echo   trust CA    : %OUT_DIR%\ca.crt       (Host    tls-ca-cert)
echo.
call openssl rand -hex 16
echo Set the same token as above on BOTH Host and Worker (plugin.distributed.auth-token)
exit /b 0

:help
echo Usage: gen-cert.bat [--out dir] [--days n] [--keysize n] [--cn name] [--ip ip,...]
echo Example: gen-cert.bat --out D:\certs --ip 10.0.0.21 --ip 127.0.0.1
exit /b 0