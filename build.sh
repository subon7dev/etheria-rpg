#!/bin/bash
set -e

PROJECT_DIR="/data/user/0/ai.antigem/files/home/projects/Etheria"

if ! command -v java >/dev/null 2>&1; then
    JVM_DIR=$(ls /data/user/0/ai.antigem/files/usr/lib/jvm/ 2>/dev/null | head -1)
    if [ -n "$JVM_DIR" ]; then
        export JAVA_HOME="/data/user/0/ai.antigem/files/usr/lib/jvm/$JVM_DIR"
        export PATH="$JAVA_HOME/bin:$PATH"
    else
        echo "ERROR: Java 17 not found."
        exit 1
    fi
fi

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

EXTRA_ARGS=""
if [ "$(uname -m)" = "aarch64" ] && [ -x "/data/user/0/ai.antigem/files/usr/bin/aapt2" ]; then
    EXTRA_ARGS="-Pandroid.aapt2FromMavenOverride=/data/user/0/ai.antigem/files/usr/bin/aapt2"
fi

cd "$PROJECT_DIR"
./gradlew assembleDebug assembleRelease $EXTRA_ARGS --no-daemon --stacktrace
