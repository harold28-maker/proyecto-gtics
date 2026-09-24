@echo off
setlocal
set "MAVEN_VERSION=3.9.11"
set "BASE=%~dp0.mvn\apache-maven-%MAVEN_VERSION%"
set "MVN=%BASE%\bin\mvn.cmd"

if exist "%MVN%" goto run

echo Maven no se encontro dentro del proyecto. Descargando Apache Maven %MAVEN_VERSION%...
set "ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%ZIP%' } catch { Write-Error $_; exit 1 }"
if errorlevel 1 goto error

powershell -NoProfile -ExecutionPolicy Bypass -Command "try { if (Test-Path '%BASE%') { Remove-Item -Recurse -Force '%BASE%' }; Expand-Archive -Path '%ZIP%' -DestinationPath '%~dp0.mvn' -Force } catch { Write-Error $_; exit 1 }"
if errorlevel 1 goto error

if not exist "%MVN%" goto error

:run
call "%MVN%" %*
exit /b %ERRORLEVEL%

:error
echo.
echo No se pudo preparar Maven. Verifica que Java 17 e Internet esten disponibles.
exit /b 1
