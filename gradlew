#!/usr/bin/env bash
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="Gradle"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec java -jar "$CLASSPATH" "$@"
