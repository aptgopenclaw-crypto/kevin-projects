#!/usr/bin/env bash
# 決標歷史資料補充 — 逐日執行
# 用法：./kk.sh 2026-01-01 2026-05-14

set -euo pipefail

FROM="${1:-}"
TO="${2:-}"

if [[ -z "$FROM" || -z "$TO" ]]; then
    echo "用法：$0 <from-date> <to-date>"
    echo "範例：$0 2026-01-01 2026-05-14"
    exit 1
fi

JAR="$(dirname "$0")/target/fix-data-1.0.0.jar"

if [[ ! -f "$JAR" ]]; then
    echo "[ERROR] 找不到 $JAR，請先執行 mvn clean package -DskipTests"
    exit 1
fi

# 逐日產生日期清單並執行
current="$FROM"
to_num=$(date -d "$TO" +%Y%m%d)
while [[ $(date -d "$current" +%Y%m%d) -le $to_num ]]; do
    echo ""
    echo "================================================================"
    echo "  執行日期：$current"
    echo "================================================================"
    java -jar "$JAR" --from="$current" --to="$current"
    echo "[kk.sh] $current 完成"

    # 日期 +1 天（macOS 用 gdate，Linux 用 date）
    if date --version &>/dev/null 2>&1; then
        current=$(date -d "$current + 1 day" +%Y-%m-%d)
    else
        current=$(gdate -d "$current + 1 day" +%Y-%m-%d)
    fi
done

echo ""
echo "================================================================"
echo "  全部完成：$FROM ~ $TO"
echo "================================================================"
