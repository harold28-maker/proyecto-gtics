#!/bin/sh
set -eu
MAVEN_VERSION=3.9.11
BASE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/.mvn/apache-maven-$MAVEN_VERSION"
MVN="$BASE/bin/mvn"
if [ ! -x "$MVN" ]; then
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
  TMP="${TMPDIR:-/tmp}/apache-maven-$MAVEN_VERSION-bin.zip"
  echo "Descargando Apache Maven $MAVEN_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$TMP"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$URL" -O "$TMP"
  else
    echo "Necesitas curl o wget para descargar Maven." >&2
    exit 1
  fi
  mkdir -p "$(dirname "$BASE")"
  rm -rf "$BASE"
  unzip -q "$TMP" -d "$(dirname "$BASE")"
fi
exec "$MVN" "$@"
