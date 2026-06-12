#!/usr/bin/env bash
set -euo pipefail

# Publish brick-bootkit artifacts to Sonatype Central Portal.
# Usage:
#   ./scripts/publish-central.sh
#
# Optional overrides:
#   MAVEN_BIN=/path/to/mvn \
#   MAVEN_SETTINGS=/path/to/settings.xml \
#   MAVEN_REPO_LOCAL=/path/to/repo \
#   JAVA_HOME=/path/to/jdk17 \
#   GPG_KEY_ID=your-key-id \
#   ./scripts/publish-central.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

MAVEN_BIN="${MAVEN_BIN:-/Users/vim/Desktop/hDocuments/apache-maven-3.9.11/bin/mvn}"
MAVEN_SETTINGS="${MAVEN_SETTINGS:-/Users/vim/Desktop/hDocuments/apache-maven-3.9.11/conf/settings.xml}"
MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-/Users/vim/Desktop/hDocuments/repos}"
GPG_KEY_ID="${GPG_KEY_ID:-2A29535B67FA2CFC}"

if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -d "/Users/vim/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home" ]]; then
    export JAVA_HOME="/Users/vim/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home"
  elif command -v /usr/libexec/java_home >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  fi
fi

export PATH="$JAVA_HOME/bin:$PATH"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

[[ -x "$MAVEN_BIN" ]] || fail "Maven executable not found or not executable: $MAVEN_BIN"
[[ -f "$MAVEN_SETTINGS" ]] || fail "Maven settings.xml not found: $MAVEN_SETTINGS"
[[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]] || fail "JAVA_HOME is not a valid JDK: ${JAVA_HOME:-<empty>}"
[[ -x "$JAVA_HOME/bin/javadoc" ]] || fail "JAVA_HOME must point to a full JDK with javadoc: $JAVA_HOME"
command -v gpg >/dev/null 2>&1 || fail "gpg command not found"
gpg --list-secret-keys --keyid-format LONG "$GPG_KEY_ID" >/dev/null 2>&1 || fail "GPG secret key not found: $GPG_KEY_ID"

mkdir -p "$MAVEN_REPO_LOCAL"

cd "$ROOT_DIR"

echo "Publishing brick-bootkit to Sonatype Central"
echo "  project       : $ROOT_DIR"
echo "  maven         : $MAVEN_BIN"
echo "  settings      : $MAVEN_SETTINGS"
echo "  local repo    : $MAVEN_REPO_LOCAL"
echo "  JAVA_HOME     : $JAVA_HOME"
echo "  GPG key       : $GPG_KEY_ID"
echo

echo "Running deploy..."
"$MAVEN_BIN" \
  --update-snapshots \
  -s "$MAVEN_SETTINGS" \
  -Dmaven.repo.local="$MAVEN_REPO_LOCAL" \
  clean -Prelease deploy \
  -Dmaven.test.skip=true \
  -Dgpg.keyname="$GPG_KEY_ID" \
  -Dgpg.passphrase= \
  -f pom.xml

echo
echo "Deploy upload finished."
echo "If Central reports 'validated', finish publishing here:"
echo "https://central.sonatype.com/publishing/deployments"
