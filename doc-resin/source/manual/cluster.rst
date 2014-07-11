Resin Clustering
====================

.. contents:: Page contents


Cluster Overview
-------------------

A Resin cluster is formed when servers connect to seed servers defined
in the configuration file. The seed servers might include all the servers
in a static configuration, or only a subset for a dynamic cluster. A sample
three-server ``resin.cf`` configuration might look like::

  cluster {
    server 192.168.1.10 8080;
    server 192.168.1.11 8080;
    server 192.168.1.12 8080;
  }

That three-server configuration would be started with the ``--conf`` option
to ``resin start`` as follows::

  $ resin start --conf resin.cf

For a dynamic configuration, at least one of the servers will use a default
port. Any Resin that does not match one of the named IP addresses will use
the default port and will try to contact the seed servers::

  cluster {
    server 192.168.1.10 8080;
    server 192.168.1.11 8080;
    server 192.168.1.12 8080;
    server 8080;
  }

Clusters can also be named to separate servers with different functions::

  cluster "web-tier" {
    server 192.168.1.10 8080;
    server 192.168.1.11 8080;
  }
  
  cluster "app-tier" {
    server 192.168.1.20 8180;
    server 192.168.1.20 8180;
  }

For testing, a local cluster can also be formed just using different
ports. The ``resin start`` will start a server for each named port::

  cluster "web-tier" {
     server 8080;
  }
    
  cluster "app-tier" {
     server 8081;
     server 8082;
     server 8083;
   }

Starting it would start all the servers::

  $ resin start --conf resin.cf
  Resin/5.0.0 start with watchdog at 127.0.0.1:6600
  Resin/5.0.0 launching watchdog at 127.0.0.1:6600
    starting *:8080 (web-tier-8080)
    starting *:8081 (app-tier-8081)
    starting *:8082 (app-tier-8082)
    starting *:8083 (app-tier-8083)
      
  $ resin deploy --conf resin.cf --cluster app-tier examples/hello.war
  deployed hello.war to bfs:///system/webapp/deploy/app-tier/host/hello/hello.war




  

