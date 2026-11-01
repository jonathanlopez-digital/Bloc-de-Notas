@echo off
chcp 65001 >nul
cd /d "%~dp0"

set JDK=C:\Program Files\Java\jdk-25.0.2
set VERSION=1.1
set PATH=%PATH%;C:\Program Files (x86)\WiX Toolset v3.14\bin

if not exist "instalador\BlocNotas\BlocNotas.exe" (
    echo ERROR: primero ejecuta crear-exe.bat
    pause
    exit /b 1
)

if exist salida rmdir /s /q salida

set ICONO=
if exist "icono.ico" set ICONO=--icon icono.ico

"%JDK%\bin\jpackage" --type exe ^
  --app-image instalador\BlocNotas ^
  --name BlocNotas ^
  --app-version %VERSION% ^
  --vendor "Jonathan" ^
  --win-upgrade-uuid 6f1b7c2e-3a54-4d1e-9b8a-2c7d5e0f4a31 ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --win-per-user-install ^
  %ICONO% ^
  --dest salida

echo.
if exist "salida\BlocNotas-%VERSION%.exe" (
    echo LISTO: salida\BlocNotas-%VERSION%.exe
) else (
    echo Algo fallo. Lee los mensajes de arriba.
)
pause