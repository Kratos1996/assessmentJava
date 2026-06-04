@echo off
setlocal

rem Resolve the directory containing this script.
set "MAVEN_PROJECTBASEDIR=%~dp0"
if not defined MAVEN_PROJECTBASEDIR set "MAVEN_PROJECTBASEDIR=%cd%"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" (
  echo Error: Maven wrapper JAR not found at "%WRAPPER_JAR%"
  exit /b 1
)

set "JAVA_EXE=java"
if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

"%JAVA_EXE%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
