#!/usr/bin/env bash
# scripts/build-android.sh
# Build the myPOS Android project with a consistent environment.
#
# Usage:
#   scripts/build-android.sh                  # assembleDebug, arm64-v8a
#   scripts/build-android.sh release arm64-v8a,armeabi-v7a
#   scripts/build-android.sh installDebug
#
# Available tasks: assembleDebug, installDebug, assembleRelease, installRelease, clean

set -euo pipefail

# ---- Resolve project root (script dir/..) ----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android"

# ---- Load environment ----
# shellcheck source=env.sh
source "$SCRIPT_DIR/env.sh"

# ---- Args ----
TASK="${1:-assembleDebug}"
ARCHS="${2:-arm64-v8a}"

# ---- Ensure local.properties exists ----
LOCAL_PROPS="$ANDROID_DIR/local.properties"
if [ ! -f "$LOCAL_PROPS" ] || ! grep -q "^sdk.dir=" "$LOCAL_PROPS"; then
  echo "sdk.dir=$ANDROID_HOME" > "$LOCAL_PROPS"
  echo "Created $LOCAL_PROPS"
fi

# ---- Stop any stale daemons that might hold the old toolchain cache ----
cd "$ANDROID_DIR"
./gradlew --stop > /dev/null 2>&1 || true

# ---- Build ----
echo "==> Building: $TASK  (arch: $ARCHS)"
./gradlew "$TASK" \
  -x lint -x test \
  --configure-on-demand \
  --build-cache \
  -PreactNativeDevServerPort=8081 \
  -PreactNativeArchitectures="$ARCHS" \
  --no-daemon

echo "==> Done."
