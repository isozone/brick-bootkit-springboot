#!/usr/bin/env bash
#
# brick-bootkit 接入前体检器启动脚本。
#
# 用法:
#   ./preflight.sh /path/to/your/project
#   ./preflight.sh /path/to/your/project --json
#
# 说明:
#   体检器为零依赖单文件 Java 程序，通过 JDK 单文件源码模式直接运行，
#   不需要先把项目编译或打包，也不会修改目标项目的任何文件。
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_FILE="$SCRIPT_DIR/Preflight.java"

if [ ! -f "$SOURCE_FILE" ]; then
  echo "未找到 Preflight.java: $SOURCE_FILE" >&2
  exit 2
fi

JAVA_BIN="java"
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
fi

if ! command -v "$JAVA_BIN" >/dev/null 2>&1 && [ ! -x "$JAVA_BIN" ]; then
  echo "未找到 java 命令，请安装 JDK 17+ 或设置 JAVA_HOME。" >&2
  exit 2
fi

JAVA_MAJOR="$("$JAVA_BIN" -version 2>&1 | head -n 1 | sed -E 's/.*version "([^"]+)".*/\1/' | cut -d. -f1)"
if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -lt 17 ] 2>/dev/null; then
  echo "当前 java 版本过低（检测到 ${JAVA_MAJOR:-未知}），体检器需要 JDK 17+。" >&2
  echo "注意：这里要求的是运行体检器的 JDK，不是被体检项目的编译版本。" >&2
  exit 2
fi

exec "$JAVA_BIN" -Dfile.encoding=UTF-8 "$SOURCE_FILE" "$@"
