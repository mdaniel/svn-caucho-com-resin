#! /bin/sh
#
# httpd.sh can be called like apachectl
#
# httpd.sh         -- execs the web server in the foreground
# httpd.sh start   -- starts the web server in the background
# httpd.sh stop    -- stops the web server
# httpd.sh restart -- restarts the web server
#
# httpd.sh will return a status code if the wrapper detects an error, but
# some errors, like bind exceptions or Java errors, are not detected.
#
# Customized arguments, e.g. -resin_home or -java_home or -pid.
#
# -pid <pidfile>         -- use a non-default pid file
#                           (useful for multiple servers)
# -java_home <java_home> -- use a non-default Java home
# -stdout <filename>     -- stdout message log
# -stderr <filename>     -- stderr message log
# -native                -- force native threads
# -green                 -- force green threads
# -verbose               -- prints Java arguments before starting.
# -no-auto-restart       -- disable automatic server restart
#                        -- (this only appled to start and restart)
#
# This script can be used as a Linux boot script in init.d.  You'll need to
# configure JAVA_HOME and RESIN_HOME directly.
#
# chkconfig: 345 86 14
# description: Resin is a servlet web server.
# processname: wrapper.pl
#
# To install, you'll need to configure JAVA_HOME and RESIN_HOME and
# copy httpd.sh to /etc/rc.d/init.d as resin.  Then
# use "unix# /sbin/chkconfig resin on"
#
#
# You can predefine JAVA_HOME and RESIN_HOME
#
# JAVA_HOME=/usr/java
# export JAVA_HOME
#
# RESIN_HOME=/usr/local/resin
# export RESIN_HOME
#
# Extra arguments to Java.  If you're passing arguments to the JVM, you'll
# need to use -Jxxx.  For example, args="-J-ms48m".  You can modify
# the pid file with args="-pid server-a.pid"
#
args=
#
# class to start
#
class=com.caucho.server.resin.Resin
#
# name of the server
#
name=httpd
#
# location of perl executable
#
perl=perl

#
# On Linux, you may want to reduce the stack size per thread to allow
# for more threads.  The default 8192k stack per thread gives 255 threads.
# Changing it to 2048k per thread gives 1023 threads
#
# ulimit -s 2048
#
# trace script and simlinks to find thw wrapper
#
script=`/bin/ls -l $0 | awk '{ print $NF; }'`

while test -h "$script"
do
  script=`/bin/ls -l $script | awk '{ print $NF; }'`
done

bin=`dirname $script`

exec $perl $bin/wrapper.pl -chdir -name "$name" -class "$class" $args $*
