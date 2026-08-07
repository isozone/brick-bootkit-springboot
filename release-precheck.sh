#!/usr/bin/env bash

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FAILED=0
WARNED=0

print_step() {
  echo -e "${BLUE}== $1 ==${NC}"
}

pass() {
  echo -e "${GREEN}[OK] $1${NC}"
}

warn() {
  WARNED=1
  echo -e "${YELLOW}[WARN] $1${NC}"
}

fail() {
  FAILED=1
  echo -e "${RED}[FAIL] $1${NC}"
}

run_check() {
  local description="$1"
  shift
  print_step "$description"
  if "$@"; then
    pass "$description"
  else
    fail "$description"
  fi
}

cd "$ROOT_DIR"

echo -e "${BLUE}Brick BootKit Release Precheck${NC}"
echo "Project root: $ROOT_DIR"
echo

run_check "git diff --check" git diff --check

print_step "Tracked workspace noise"
if git status --short | grep -E '^\s*[MADRCU?]{1,2}\s+\.idea/' >/dev/null 2>&1; then
  warn "Detected .idea changes. These are usually local IDE files and should not enter a release commit."
else
  pass "No tracked .idea workspace noise detected"
fi

print_step "Template presence"
for path in \
  "templates/README.md" \
  "templates/host-minimal/pom.xml" \
  "templates/plugin-minimal/pom.xml" \
  "templates/host-broken-main-package/README.md" \
  "templates/host-broken-plugin-path/README.md" \
  "templates/plugin-broken-packaging/README.md" \
  "templates/host-cluster/pom.xml" \
  "templates/plugin-with-dependency/pom.xml" \
  "templates/plugin-capability-demo/pom.xml"; do
  if [[ ! -f "$path" ]]; then
    fail "Missing required template file: $path"
  fi
done
if [[ $FAILED -eq 0 ]]; then
  pass "Template files are present"
fi

print_step "Key docs presence"
for path in \
  "readme.md" \
  "CONTRIBUTING.md" \
  "CODE_OF_CONDUCT.md" \
  "SECURITY.md" \
  "ROADMAP.md" \
  "doc/2.SpringBoot项目快速接入指南.md" \
  "doc/3.兼容与支持矩阵.md" \
  "doc/4.发布与验收清单.md" \
  "doc/5.API清单.md" \
  "doc/updates/4.0.6.md"; do
  if [[ ! -f "$path" ]]; then
    fail "Missing required documentation file: $path"
  fi
done
if [[ $FAILED -eq 0 ]]; then
  pass "Key docs are present"
fi

print_step "Capability controllers presence (2026-08)"
for path in \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/SecurityController.java" \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/RegistryController.java" \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/ConfigurationController.java" \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/PerformanceController.java" \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/ClusterController.java" \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/DependencyController.java" \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/RolloutController.java" \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/EventBusController.java" \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/LogController.java" \
  "spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/MarketplaceController.java"; do
  if [[ ! -f "$path" ]]; then
    fail "Missing capability controller: $path"
  fi
done
if [[ $FAILED -eq 0 ]]; then
  pass "Capability controllers are present"
fi

print_step "Capability frontend pages presence (2026-08)"
for path in \
  "spring-boot3-brick-bootkit-web/vue3/src/views/security/index.vue" \
  "spring-boot3-brick-bootkit-web/vue3/src/views/registry/index.vue" \
  "spring-boot3-brick-bootkit-web/vue3/src/views/config/index.vue" \
  "spring-boot3-brick-bootkit-web/vue3/src/views/performance/index.vue" \
  "spring-boot3-brick-bootkit-web/vue3/src/views/cluster/index.vue" \
  "spring-boot3-brick-bootkit-web/vue3/src/views/dependency/index.vue" \
  "spring-boot3-brick-bootkit-web/vue3/src/views/rollout/index.vue" \
  "spring-boot3-brick-bootkit-web/vue3/src/views/eventbus/index.vue" \
  "spring-boot3-brick-bootkit-web/vue3/src/views/logs/index.vue" \
  "spring-boot3-brick-bootkit-web/vue3/src/views/marketplace/index.vue"; do
  if [[ ! -f "$path" ]]; then
    fail "Missing capability frontend page: $path"
  fi
done
if [[ $FAILED -eq 0 ]]; then
  pass "Capability frontend pages are present"
fi

print_step "Controller / Service unit tests presence"
for path in \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/controller/api/SecurityControllerTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/controller/api/RegistryControllerTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/controller/api/ConfigurationControllerTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/controller/api/PerformanceControllerTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/controller/api/ClusterControllerTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/controller/api/DependencyControllerTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/controller/api/RolloutControllerTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/controller/api/EventBusControllerTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/controller/api/LogControllerTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/SecurityWebServiceTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/RegistryWebServiceTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/ConfigurationWebServiceTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/PerformanceWebServiceTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/ClusterWebServiceTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/DependencyWebServiceTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/RolloutWebServiceTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/EventBusWebServiceTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/LogWebServiceTest.java" \
  "spring-boot3-brick-bootkit-web/src/test/java/com/zqzqq/bootkits/web/service/PluginMarketplaceServiceTest.java"; do
  if [[ ! -f "$path" ]]; then
    fail "Missing unit test: $path"
  fi
done
if [[ $FAILED -eq 0 ]]; then
  pass "Controller / Service unit tests are present"
fi

print_step "Frontend tests (vitest) presence"
for path in \
  "spring-boot3-brick-bootkit-web/vue3/src/utils/error-helper.test.js" \
  "spring-boot3-brick-bootkit-web/vue3/src/utils/download-helper.test.js"; do
  if [[ ! -f "$path" ]]; then
    fail "Missing frontend test: $path"
  fi
done
if [[ $FAILED -eq 0 ]]; then
  pass "Frontend tests are present"
fi

print_step "Stable Java tests"
if [[ -n "${JAVA_HOME:-}" ]]; then
  if ./mvnw -B -ntp \
      -pl spring-boot3-brick-bootkit-loader,spring-boot3-brick-bootkit-core,spring-boot3-brick-bootkit \
      -am test; then
    pass "Stable Java tests"
  else
    fail "Stable Java tests"
  fi
else
  warn "JAVA_HOME is not set. Skipping Maven test execution."
fi

print_step "Docs verification"
if command -v npm >/dev/null 2>&1; then
  if [[ -d "docs-website/node_modules" ]]; then
    if (cd docs-website && npm run verify-docs); then
      pass "Docs verification"
    else
      fail "Docs verification"
    fi
  else
    warn "docs-website/node_modules is missing. Run 'cd docs-website && npm ci' before release."
  fi
else
  warn "npm is not available. Skipping docs verification."
fi

print_step "Web console front-end build"
if command -v npm >/dev/null 2>&1; then
  if [[ -d "spring-boot3-brick-bootkit-web/vue3/node_modules" ]]; then
    if (cd spring-boot3-brick-bootkit-web/vue3 && npm run build); then
      pass "Web console front-end build"
    else
      warn "Front-end build did not complete. Check vue3 dependency state before release."
    fi
  else
    warn "spring-boot3-brick-bootkit-web/vue3/node_modules is missing. Run 'cd spring-boot3-brick-bootkit-web/vue3 && npm install'."
  fi
else
  warn "npm is not available. Skipping web front-end build."
fi

echo
if [[ $FAILED -eq 1 ]]; then
  echo -e "${RED}Release precheck finished with failures.${NC}"
  exit 1
fi

if [[ $WARNED -eq 1 ]]; then
  echo -e "${YELLOW}Release precheck finished with warnings.${NC}"
  exit 0
fi

echo -e "${GREEN}Release precheck finished successfully.${NC}"
