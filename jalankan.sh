#!/usr/bin/env bash
# Jalankan Billing PS di Linux/macOS. Build otomatis bila jar belum ada (butuh JDK 17+ dan Maven).
set -e
cd "$(dirname "$0")"
if [ ! -f target/billing-ps.jar ]; then
  echo "Membangun aplikasi, tunggu sebentar..."
  mvn -q -DskipTests package
fi
exec java -jar target/billing-ps.jar
