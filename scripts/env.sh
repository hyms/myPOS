#!/usr/bin/env bash
# scripts/env.sh
# Source this file to load the build environment for the myPOS Android project.
#   source scripts/env.sh
# All variables can be overridden by exporting them before sourcing.

# ---- JDK 17 (Temurin, auto-provisioned by Gradle) ----
export JAVA_HOME="${JAVA_HOME:-/home/helier/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2}"
export ORG_GRADLE_JAVA_INSTALLATIONS_PATHS="${ORG_GRADLE_JAVA_INSTALLATIONS_PATHS:-$JAVA_HOME}"

# ---- Android SDK ----
export ANDROID_HOME="${ANDROID_HOME:-/home/helier/Android/Sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

# ---- PATH ----
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

# ---- Sanity check (warn, do not abort) ----
if [ ! -x "$JAVA_HOME/bin/javac" ]; then
  echo "WARNING: javac not found in \$JAVA_HOME ($JAVA_HOME)" >&2
fi
if [ ! -d "$ANDROID_HOME/platforms/android-36" ]; then
  echo "WARNING: Android platform 36 not found in \$ANDROID_HOME ($ANDROID_HOME)" >&2
fi
if [ ! -d "$ANDROID_HOME/ndk/27.1.12297006" ]; then
  echo "WARNING: NDK 27.1.12297006 not found in \$ANDROID_HOME ($ANDROID_HOME)" >&2
fi

echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_HOME=$ANDROID_HOME"
