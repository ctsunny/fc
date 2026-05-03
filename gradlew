#!/bin/sh
# Copyright © 2015-2021 the original authors.
# Licensed under the Apache License, Version 2.0

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
MAX_FD=maximum

warn() { echo "$*"; } >&2
die() { echo; echo "$*"; echo; exit 1; } >&2

cygwin=false; msys=false; darwin=false; nonstop=false
case "$(uname)" in
  CYGWIN*)  cygwin=true  ;;
  Darwin*)  darwin=true  ;;
  MSYS*|MINGW*) msys=true ;;
  NONSTOP*) nonstop=true ;;
esac

app_path=$0
while [ -h "$app_path" ]; do
  ls=$(ls -ld "$app_path")
  link=${ls#*' -> '}
  case $link in
    /*)  app_path=$link ;;
    *)   app_path=$(dirname "$app_path")/$link ;;
  esac
done
APP_HOME=$(cd "$(dirname "$app_path")" && pwd -P) || exit

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    JAVACMD=$JAVA_HOME/jre/sh/java
  else
    JAVACMD=$JAVA_HOME/bin/java
  fi
  if [ ! -x "$JAVACMD" ]; then
    die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
  fi
else
  JAVACMD=java
  which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command was found."
fi

if ! "$cygwin" && ! "$darwin" && ! "$nonstop"; then
  case $MAX_FD in
    max*) MAX_FD=$(ulimit -H -n) || warn "Could not query max file descriptor limit" ;;
  esac
  case $MAX_FD in
    ''|soft) :;;
    *) ulimit -n "$MAX_FD" || warn "Could not set max file descriptor limit to $MAX_FD" ;;
  esac
fi

set -- \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"

eval "set -- $(
  printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
  xargs -n1 |
  sed 's~[^a-zA-Z0-9/=@_.-]~\\&~g;' |
  tr '\n' ' '
)" '"$@"'

exec "$JAVACMD" "$@"
