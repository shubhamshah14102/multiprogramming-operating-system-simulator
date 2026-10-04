#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mvn -q -DskipTests compile
java -cp target/classes os.benchmark.BenchmarkDriver --output results
echo
echo "Reproduced reports in $ROOT/results"
