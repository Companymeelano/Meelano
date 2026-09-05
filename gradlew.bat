@rem
@rem Gradle startup script for Windows.
@rem Downloads gradle-wrapper.jar automatically when it is not present.
@rem
@if "%DEBUG%"=="" @echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
set WRAPPER_PROPERTIES=%APP_HOME%gradle\wrapper\gradle-wrapper.properties

if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
)

if not exist "%WRAPPER_JAR%" (
  echo gradle-wrapper.jar is missing - downloading it...
  powershell -NoProfile -Command ^
    "$p = Get-Content '%WRAPPER_PROPERTIES%' | Select-String -Pattern 'gradle-([0-9.]+)-bin.zip';" ^
    "$v = if ($p) { $p.Matches[0].Groups[1].Value } else { '9.3.1' };" ^
    "New-Item -ItemType Directory -Force -Path '%APP_HOME%gradle\wrapper' | Out-Null;" ^
    "Invoke-WebRequest -Uri \"https://raw.githubusercontent.com/gradle/gradle/v$v/gradle/wrapper/gradle-wrapper.jar\" -OutFile '%WRAPPER_JAR%'"
)

if not exist "%WRAPPER_JAR%" (
  echo ERROR: could not obtain gradle-wrapper.jar. Run "gradle wrapper" once with a local Gradle install.
  exit /b 1
)

"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
