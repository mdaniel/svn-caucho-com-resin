#! /bin/sh
#
# resin.sh can be called like apachectl
#
# resin.sh         -- execs resin in the foreground
# resin.sh start   -- starts resin in the background
# resin.sh stop    -- stops resin
# resin.sh restart -- restarts resin
#
# resin.sh will return a status code if the wrapper detects an error, but
# some errors, like bind exceptions or Java errors, are not detected.
#
# chkconfig: 345 86 14
# description: Resin is a Java application server
# processname: java
#
# To install, you'll need to configure JAVA_HOME and RESIN_HOME and
# copy httpd.sh to /etc/rc.d/init.d as resin.  Then
# use "unix# /sbin/chkconfig resin on"
#
# trace script and simlinks to find thw wrapper
#
java=java

script=`/bin/ls -l $0 | awk '{ print $NF; }'`

while test -h "$script"
do
  script=`/bin/ls -l $script | awk '{ print $NF; }'`
done

bin=`dirname $script`

exec $java -jar $bin/../lib/resin.jar $*
