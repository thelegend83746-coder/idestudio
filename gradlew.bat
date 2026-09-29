@rem Gradle startup script for Windows
@if "%DEBUG%" == "" @echo off
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
set DEFAULT_JVM_OPTS=
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init
echo ERROR: JAVA_HOME is not set and no java command could be found in your PATH.
goto fail
:init
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -jar "%APP_HOME%/gradle/wrapper/gradle-wrapper.jar" %*
:fail
exit /b 1
