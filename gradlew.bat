@echo off
set "APP_HOME=%~dp0"
set "APP_NAME=Gradle"
set "CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"
java -jar "%CLASSPATH%" %*
