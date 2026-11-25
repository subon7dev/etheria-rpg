#!/bin/bash
set -e

PROJECT_DIR="/data/user/0/ai.antigem/files/home/projects/Etheria"
ANDROID_DIR="$HOME/android-sdk"

JVM_DIR=$(ls /data/user/0/ai.antigem/files/usr/lib/jvm/ 2>/dev/null | head -1)
if [ -n "$JVM_DIR" ]; then
    export JAVA_HOME="/data/user/0/ai.antigem/files/usr/lib/jvm/$JVM_DIR"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: openjdk-17 is required. Install it first with user approval."
    exit 1
fi

export ANDROID_HOME="$ANDROID_DIR"
export ANDROID_SDK_ROOT="$ANDROID_DIR"
export PATH="$ANDROID_DIR/cmdline-tools/latest/bin:$ANDROID_DIR/platform-tools:$PATH"

if command -v sdkmanager >/dev/null 2>&1; then
    yes | sdkmanager --licenses >/dev/null 2>&1 || true
    sdkmanager "platforms;android-35" "build-tools;34.0.0"
fi

python3 "$PROJECT_DIR/assets/generate_assets.py"

EXTRA_ARGS=""
if [ "$(uname -m)" = "aarch64" ] && [ -x "/data/user/0/ai.antigem/files/usr/bin/aapt2" ]; then
    EXTRA_ARGS="-Pandroid.aapt2FromMavenOverride=/data/user/0/ai.antigem/files/usr/bin/aapt2"
fi

cd "$PROJECT_DIR"
chmod +x gradlew
./gradlew assembleDebug assembleRelease $EXTRA_ARGS --no-daemon
