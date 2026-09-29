#!/usr/bin/env sh
APP_BASE_NAME=`basename "$0"`
DIRNAME=`dirname "$0"`
if [ "$DIRNAME" = "" ]; then
    DIRNAME=.
fi
APP_HOME=`cd "$DIRNAME" && pwd`
DEFAULT_JVM_OPTS=""
JAVACMD="java"
which java >/dev/null 2>&1 || {
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
}
exec "$JAVACMD" "-Dorg.gradle.appname=$APP_BASE_NAME" -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
