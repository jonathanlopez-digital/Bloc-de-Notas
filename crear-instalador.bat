@echo off
chcp 65001 >nul
cd /d "%~dp0"

set JDK=C:\Program Files\Java\jdk-25.0.2

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
  --app-version 1.0 ^
  --vendor "Jonathan" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --win-per-user-install ^
  %ICONO% ^
  --dest salida

echo.
if exist "salida\BlocNotas-1.0.exe" (
    echo LISTO: salida\BlocNotas-1.0.exe
) else (
    echo Algo fallo. Lee los mensajes de arriba.
)
pause