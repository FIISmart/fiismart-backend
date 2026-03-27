#!/bin/sh
# Maven Wrapper script
set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    DIST_URL=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
fi

DIST_DIR="$HOME/.m2/wrapper/dists/$(basename "$DIST_URL" .zip)"

if [ ! -d "$DIST_DIR" ]; then
    echo "Downloading Maven..."
    mkdir -p "$DIST_DIR"
    TMP_FILE=$(mktemp)
    curl -sL "$DIST_URL" -o "$TMP_FILE"
    unzip -q -o "$TMP_FILE" -d "$HOME/.m2/wrapper/dists/"
    rm -f "$TMP_FILE"
fi

MAVEN_HOME="$(find "$DIST_DIR" -maxdepth 1 -type d -name 'apache-maven-*' | head -1)"
if [ -z "$MAVEN_HOME" ] || [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
    MAVEN_HOME="$(dirname "$(dirname "$(find "$DIST_DIR" -name mvn -type f | head -1)")")"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
