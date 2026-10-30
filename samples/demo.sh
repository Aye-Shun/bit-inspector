#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"
printf 'java -cp build BitInspector --width 8 -13\n'
"$JAVA_BIN" -cp build BitInspector --width 8 -13
printf '\njava -cp build BitInspector --width 16 65535\n'
"$JAVA_BIN" -cp build BitInspector --width 16 65535
