#!/usr/bin/env bash
# =============================================================================
# 一键生成 Brick BootKit 分布式插件模块所需的「自签名 TLS 证书」。
#
# 用法：
#   ./spring-boot3-brick-bootkit-distributed/gen-cert.sh                     # 默认输出到 cert/
#   ./spring-boot3-brick-bootkit-distributed/gen-cert.sh --out /etc/brick/certs
#   ./spring-boot3-brick-bootkit-distributed/gen-cert.sh --ip 10.0.0.21 --ip 127.0.0.1
#   ./spring-boot3-brick-bootkit-distributed/gen-cert.sh --dns worker.example.com
#
# 产物（位于 --out 目录）：
#   server.key   PKCS#8 私钥（PEM）  → WORKER 的 plugin.distributed.tls-private-key
#   server.crt   证书（自签，含 SAN） → WORKER 的 plugin.distributed.tls-cert-chain
#   ca.crt       同一张证书（作为受信任锚点） → HOST 的 plugin.distributed.tls-ca-cert
#
# 说明：
#   - 依赖 openssl，请确保已安装且在 PATH 中（Windows 可用 Git Bash + openssl）。
#   - 自签证书同时充当「信任 CA」，开发者/内网自建环境最省事；生产建议改用受信 CA 签发。
#   - 私钥默认不含密码，方便 gRPC 服务端启动读取；如要加密可自行 `openssl rsa -aes256`.
# =============================================================================
set -euo pipefail

OUT_DIR="cert"
DAYS=3650
KEYSIZE=2048
CN="brick-bootkit"
IPS=(${CERTS_IP:-127.0.0.1})
DNS=()
C=CN
O="Brick BootKit"
ST="${CERTS_STATE:-}"

usage() {
  sed -n '2,30p' "$0" | sed 's/^ *# \{0,1\}//'
  cat <<'EOF'

  -h, --help        显示本帮助
  -o, --out <dir>   证书输出目录（默认 cert）
  -d, --days <n>    证书有效期天数（默认 3650）
  -k, --keysize <n> RSA 密钥位数（默认 2048）
  -c, --cn <name>   证书 CN（默认 brick-bootkit）
      --ip <ip>     追加一个 SAN IP（可多次），默认 [127.0.0.1]
      --dns <name>  追加一个 SAN DNS（可多次）
      --state <s>   Subject 的 ST 字段（可选）
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    -o|--out) OUT_DIR="$2"; shift 2 ;;
    -d|--days) DAYS="$2"; shift 2 ;;
    -k|--keysize) KEYSIZE="$2"; shift 2 ;;
    -c|--cn) CN="$2"; shift 2 ;;
    --ip) IPS+=("$2"); shift 2 ;;
    --dns) DNS+=("$2"); shift 2 ;;
    --state) ST="$2"; shift 2 ;;
    *) echo "未知参数: $1" >&2; usage >&2; exit 1 ;;
  esac
done

command -v openssl >/dev/null 2>&1 || {
  echo "ERROR: 未找到 openssl，请先安装（Windows 可用 Git Bash 内置 openssl）。" >&2
  exit 1
}

mkdir -p "$OUT_DIR"
cd "$OUT_DIR"

KEY="server.key"
CRT="server.crt"
CA="ca.crt"
SUBJ="/C=${C}/O=${O}/CN=${CN}"
[[ -n "$ST" ]] && SUBJ="/C=${C}/ST=${ST}/O=${O}/CN=${CN}"

# 组装 SAN：IP（去重）+ DNS（去重）
san=""
declare -A seen
for ip in "${IPS[@]}"; do
  [[ -n "$ip" ]] || continue
  if [[ -z "${seen["ip:$ip"]:-}" ]]; then
    seen["ip:$ip"]=1
    san="${san}IP:${ip},"
  fi
done
for d in "${DNS[@]}"; do
  [[ -n "$d" ]] || continue
  if [[ -z "${seen["dns:$d"]:-}" ]]; then
    seen["dns:$d"]=1
    san="${san}DNS:${d},"
  fi
done
san="${san%,}"   # 去掉末尾逗号
[[ -n "$san" ]] || san="IP:127.0.0.1"

echo ">>> 生成自签名证书（有效 ${DAYS} 天，RSA ${KEYSIZE}）"
echo "    CN        : ${CN}"
echo "    SAN       : ${san}"
echo "    输出目录  : $(pwd)"
echo

# 1) 私钥（PKCS#8 PEM）
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:"$KEYSIZE" -out "$KEY"
# 2) 自签名证书（叶子证书，同时被当作根锚点），带 SAN
openssl req -new -key "$KEY" -subj "$SUBJ" \
  -addext "subjectAltName=${san}" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,digitalSignature,keyEncipherment,keyCertSign" \
  -addext "extendedKeyUsage=serverAuth,clientAuth" \
  -x509 -days "$DAYS" -sha256 -out "$CRT"
# 3) 同一张证书当信任锚点（HOST 侧校验 server 证书用）
cp "$CRT" "$CA"

chmod 600 "$KEY"

echo
echo ">>> 生成完成 ✅"
echo "    私钥        : $KEY  (WORKER  tls-private-key)"
echo "    证书        : $CRT  (WORKER  tls-cert-chain)"
echo "    信任 CA     : $CA   (HOST     tls-ca-cert)"
echo
echo ">>> 建议配置："
echo "Worker 执行节点 application.yml:"
echo "  plugin.distributed.tls-enabled: true"
echo "  plugin.distributed.tls-cert-chain:  <abspath>/$CRT"
echo "  plugin.distributed.tls-private-key: <abspath>/$KEY"
echo "  plugin.distributed.auth-token: $(openssl rand -hex 16)"
echo
echo "Host 宿主节点 application.yml:"
echo "  plugin.distributed.tls-enabled: true"
echo "  plugin.distributed.tls-ca-cert:  <abspath>/$CA"
echo "  plugin.distributed.auth-token:   <同上，必须与 Worker 一致>"