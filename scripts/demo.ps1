$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
mvn -q -DskipTests compile
java -cp target/classes os.benchmark.BenchmarkDriver --output results
Write-Host ""
Write-Host "Reproduced reports in $Root\results"
