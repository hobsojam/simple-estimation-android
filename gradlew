#!/bin/sh

APP_HOME=$(cd "${0%/*}" >/dev/null 2>&1 && pwd -P) || exit
APP_NAME="Gradle"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

warn () { echo "$*"; }
die () { echo; echo "$*"; echo; exit 1; }

cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
  CYGWIN* ) cygwin=true ;;
  Darwin* ) darwin=true ;;
  MSYS* | MINGW* ) msys=true ;;
  NONSTOP* ) nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
  if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
    JAVACMD=$JAVA_HOME/jre/sh/java
  else
    JAVACMD=$JAVA_HOME/bin/java
  fi
  if [ ! -x "$JAVACMD" ] ; then
    die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
  fi
else
  JAVACMD=java
  command -v java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

if [ "$cygwin" = "true" ] || [ "$msys" = "true" ] ; then
  APP_HOME=$(cygpath --path --mixed "$APP_HOME")
  CLASSPATH=$(cygpath --path --mixed "$CLASSPATH")
  JAVACMD=$(cygpath --unix "$JAVACMD")
fi

set -- \
  "-Dorg.gradle.appname=$APP_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"

exec "$JAVACMD" "$@"
