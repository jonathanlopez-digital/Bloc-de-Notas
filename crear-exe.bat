@echo off
chcp 65001 >nul
cd /d "%~dp0"

set JDK=C:\Program Files\Java\jdk-25.0.2
set JAVAFXSDK=C:\Users\Jonathan López\Documents\Librerias\javafx-sdk-25.0.5
set JAVAFX=%JAVAFXSDK%\lib

if not exist "%JDK%\bin\jpackage.exe" (
    echo ERROR: no se encontro jpackage en "%JDK%\bin"
    pause
    exit /b 1
)

if not exist "dist\Bloc-de-Notas.jar" (
    echo ERROR: no existe dist\Bloc-de-Notas.jar. Haz Clean and Build.
    pause
    exit /b 1
)

if not exist "%JAVAFXSDK%\bin\glass.dll" (
    echo ERROR: no se encontraron las DLL de JavaFX en "%JAVAFXSDK%\bin"
    pause
    exit /b 1
)

if exist instalador rmdir /s /q instalador

set ICONO=
if exist "icono.ico" set ICONO=--icon icono.ico

"%JDK%\bin\jpackage" --type app-image --name BlocNotas ^
  --input dist --main-jar Bloc-de-Notas.jar ^
  --main-class main.java.jonathan.blocnotas.Launcher ^
  --module-path "%JDK%\jmods;%JAVAFX%" ^
  --add-modules java.base,java.desktop,java.logging,java.prefs,java.xml,jdk.unsupported,javafx.controls,javafx.fxml,javafx.graphics,javafx.base ^
  --java-options "--enable-native-access=javafx.graphics" ^
  %ICONO% ^
  --dest instalador

if not exist "instalador\BlocNotas\BlocNotas.exe" (
    echo.
    echo Algo fallo. Lee los mensajes de arriba.
    pause
    exit /b 1
)

echo Copiando DLL de JavaFX...
xcopy /y /q "%JAVAFXSDK%\bin\*.dll" "instalador\BlocNotas\runtime\bin\"

echo.
echo LISTO: instalador\BlocNotas\BlocNotas.exe
pause