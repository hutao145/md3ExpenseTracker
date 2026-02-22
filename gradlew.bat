@echo off
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
  echo ERROR: gradle-wrapper.jar not found at "%CLASSPATH%"
  exit /b 1
)

java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
