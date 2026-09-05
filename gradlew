#!/bin/sh
#
# Gradle start up script for POSIX systems.
#
# This variant self-heals: if gradle/wrapper/gradle-wrapper.jar is not present
# in the checkout (it is a binary and is often not committed), it is downloaded
# from the official Gradle distribution services before the build starts.
#

set -e

APP_HOME=$(cd "$(dirname "$0")" >/dev/null && pwd -P)
WRAPPER_DIR="$APP_HOME/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
WRAPPER_PROPERTIES="$WRAPPER_DIR/gradle-wrapper.properties"

die() {
    echo "$*" 1>&2
    exit 1
}

# ---- locate java -------------------------------------------------------------
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=$(command -v java || true)
fi
[ -n "$JAVACMD" ] || die "ERROR: JAVA_HOME is not set and no 'java' command could be found.
Install JDK 17 (e.g. 'sudo apt install openjdk-17-jdk') or set JAVA_HOME."

# ---- fetch the wrapper jar if it is missing ----------------------------------
if [ ! -f "$WRAPPER_JAR" ]; then
    GRADLE_VERSION=$(sed -n 's#.*gradle-\([0-9.]*\)-bin\.zip.*#\1#p' "$WRAPPER_PROPERTIES")
    [ -n "$GRADLE_VERSION" ] || GRADLE_VERSION="9.3.1"
    WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"

    echo "gradle-wrapper.jar is missing — downloading it for Gradle ${GRADLE_VERSION}…"
    mkdir -p "$WRAPPER_DIR"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL" || true
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL" || true
    fi

    if [ ! -s "$WRAPPER_JAR" ]; then
        rm -f "$WRAPPER_JAR"
        die "ERROR: could not download gradle-wrapper.jar.
Either connect to the internet, or run 'gradle wrapper --gradle-version ${GRADLE_VERSION}'
once with a locally installed Gradle to generate it."
    fi
fi

CLASSPATH="$WRAPPER_JAR"

exec "$JAVACMD" \
    ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \
    "-Dorg.gradle.appname=$(basename "$0")" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
