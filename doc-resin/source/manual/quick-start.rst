Resin Quick Start
====================

.. contents:: Page contents


Installing Resin
-------------------

Download Resin from http://caucho.com/download and extract it to any directory.

Starting Resin
-----------------

Start up Resin with::

  $ bin/resin start
  Resin/5.0.0 start with watchdog at 127.0.0.1:6600
  Resin/5.0.0 launching watchdog at 127.0.0.1:6600
    starting *:8080 (cluster-8080)

The default work directory is ``/tmp/resin``, which can be changed
with the ``-d`` option.  The default http port is 8080, which can be
changed with the ``-p`` option or in a config file.  The default
port for the watchdog server is 6600.

Resin is now running, but it doesn't any web-apps deployed to it
yet. To deploy the example hello service distributed with Resin::

  $ bin/resin deploy examples/hello.war
    deployed hello.war to bfs:///system/webapp/deploy/cluster/host/hello/hello.war
\
  .. TIP::
    You can combine the start and deploy commands together::

        $ bin/resin start --deploy examples/hello.war

The web-app is now accessible from HTTP.  You can call the
servlet using ``curl`` or a browser::
  
  $ curl http://localhost:8080/hello/hello-servlet
  <h1>hello, world</h1>

To shutdown Resin, issue the ``shutdown`` command::

  $ bin/resin shutdown  

Simple Web-Application
------------------------

Create a JSP file named source/hello.jsp::
  
  /hello.jsp: <%= "hello, " + "world" %>

Then create a .war file (web-application archive) with the ``jar`` command::

 $ jar -cf hello.war -C source hello.jsp

Then deploy the war as the "root" web-app, the web-app without an extra
URL prefix::

 $ resin start
  Resin/5.0.0 start with watchdog at 127.0.0.1:6600
  Resin/5.0.0 launching watchdog at 127.0.0.1:6600
    starting *:8080 (cluster-8080)
      
 $ resin deploy --name ROOT hello.war
 deployed hello.war to bfs:///system/webapp/deploy/cluster/host/ROOT/ROOT.war
  
 $ curl http://localhost:8080/hello.jsp
 /hello.jsp: hello, world
 
  
  