@echo off
rem Jalankan Billing PS di Windows. Build otomatis bila jar belum ada (butuh JDK 17+ dan Maven).
cd /d "%~dp0"
if not exist "target\billing-ps.jar" (
  echo Membangun aplikasi, tunggu sebentar...
  call mvn -q -DskipTests package
  if errorlevel 1 goto gagal
)
start "" javaw -jar "target\billing-ps.jar"
exit /b 0
:gagal
echo.
echo Build gagal. Pastikan JDK 17 atau lebih baru dan Apache Maven sudah terpasang.
pause
