#!/bin/sh

APXS=
RESIN_HOME=
DEBUG=

usage() {
  echo "usage: install.sh [flags]"
  echo "flags:"
  echo "  -help                    : this usage message"
  echo "  -conf <conf>             : apache config"
  echo "  -libexec <dir>           : libexec directory"
  echo "  -resin_home <resin_home> : resin home"
}

DEBUG=
while test "$#" -ne 0 ; do
    case "$1" in

    # Documented arguments
    -h | -help)       SHOW_HELP=true; shift;;
    -conf) CONF="$2"; shift 2;;
    -libexec) LIBEXECDIR="$2"; shift 2;;
    -resin_home) RESIN_HOME="$2"; shift 2;;

    *)   shift ; break;
    esac
done

if test ! -r "$CONF"; then
  echo "Can't find valid Apache configuration \"$CONF\""
  exit 1
fi

#if test -d "$LIBEXECDIR"; then
#  echo cp mod_caucho.so $LIBEXECDIR
#  cp mod_caucho.so $LIBEXECDIR
#elif test -z "$LIBEXECDIR"; then
#  LIBEXECDIR=`pwd`
#else
#  echo "Can't find valid Apache module directory in \"$LIBEXECDIR\""
#  exit 1
#fi

grep mod_caucho $CONF >/dev/null 2>/dev/null
if test "$?" != 0; then
  cat >>$CONF <<EOF
#
# mod_caucho Resin Configuration
#

LoadModule caucho_module $LIBEXECDIR/mod_caucho.so
ResinConfigServer localhost 6802
CauchoStatus yes
EOF
fi

