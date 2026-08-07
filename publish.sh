#!/usr/bin/env bash
# publish.sh — 一键发布 Brick BootKit 到 Maven 中央仓库 (Sonatype Central Portal)
# 版本号优先从根 pom.xml 当前版本推导, 并结合 .publish-version 防止回退; 发布后写入 .publish-version 锚定下次递增
#
# 用法:
#   ./publish.sh                  Run real release deploy.
#   ./publish.sh --check          仅校验 release profile, 不改版本, 不发布.
#   ./publish.sh --version 4.1.0  手动指定本次发布版本.
#
# macOS/平台支持:
#   本脚本在 macOS (Darwin) 与 Linux 上均可运行.
#   macOS 默认使用 BSD sed/grep; 版本号读取改用 awk 兼容 BSD.
#   若需指定发布平台 (multi-arch gpg / jdk), 可用:
#     PLATFORM=darwin ./publish.sh          # 强制 darwin 平台
#     PLATFORM=linux ./publish.sh           # 强制 linux 平台
#     MAVEN_SETTINGS=...  GPG_KEYNAME=...  ./publish.sh
set -euo pipefail

cd "$(dirname "$0")"

# ===== 参数解析 =====
CHECK_ONLY=0
PIN_VERSION=""
while [ $# -gt 0 ]; do
    case "$1" in
        --check)
            CHECK_ONLY=1
            shift
            ;;
        --version)
            if [ $# -lt 2 ]; then
                echo "ERROR: --version 需要一个版本号参数"
                exit 1
            fi
            PIN_VERSION="$2"
            shift 2
            ;;
        *)
            echo "ERROR: 未知参数: $1"
            exit 1
            ;;
    esac
done

# ===== Maven settings / Central 账号 =====
# central-publishing-maven-plugin 使用 settings.xml 中 serverId=central 的 username/password。
# 如需使用桌面上的 settings 文件, 可执行: MAVEN_SETTINGS="$HOME/Desktop/settings.xml" ./publish.sh
: "${MAVEN_SETTINGS:=}"
: "${CENTRAL_USERNAME:=}"
: "${CENTRAL_PASSWORD:=}"
: "${GPG_KEYNAME:=}"
: "${GPG_PASSPHRASE:=}"
: "${PLATFORM:=}"

# ===== 平台检测 (macOS 支持) =====
OS_TYPE="$(uname -s)"
if [ -z "$PLATFORM" ]; then
    case "$OS_TYPE" in
        Darwin*) PLATFORM="darwin" ;;
        Linux*)  PLATFORM="linux" ;;
        MINGW*|MSYS*|CYGWIN*) PLATFORM="windows" ;;
        *) PLATFORM="unknown" ;;
    esac
fi
echo "Publish platform: $PLATFORM (uname: $OS_TYPE)"

# ===== 工具检测 =====
command -v mvn >/dev/null || { echo "ERROR: mvn 不在 PATH"; exit 1; }
command -v gpg >/dev/null || { echo "WARN: gpg 未安装, GPG 签名会失败"; }

# 未显式指定时, 自动尝试桌面常见文件名; 都不存在则使用 Maven 默认 ~/.m2/settings.xml。
if [ -z "$MAVEN_SETTINGS" ]; then
    if [ -f "$HOME/Desktop/settings.xml" ]; then
        MAVEN_SETTINGS="$HOME/Desktop/settings.xml"
    elif [ -f "$HOME/Desktop/setting.xml" ]; then
        MAVEN_SETTINGS="$HOME/Desktop/setting.xml"
    fi
fi

MAVEN_SETTINGS_ARG=()
if [ -n "$MAVEN_SETTINGS" ]; then
    if [ ! -f "$MAVEN_SETTINGS" ]; then
        echo "ERROR: MAVEN_SETTINGS 指定的文件不存在: $MAVEN_SETTINGS"
        exit 1
    fi
    MAVEN_SETTINGS_ARG=(-s "$MAVEN_SETTINGS")
fi

# ===== 版本号管理: 优先从根 pom.xml 当前版本推导, 结合 .publish-version 防止回退 =====
# 规则:
# 1. 根 pom.xml 当前版本为 4.0.9, 则本次发布 4.0.10;
# 2. 如果 .publish-version 记录的发布版本更高, 则基于 .publish-version +1;
# 3. 可用环境变量 PIN_VERSION 或 --version 参数手动指定本次发布版本。
VERSION_FILE=".publish-version"

# 兼容 BSD sed (macOS): 用 awk 提取根 pom 第一个 <version> 行内容
POM_VERSION=$(awk -F'[<>]' '/<version>/ {print $3; exit}' pom.xml | tr -d '[:space:]')
if [ -z "$POM_VERSION" ]; then
    echo "ERROR: 无法从 pom.xml 读取版本号"
    exit 1
fi
POM_RELEASE_VERSION="${POM_VERSION%-SNAPSHOT}"

if [ -f "$VERSION_FILE" ]; then
    LAST=$(tr -d '[:space:]' < "$VERSION_FILE")
else
    LAST="0.0.0"
fi

semver_to_num() {
    local v=${1%-SNAPSHOT}
    local major minor patch
    IFS=. read -r major minor patch <<< "$v"
    printf '%d%03d%03d\n' "$major" "$minor" "$patch"
}

bump_patch() {
    local v=${1%-SNAPSHOT}
    local major minor patch
    IFS=. read -r major minor patch <<< "$v"
    patch=$((patch + 1))
    printf '%s.%s.%s\n' "$major" "$minor" "$patch"
}

validate_semver() {
    [[ "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]
}

if ! validate_semver "$POM_RELEASE_VERSION"; then
    echo "ERROR: 无法从 pom.xml 读取有效版本号: ${POM_VERSION:-<empty>}"
    exit 1
fi
if ! validate_semver "$LAST"; then
    echo "ERROR: $VERSION_FILE 中不是有效版本号: $LAST"
    exit 1
fi

if [ "$(semver_to_num "$LAST")" -gt "$(semver_to_num "$POM_RELEASE_VERSION")" ]; then
    BASE_VERSION="$LAST"
else
    BASE_VERSION="$POM_RELEASE_VERSION"
fi

if [ -n "$PIN_VERSION" ]; then
    if ! validate_semver "$PIN_VERSION"; then
        echo "ERROR: 指定版本号不是有效 semver: $PIN_VERSION"
        exit 1
    fi
    NEW_VERSION="$PIN_VERSION"
else
    NEW_VERSION=$(bump_patch "$BASE_VERSION")
fi

MAJOR=$(echo "$NEW_VERSION" | cut -d. -f1)
MINOR=$(echo "$NEW_VERSION" | cut -d. -f2)
PATCH=$(echo "$NEW_VERSION" | cut -d. -f3)

echo "================================================"
echo " Brick BootKit 发布到 Maven 中央仓库"
echo " 上次发布版本: $LAST"
echo " 当前 POM 版本: $POM_RELEASE_VERSION"
echo " 版本计算基准: $BASE_VERSION"
echo " 本次发布版本: $NEW_VERSION"
echo "================================================"

if [ "$CHECK_ONLY" = "1" ]; then
    echo "CHECK ONLY: 仅校验 release profile, 不会执行发布."
    mvn "${MAVEN_SETTINGS_ARG[@]}" -q -P release -DskipTests -Dgpg.skip=true validate
    echo "CHECK OK."
    exit 0
fi

# ===== 设置本次发布版本号 (更新 parent pom + 各模块 pom 的 <version>) =====
# 用 versions:set 自动改所有模块的版本, 再 commit 消除 -SNAPSHOT 后缀
mvn "${MAVEN_SETTINGS_ARG[@]}" -q versions:set -DnewVersion="$NEW_VERSION" -DprocessAllModules=true -DgenerateBackupPoms=false
echo "已设置本次发布版本: $NEW_VERSION"

# ===== 构建 + 发布到 Maven Central (含 source/javadoc/gpg 签名) =====
# -P release 启用 GPG + central-publishing-maven-plugin。
# gpg.passphraseServerId 指向 settings.xml 中 serverId=gpg 的 passphrase。
MVN_GOALS="clean deploy"
MVN_PROFILES="release"
MVN_ARGS="-DskipTests -Dgpg.passphraseServerId=gpg"

# Central 账号通常应配置在 settings.xml 的 <server><id>central</id> 中。
# 如需环境变量注入, central-publishing-maven-plugin 使用 publishingServerId=central, 仍建议优先写 settings.xml。
if [ -n "$CENTRAL_USERNAME" ]; then
    MVN_ARGS="$MVN_ARGS -Dcentral.username=$CENTRAL_USERNAME"
fi
if [ -n "$CENTRAL_PASSWORD" ]; then
    MVN_ARGS="$MVN_ARGS -Dcentral.password=$CENTRAL_PASSWORD"
fi
if [ -n "$GPG_KEYNAME" ]; then
    MVN_ARGS="$MVN_ARGS -Dgpg.keyname=$GPG_KEYNAME"
fi

if [ -n "$MAVEN_SETTINGS" ]; then
    echo "使用 Maven settings: $MAVEN_SETTINGS"
else
    echo "使用 Maven 默认 settings: ~/.m2/settings.xml"
fi

echo "正在构建并发布到 Maven Central..."
mvn "${MAVEN_SETTINGS_ARG[@]}" $MVN_GOALS -P $MVN_PROFILES $MVN_ARGS

# ===== Central 发布完成 =====
echo "Maven Central 发布流程已执行完成"

# ===== 写入本次版本号锚点, 下次脚本执行时递增 =====
echo "$NEW_VERSION" > "$VERSION_FILE"
echo ""
echo "================================================"
echo " 发布完成! 版本: $NEW_VERSION"
echo " 中央仓库同步通常需要 30 分钟~2 小时"
echo " 下次执行本脚本将自动递增到 $MAJOR.$MINOR.$((PATCH + 1))"
echo "================================================"

# ===== 回滚 pom 版本为 -SNAPSHOT (保持开发态) =====
mvn "${MAVEN_SETTINGS_ARG[@]}" -q versions:set -DnewVersion="${NEW_VERSION}-SNAPSHOT" -DprocessAllModules=true -DgenerateBackupPoms=false
echo "pom 已回滚到 ${NEW_VERSION}-SNAPSHOT 开发态"
