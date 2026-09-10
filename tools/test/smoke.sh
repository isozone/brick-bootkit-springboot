#!/usr/bin/env bash
#
# tools 自检脚本：验证 preflight 与 slicer 的核心行为不回归。
# 用法：从仓库根目录执行  ./tools/test/smoke.sh
#
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

PREFLIGHT="$ROOT/tools/preflight/preflight.sh"
SLICER="$ROOT/tools/slicer/slice.sh"

PASS=0
FAIL=0

pass() { echo "  PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL: $1"; FAIL=$((FAIL + 1)); }

# ------------------------------------------------------------------ fixture 生成

# blocked: SB 2.7 + JDK 1.8 → preflight 应报阻断
mkdir -p "$TMP/blocked/src/main/java/com/oldcorp/app"
cat > "$TMP/blocked/pom.xml" <<'POM'
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
  </parent>
  <groupId>com.oldcorp</groupId>
  <artifactId>blocked</artifactId>
  <version>1.0</version>
  <properties><java.version>1.8</java.version></properties>
</project>
POM
cat > "$TMP/blocked/src/main/java/com/oldcorp/app/LegacyApplication.java" <<'JAVA'
package com.oldcorp.app;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class LegacyApplication {
    public static void main(String[] args) {}
}
JAVA

# monolith: 多包 + 循环依赖 + 共享层
mkdir -p "$TMP/monolith/src/main/java/com/fixture"/{common,report,order,user,web}
cat > "$TMP/monolith/pom.xml" <<'POM'
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.fixture</groupId>
  <artifactId>monolith</artifactId>
  <version>1.0</version>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId><version>3.5.5</version></dependency>
  </dependencies>
</project>
POM

cat > "$TMP/monolith/src/main/java/com/fixture/common/Result.java" <<'JAVA'
package com.fixture.common;
public class Result<T> { public static <T> Result<T> ok(T d) { return new Result<>(); } }
JAVA
cat > "$TMP/monolith/src/main/java/com/fixture/common/PageResult.java" <<'JAVA'
package com.fixture.common;
public class PageResult { }
JAVA
cat > "$TMP/monolith/src/main/java/com/fixture/common/BizException.java" <<'JAVA'
package com.fixture.common;
public class BizException extends RuntimeException { public BizException(String m) { super(m); } }
JAVA

cat > "$TMP/monolith/src/main/java/com/fixture/report/ReportService.java" <<'JAVA'
package com.fixture.report;
public interface ReportService { String build(); }
JAVA
cat > "$TMP/monolith/src/main/java/com/fixture/report/ReportServiceImpl.java" <<'JAVA'
package com.fixture.report;
import com.fixture.common.BizException;
public class ReportServiceImpl implements ReportService {
    public String build() { if (false) throw new BizException("x"); return "ok"; }
}
JAVA
cat > "$TMP/monolith/src/main/java/com/fixture/report/ReportExporter.java" <<'JAVA'
package com.fixture.report;
import com.fixture.common.BizException;
public class ReportExporter { }
JAVA

cat > "$TMP/monolith/src/main/java/com/fixture/order/OrderService.java" <<'JAVA'
package com.fixture.order;
public interface OrderService { void submit(); }
JAVA
cat > "$TMP/monolith/src/main/java/com/fixture/order/OrderServiceImpl.java" <<'JAVA'
package com.fixture.order;
import com.fixture.common.BizException;
import com.fixture.user.UserService;
public class OrderServiceImpl implements OrderService {
    private final UserService userService;
    public OrderServiceImpl(UserService u) { this.userService = u; }
    public void submit() { if (userService == null) throw new BizException("x"); }
}
JAVA
cat > "$TMP/monolith/src/main/java/com/fixture/order/OrderEntity.java" <<'JAVA'
package com.fixture.order;
import javax.persistence.Entity;
@Entity
public class OrderEntity { private Long id; }
JAVA

cat > "$TMP/monolith/src/main/java/com/fixture/user/UserService.java" <<'JAVA'
package com.fixture.user;
public interface UserService { long count(); }
JAVA
cat > "$TMP/monolith/src/main/java/com/fixture/user/UserServiceImpl.java" <<'JAVA'
package com.fixture.user;
import com.fixture.common.BizException;
import com.fixture.order.OrderService;
public class UserServiceImpl implements UserService {
    private final OrderService orderService;
    public UserServiceImpl(OrderService o) { this.orderService = o; }
    public long count() { if (orderService == null) throw new BizException("x"); return 0L; }
}
JAVA
cat > "$TMP/monolith/src/main/java/com/fixture/user/UserMapper.java" <<'JAVA'
package com.fixture.user;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface UserMapper { UserEntity findById(Long id); }
class UserEntity { private Long id; }
JAVA

cat > "$TMP/monolith/src/main/java/com/fixture/web/ReportController.java" <<'JAVA'
package com.fixture.web;
import com.fixture.report.ReportService;
import com.fixture.common.Result;
import com.fixture.common.PageResult;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class ReportController {
    private final ReportService reportService;
    public ReportController(ReportService r) { this.reportService = r; }
    public Result<PageResult> list() { return Result.ok(new PageResult()); }
}
JAVA
cat > "$TMP/monolith/src/main/java/com/fixture/web/App.java" <<'JAVA'
package com.fixture.web;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class App {
    public static void main(String[] args) {}
}
JAVA

mkdir -p "$TMP/empty"

# ------------------------------------------------------------------ preflight

echo "[preflight] ok fixture"
set +e
"$PREFLIGHT" "$ROOT/templates/host-minimal" --json > "$TMP/pf_ok.out" 2>&1
code=$?
set -e
if [ "$code" -eq 0 ]; then pass "exit 0"; else fail "exit 0 (got $code)"; fi
if grep -q '"blockCount": 0' "$TMP/pf_ok.out"; then pass "blockCount 0"; else fail "blockCount 0"; fi
if grep -q '"adoptionLevel":' "$TMP/pf_ok.out"; then pass "has adoptionLevel"; else fail "has adoptionLevel"; fi

echo "[preflight] blocked fixture"
set +e
"$PREFLIGHT" "$TMP/blocked" --json > "$TMP/pf_blk.out" 2>&1
code=$?
set -e
if [ "$code" -eq 1 ]; then pass "exit 1"; else fail "exit 1 (got $code)"; fi
if grep -q '"blockCount":' "$TMP/pf_blk.out" && grep -qv '"blockCount": 0' "$TMP/pf_blk.out"; then pass "has blocks"; else fail "has blocks"; fi
if grep -q '"code": "PF002"' "$TMP/pf_blk.out"; then pass "PF002 present"; else fail "PF002 present"; fi
if grep -q '"code": "PF004"' "$TMP/pf_blk.out"; then pass "PF004 present"; else fail "PF004 present"; fi

echo "[preflight] missing dir"
set +e
"$PREFLIGHT" "$TMP/nonexistent" > "$TMP/pf_miss.out" 2>&1
code=$?
set -e
if [ "$code" -eq 2 ]; then pass "exit 2"; else fail "exit 2 (got $code)"; fi

echo "[preflight] json output structure"
set +e
"$PREFLIGHT" "$ROOT/templates/host-minimal" --json > "$TMP/pf_json.out" 2>&1
code=$?
set -e
if [ "$code" -eq 0 ]; then pass "json exit 0"; else fail "json exit 0 (got $code)"; fi
if grep -q '"findings"' "$TMP/pf_json.out"; then pass "json has findings"; else fail "json has findings"; fi
if grep -q '"kitVersion"' "$TMP/pf_json.out"; then pass "json has kitVersion"; else fail "json has kitVersion"; fi

# ------------------------------------------------------------------ slicer

echo "[slicer] monolith"
set +e
"$SLICER" "$TMP/monolith" > "$TMP/sl_mono.out" 2>&1
code=$?
set -e
if [ "$code" -eq 0 ]; then pass "exit 0"; else fail "exit 0 (got $code)"; fi
if grep -q '1\. com\.fixture\.report' "$TMP/sl_mono.out"; then pass "report rank 1"; else fail "report rank 1"; fi
if grep -A 5 '不建议作为切片' "$TMP/sl_mono.out" | grep -q 'com\.fixture\.common'; then pass "common in shared"; else fail "common in shared"; fi
if grep -q 'com\.fixture\.order  <->  com\.fixture\.user' "$TMP/sl_mono.out"; then pass "order-user cycle"; else fail "order-user cycle"; fi

echo "[slicer] empty dir"
set +e
"$SLICER" "$TMP/empty" > "$TMP/sl_empty.out" 2>&1
code=$?
set -e
if [ "$code" -eq 0 ]; then pass "exit 0"; else fail "exit 0 (got $code)"; fi
if grep -q "$TMP/empty" "$TMP/sl_empty.out"; then pass "mentions path"; else fail "mentions path"; fi
if grep -q '候选切片' "$TMP/sl_empty.out"; then fail "no candidates section"; else pass "no candidates section"; fi

echo "[slicer] json mode"
set +e
"$SLICER" "$TMP/monolith" --json > "$TMP/sl_json.out" 2>&1
code=$?
set -e
if [ "$code" -eq 0 ]; then pass "json exit 0"; else fail "json exit 0 (got $code)"; fi
if grep -q '"cyclicPackages"' "$TMP/sl_json.out"; then pass "json has cyclicPackages"; else fail "json has cyclicPackages"; fi
if grep -q '"projectPath"' "$TMP/sl_json.out"; then pass "json has projectPath"; else fail "json has projectPath"; fi
if grep -q '"dependsOn"' "$TMP/sl_json.out"; then pass "json has dependsOn"; else fail "json has dependsOn"; fi

# ------------------------------------------------------------------ scaffold

SCAFFOLD="$ROOT/tools/scaffold/scaffold.sh"

echo "[scaffold] generate from report package"
set +e
"$SCAFFOLD" "$TMP/monolith" com.fixture.report "$TMP/gen-plugin" --force > "$TMP/sc_out.out" 2>&1
code=$?
set -e
if [ "$code" -eq 0 ]; then pass "exit 0"; else fail "exit 0 (got $code)"; fi
if [ -f "$TMP/gen-plugin/pom.xml" ]; then pass "pom.xml exists"; else fail "pom.xml exists"; fi
if [ -f "$TMP/gen-plugin/src/main/java/com/fixture/report/plugin/ReportPluginBootstrap.java" ]; then pass "bootstrap exists"; else fail "bootstrap exists"; fi
if [ -f "$TMP/gen-plugin/src/main/java/com/fixture/report/api/ReportService.java" ]; then pass "contract interface exists"; else fail "contract interface exists"; fi
if grep -q 'String build()' "$TMP/gen-plugin/src/main/java/com/fixture/report/api/ReportService.java"; then pass "contract has build method"; else fail "contract has build method"; fi
if [ -f "$TMP/gen-plugin/src/main/java/com/fixture/report/plugin/ReportServiceImpl.java" ]; then pass "impl stub exists"; else fail "impl stub exists"; fi
if grep -q 'TODO: migrate business logic' "$TMP/gen-plugin/src/main/java/com/fixture/report/plugin/ReportServiceImpl.java"; then pass "impl has TODO"; else fail "impl has TODO"; fi
if [ -f "$TMP/gen-plugin/src/main/resources/META-INF/spring.factories" ]; then pass "spring.factories exists"; else fail "spring.factories exists"; fi

echo "[scaffold] missing package"
set +e
"$SCAFFOLD" "$TMP/monolith" com.nonexistent > "$TMP/sc_miss.out" 2>&1
code=$?
set -e
if [ "$code" -eq 2 ]; then pass "exit 2 for missing package"; else fail "exit 2 for missing package (got $code)"; fi

# ------------------------------------------------------------------ migrate

MIGRATE="$ROOT/tools/migrate/migrate.sh"

echo "[migrate] full pipeline on compliant project"
# 创建一个能通过 preflight 的项目
mkdir -p "$TMP/okproj/src/main/java/com/demo"/{common,report,web}
cat > "$TMP/okproj/pom.xml" <<POM
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.5</version>
  </parent>
  <groupId>com.demo</groupId>
  <artifactId>okproj</artifactId>
  <version>1.0</version>
  <properties><java.version>17</java.version></properties>
</project>
POM
cat > "$TMP/okproj/src/main/java/com/demo/common/Result.java" <<JAVA
package com.demo.common;
public class Result<T> { public static <T> Result<T> ok(T d) { return new Result<>(); } }
JAVA
cat > "$TMP/okproj/src/main/java/com/demo/report/ReportService.java" <<JAVA
package com.demo.report;
public interface ReportService { String build(); }
JAVA
cat > "$TMP/okproj/src/main/java/com/demo/report/ReportServiceImpl.java" <<JAVA
package com.demo.report;
public class ReportServiceImpl implements ReportService {
  public String build() { return "ok"; }
}
JAVA
cat > "$TMP/okproj/src/main/java/com/demo/report/ReportExporter.java" <<JAVA
package com.demo.report;
public class ReportExporter { }
JAVA
cat > "$TMP/okproj/src/main/java/com/demo/web/App.java" <<JAVA
package com.demo.web;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class App { public static void main(String[] args) {} }
JAVA
cat > "$TMP/okproj/src/main/java/com/demo/web/ReportController.java" <<JAVA
package com.demo.web;
import com.demo.report.ReportService;
import com.demo.common.Result;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class ReportController {
  private final ReportService reportService;
  public ReportController(ReportService r) { this.reportService = r; }
  public Result<String> list() { return Result.ok(reportService.build()); }
}
JAVA

set +e
"$MIGRATE" "$TMP/okproj" --auto --force > "$TMP/migrate.out" 2>&1
code=$?
set -e
if [ "$code" -eq 0 ]; then pass "migrate exit 0"; else fail "migrate exit 0 (got $code)"; fi
if grep -q '体检通过' "$TMP/migrate.out"; then pass "migrate preflight passed"; else fail "migrate preflight passed"; fi
if grep -q '候选切片' "$TMP/migrate.out"; then pass "migrate slicer ran"; else fail "migrate slicer ran"; fi
if grep -q '插件骨架已生成' "$TMP/migrate.out"; then pass "migrate scaffold ran"; else fail "migrate scaffold ran"; fi
if grep -q 'report-plugin' "$TMP/migrate.out"; then pass "migrate selected report"; else fail "migrate selected report"; fi

echo "[migrate] blocked project should stop early"
set +e
"$MIGRATE" "$TMP/blocked" --auto > "$TMP/migrate_blk.out" 2>&1
code=$?
set -e
if [ "$code" -eq 1 ]; then pass "migrate stops on blocks"; else fail "migrate stops on blocks (got $code)"; fi
if grep -q '体检未通过' "$TMP/migrate_blk.out"; then pass "migrate reports blocked"; else fail "migrate reports blocked"; fi

# ------------------------------------------------------------------ summary

echo ""
echo "========================================"
echo "Results: $PASS passed, $FAIL failed"
echo "========================================"
[ "$FAIL" -eq 0 ] || exit 1
