#!/usr/bin/env bash
# ============================================================
# Brick BootKit 项目专用 Maven 启动器
# ------------------------------------------------------------
# 背景：本机 Apache Maven 安装于含空格的路径
#       ("C:\Program Files\..." 或 "D:\Program Files\...")，
#       在 Git-Bash 下运行原版 mvn 脚本时，脚本生成的类路径
#       使用 UNIX 风格路径(/c/...)，Windows 的 java.exe 无法识别，
#       导致报 ClassNotFoundException: plexus.classworlds.launcher.Launcher。
#
# 解决：本项目统一用 Windows 风格路径直接起动 classworlds Launcher，
#       并显式传入 -Dmaven.multiModuleProjectDirectory，绕开脚本缺陷。
#
# 用法：
#   MAVEN_HOME=<maven安装目录> tools/mvn-shim.sh <maven参数...>
#   示例：
#     MAVEN_HOME="C:\tools\maven" tools/mvn-shim.sh clean install -DskipTests
#
# 默认 MAVEN_HOME：C:\tools\maven（项目开发机上的无空格副本）
# ============================================================

set -e

# 默认 Maven 安装目录（无空格副本）
DEFAULT_M2='C:\tools\maven'
M2="${MAVEN_HOME:-$DEFAULT_M2}"

# Git-Bash 前缀下的根目录换算：/c/x -> C:\x
M2_UNIX="$(echo "$M2" | sed 's#^C:\\\\#/c/#; s#^C:#/c/#; s#\\\\#/#g')"
if [ ! -d "$M2_UNIX" ]; then
  M2_UNIX="$(cygpath -u "$M2" 2>/dev/null || echo "$M2")"
fi

# 定位 classworlds jar
CW_JAR="$(ls "$M2_UNIX"/boot/plexus-classworlds-*.jar 2>/dev/null | head -1)"
if [ -z "$CW_JAR" ]; then
  echo "[mvn-shim] 找不到 classworlds jar: $M2" >&2
  exit 1
fi

CW_WIN="$(cygpath -w "$CW_JAR" 2>/dev/null || echo "$M2\\boot\\$(basename "$CW_JAR")")"
CONF_WIN="$(cygpath -w "$M2_UNIX/bin/m2.conf" 2>/dev/null || echo "$M2\\bin\\m2.conf")"
M2_HOME_WIN="$(cygpath -w "$M2_UNIX" 2>/dev/null || echo "$M2")"

# 项目根目录（本脚本位于 <root>/tools/ 下）
PROJ_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJ_ROOT_WIN="$(cygpath -w "$PROJ_ROOT" 2>/dev/null || echo "$PROJ_ROOT")"

exec java -classpath "$CW_WIN" \
  "-Dclassworlds.conf=$CONF_WIN" \
  "-Dmaven.home=$M2_HOME_WIN" \
  "-Dmaven.multiModuleProjectDirectory=$PROJ_ROOT_WIN" \
  "-Dfile.encoding=UTF-8" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@"