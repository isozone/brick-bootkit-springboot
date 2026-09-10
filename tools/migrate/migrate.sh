#!/usr/bin/env bash
#
# brick-bootkit 统一迁移向导。
#
# 把 preflight → slicer → scaffold 串成一条命令，让存量项目接入变成一键式体验。
#
# 用法:
#   ./migrate.sh /path/to/project
#   ./migrate.sh /path/to/project --auto    # 自动选评分最高的候选包
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

PREFLIGHT="$ROOT/tools/preflight/preflight.sh"
SLICER="$ROOT/tools/slicer/slice.sh"
SCAFFOLD="$ROOT/tools/scaffold/scaffold.sh"

PROJECT=""
AUTO=false
FORCE=false

for arg in "$@"; do
  case "$arg" in
    --auto) AUTO=true ;;
    --force) FORCE=true ;;
    -h|--help)
      echo "用法: ./migrate.sh <项目路径> [--auto] [--force]"
      echo ""
      echo "  <项目路径>  要迁移的存量项目根目录"
      echo "  --auto      自动选择评分最高的候选包，不交互"
      echo "  --force     scaffold 输出目录已存在时覆盖"
      echo ""
      echo "流程: preflight 体检 → slicer 分析 → scaffold 生成骨架"
      exit 0
      ;;
    -*)
      echo "未知参数: $arg" >&2
      exit 2
      ;;
    *)
      if [ -z "$PROJECT" ]; then
        PROJECT="$arg"
      fi
      ;;
  esac
done

if [ -z "$PROJECT" ]; then
  echo "错误: 需要提供项目路径。" >&2
  echo "用法: ./migrate.sh <项目路径> [--auto] [--force]" >&2
  exit 2
fi

PROJECT="$(cd "$PROJECT" 2>/dev/null && pwd)" || true
if [ ! -d "$PROJECT" ]; then
  echo "目录不存在: $PROJECT" >&2
  exit 2
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo ""
echo "========================================================================"
echo "brick-bootkit 存量项目迁移向导"
echo "目标项目: $PROJECT"
echo "========================================================================"
echo ""

# ==================================================================== 1. preflight
echo "【步骤 1/3】运行 preflight 体检..."
echo ""

set +e
"$PREFLIGHT" "$PROJECT" --json > "$TMP/preflight.json" 2>&1
code=$?
set -e

if [ "$code" -eq 2 ]; then
  echo "预检失败（参数或环境错误）:" >&2
  cat "$TMP/preflight.json" >&2
  exit 2
fi

# 解析 JSON（用 sed/grep 做极简解析）
BLOCK_COUNT=$(grep -o '"blockCount":[0-9]*' "$TMP/preflight.json" | grep -o '[0-9]*' || echo "0")
WARN_COUNT=$(grep -o '"warnCount":[0-9]*' "$TMP/preflight.json" | grep -o '[0-9]*' || echo "0")
ADOPTION=$(grep -o '"adoptionLevel":"[^"]*"' "$TMP/preflight.json" | sed 's/.*"\([^"]*\)".*/\1/' || echo "UNKNOWN")
MAIN_PKG=$(grep -o '"mainPackage":"[^"]*"' "$TMP/preflight.json" | sed 's/.*"\([^"]*\)".*/\1/' || echo "")

echo "  阻断项: $BLOCK_COUNT"
echo "  警告项: $WARN_COUNT"
echo "  推荐接入级别: $ADOPTION"
if [ -n "$MAIN_PKG" ]; then
  echo "  建议主包: $MAIN_PKG"
fi
echo ""

if [ "$code" -ne 0 ] || [ "$BLOCK_COUNT" -gt 0 ]; then
  echo "⚠️ 体检未通过，存在阻断项。请先解决以下问题后再继续:" >&2
  echo ""
  # 打印阻断项
  grep -o '"level":"[^"]*","code":"[^"]*","message":"[^"]*"' "$TMP/preflight.json" | \
    grep '"level":"BLOCK"' | \
    sed 's/.*"code":"\([^"]*\)".*"message":"\([^"]*\)".*/  \1: \2/' || true
  echo ""
  echo "详细报告:" >&2
  cat "$TMP/preflight.json" >&2
  exit 1
fi

echo "✅ 体检通过，无阻断项。"
echo ""

# ==================================================================== 2. slicer
echo "【步骤 2/3】运行切片分析器..."
echo ""

set +e
"$SLICER" "$PROJECT" --json > "$TMP/slicer.json" 2>&1
code=$?
set -e

if [ "$code" -ne 0 ]; then
  echo "切片分析失败:" >&2
  cat "$TMP/slicer.json" >&2
  exit 2
fi

# 提取候选包列表（用 Python 解析 JSON，比纯 bash 可靠）
python3 -c "
import json, sys
try:
    d = json.load(open('$TMP/slicer.json'))
    for i, c in enumerate(d.get('candidates', [])):
        print(f'{i}|{c[\"package\"]}|{c.get(\"score\", 0):.0f}|{c.get(\"classCount\", 0)}|{c.get(\"fanIn\", 0)}|{c.get(\"fanOut\", 0)}')
except Exception as e:
    print(f'ERROR|{e}', file=sys.stderr)
    sys.exit(1)
" > "$TMP/candidates.txt" 2>&1 || {
  echo "解析 slicer 输出失败:" >&2
  cat "$TMP/candidates.txt" >&2
  exit 2
}

if [ ! -s "$TMP/candidates.txt" ]; then
  echo "⚠️ 未找到合适的候选切片。"
  echo "常见原因：项目包结构过于扁平，或所有包都处于高耦合状态。"
  echo "建议：调整 --depth 参数后再试，或先进行解耦重构。"
  exit 1
fi

echo "  候选切片（按适宜度排序）:"
echo ""
INDEX=0
while IFS='|' read -r idx pkg score classes fanIn fanOut; do
  echo "    $((INDEX + 1)). $pkg   评分 $score   类 $classes   被依赖 $fanIn   依赖他人 $fanOut"
  INDEX=$((INDEX + 1))
done < "$TMP/candidates.txt"
echo ""

# 选择候选包
SELECTED=""
if [ "$AUTO" = true ]; then
  SELECTED=$(head -n 1 "$TMP/candidates.txt" | cut -d'|' -f2)
  echo "🤖 --auto 模式：自动选择评分最高的包 $SELECTED"
else
  echo "请选择要生成骨架的候选包（输入序号，或直接回车选第一个）:"
  read -r choice
  if [ -z "$choice" ]; then
    choice=1
  fi
  if ! echo "$choice" | grep -q '^[0-9]*$'; then
    echo "无效输入: $choice" >&2
    exit 2
  fi
  if [ "$choice" -lt 1 ] || [ "$choice" -gt "$INDEX" ]; then
    echo "序号超出范围: $choice（共 $INDEX 个候选）" >&2
    exit 2
  fi
  SELECTED=$(sed -n "${choice}p" "$TMP/candidates.txt" | cut -d'|' -f2)
fi

echo ""
echo "✅ 已选择: $SELECTED"
echo ""

# ==================================================================== 3. scaffold
echo "【步骤 3/3】生成插件骨架..."
echo ""

OUT_DIR="$(pwd)/$(echo "$SELECTED" | awk -F. '{print $NF}')-plugin"

SCAFFOLD_ARGS=("$PROJECT" "$SELECTED" "$OUT_DIR")
if [ "$FORCE" = true ]; then
  SCAFFOLD_ARGS+=("--force")
fi

set +e
"$SCAFFOLD" "${SCAFFOLD_ARGS[@]}" > "$TMP/scaffold.out" 2>&1
code=$?
set -e

if [ "$code" -ne 0 ]; then
  echo "骨架生成失败:" >&2
  cat "$TMP/scaffold.out" >&2
  exit 2
fi

cat "$TMP/scaffold.out"

# ==================================================================== 完成
echo ""
echo "========================================================================"
echo "迁移向导完成"
echo "========================================================================"
echo ""
echo "已生成插件骨架: $OUT_DIR"
echo ""
echo "接入配置建议（写入 application.yml）:"
echo ""
echo "plugin:"
echo "  enable: true"
if [ -n "$MAIN_PKG" ]; then
  echo "  mainPackage: $MAIN_PKG"
fi
echo "  pluginPath:"
echo "    - ./plugins"
echo ""
echo "建议按以下顺序验证:"
echo "  1. L0 影子模式: plugin.autoLoadPlugins=false"
echo "  2. L1 观察模式: plugin.autoStartPlugins=false"
echo "  3. L2 全量模式: 默认配置"
echo ""
echo "每级验证后访问 GET /plugins-web/api/doctor 查看状态。"
echo ""
