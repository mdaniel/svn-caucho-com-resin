#!/bin/bash

#RESIN_HOME=C:/caucho/checkout/resin

if test -z "${RESIN_HOME}"; then
  echo RESIN_HOME environment variable not set
  exit
fi

PRO_HOME=${RESIN_HOME}/../pro

if test -n "${JAVA_HOME}"; then
  if test -z "${JAVA_EXE}"; then
    JAVA_EXE=$JAVA_HOME/bin/java
  fi
fi

if test -z "${JAVA_EXE}"; then
  JAVA_EXE=java
fi

if [ "${OSTYPE}" = "cygwin" ]; then
  PATH_SEP=\;
else
  PATH_SEP=\:
fi

QUERCUS_CLASS=com.caucho.quercus.CliQuercus

exec $JAVA_EXE -cp ${RESIN_HOME}/lib/resin.jar${PATH_SEP}${RESIN_HOME}/lib/quercus.jar${PATH_SEP}${RESIN_HOME}/lib/resin-kernel.jar${PATH_SEP}${RESIN_HOME}/lib/pro.jar${PATH_SEP}${PRO_HOME}/lib/pro.jar${PATH_SEP}${PRO_HOME}/lib/license.jar ${QUERCUS_CLASS} $*