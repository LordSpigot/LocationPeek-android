@echo off
setlocal
set GRADLE_VERSION=8.9
set TMPDIR=%TEMP%\locationpeek-gradle
if not exist "%TMPDIR%" mkdir "%TMPDIR%"
set ZIP=%TMPDIR%\gradle-%GRADLE_VERSION%-bin.zip
set DIR=%TMPDIR%\gradle-%GRADLE_VERSION%

echo [1/3] Gradle %GRADLE_VERSION% wird geladen...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
if errorlevel 1 goto :error

echo [2/3] Gradle wird entpackt...
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path '%DIR%') { Remove-Item -Recurse -Force '%DIR%' }; Expand-Archive -Force '%ZIP%' '%TMPDIR%'"
if errorlevel 1 goto :error

echo [3/3] Gradle Wrapper wird erzeugt...
"%DIR%\bin\gradle.bat" wrapper --gradle-version %GRADLE_VERSION%
if errorlevel 1 goto :error

echo.
echo Fertig. Jetzt den Ordner LocationPeek in Android Studio oeffnen.
pause
exit /b 0

:error
echo.
echo Vorbereitung fehlgeschlagen. Pruefe Internetverbindung und versuche es erneut.
pause
exit /b 1
