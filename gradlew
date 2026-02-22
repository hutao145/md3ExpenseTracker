#!/bin/sh

DIRNAME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="$DIRNAME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
  echo "ERROR: gradle-wrapper.jar not found at $CLASSPATH"
  exit 1
fi

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
