/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.boot;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.*;
import com.caucho.log.EnvironmentStream;
import com.caucho.log.LogConfig;
import com.caucho.log.RotateStream;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.host.Host;
import com.caucho.server.host.HostConfig;
import com.caucho.server.port.Port;
import com.caucho.server.port.ProtocolDispatchServer;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.*;
import com.caucho.Version;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process responsible for watching a backend server.
 */
public class ResinWatchdogManager extends ProtocolDispatchServer {
  private static L10N _L;
  private static Logger _log;

  private static ResinWatchdogManager _watchdog;

  private Args _args;

  private Lifecycle _lifecycle = new Lifecycle();

  private ResinConfig _resin;
  private String _password;

  private Server _dispatchServer;

  private Port _port;

  private HashMap<String,ResinWatchdog> _activeServerMap
    = new HashMap<String,ResinWatchdog>();

  ResinWatchdogManager(String []argv)
    throws Exception
  {
    _watchdog = this;

    _args = new Args(argv);

    Vfs.setPwd(_args.getRootDirectory());

    boolean isLogNew = ! _args.getLogDirectory().exists();
    Path logPath = _args.getLogDirectory().lookup("watchdog-manager.log");

    RotateStream stream = RotateStream.create(logPath);
    stream.init();
    WriteStream out = stream.getStream();
    out.setDisableClose(true);

    EnvironmentStream.setStdout(out);
    EnvironmentStream.setStderr(out);

    LogConfig log = new LogConfig();
    log.setName("");
    log.setPath(logPath);
    log.init();

    ThreadPool.getThreadPool().setThreadIdleMin(1);
    ThreadPool.getThreadPool().setThreadIdleMax(5);

    _resin = readConfig(_args);

    ResinWatchdog server = _resin.findServer(_args.getServerId());

    if (server != null)
      _password = server.getWatchdogPassword();

    Cluster cluster = new Cluster();
    ClusterServer clusterServer = new ClusterServer(cluster);
    if (server != null)
      clusterServer.setPort(server.getWatchdogPort());
    else
      clusterServer.setPort(6600);

    clusterServer.getClusterPort().setMinSpareListen(1);
    clusterServer.getClusterPort().setMaxSpareListen(2);
      
    _dispatchServer = new Server(clusterServer);

    HostConfig hostConfig = new HostConfig();
    hostConfig.setId("resin-admin");

    hostConfig.init();
    
    _dispatchServer.addHost(hostConfig);
    _dispatchServer.init();
    _dispatchServer.start();

    Host host = _dispatchServer.getHost("resin-admin", 0);

    WebAppConfig webAppConfig = new WebAppConfig();
    webAppConfig.setId("");
    webAppConfig.setRootDirectory(new RawString("watchdog-manager"));

    host.addWebApp(webAppConfig);

    WebApp webApp = host.findWebAppByURI("/");

    ServletMapping servlet = webApp.createServletMapping();

    servlet.setServletName("watchdog");
    servlet.addURLPattern("/watchdog");
    servlet.setServletClass("com.caucho.boot.ResinWatchdogServlet");
    servlet.init();

    webApp.addServletMapping(servlet);
    try {
      host.updateWebAppDeploy("/");
    } catch (Throwable e) {
      log().log(Level.WARNING, e.toString(), e);
    }

    webApp.start();
  }

  static ResinWatchdogManager getWatchdog()
  {
    return _watchdog;
  }

  Path getLogDirectory()
  {
    return _args.getLogDirectory();
  }

  boolean authenticate(String password)
  {
    return (password == _password
	    || password != null && password.equals(_password));
  }

  void startServer(String []argv)
    throws ConfigException
  {
    Args args = new Args(argv);

    String serverId = args.getServerId();

    Vfs.setPwd(_args.getRootDirectory());

    ResinConfig resin = null;

    try {
      resin = readConfig(args);
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    
    ResinWatchdog server = resin.findServer(serverId);

    if (server == null)
      throw new ConfigException(L().l("No matching <server> found for -server '{0}' in '{1}'",
				      serverId, _args.getResinConf()));

    if (args.isVerbose())
      server.setVerbose(args.isVerbose());

    synchronized (_activeServerMap) {
      if (_activeServerMap.get(serverId) != null) {
	throw new IllegalStateException(L().l("-server '{0}' is already running.",
				      serverId));
      }

      server.start(argv, args.getRootDirectory());
      
      _activeServerMap.put(serverId, server);
    }
  }

  void stopServer(String serverId)
  {
    ResinWatchdog server = null;
    
    server = _activeServerMap.get(serverId);

    if (server == null)
      throw new ConfigException(L().l("No matching <server> found for -server '{0}' in {1}",
				      serverId, _args.getResinConf()));
    
    synchronized (_activeServerMap) {
      server = _activeServerMap.remove(serverId);
    }

    log().info(server + " stopping");

    if (server == null)
      throw new IllegalStateException(L().l("-server '{0}' is already stopped.",
					  serverId));

    server.stop();
  }

  void restartServer(String serverId, String []argv)
  {
    ResinWatchdog server = null;
    
    synchronized (_activeServerMap) {
      server = _activeServerMap.remove(serverId);
    }

    if (server != null)
      log().info(server + " stopping");

    server.stop();

    startServer(argv);
  }

  private ResinConfig readConfig(Args args)
    throws Exception
  {
    Config config = new Config();
    config.setIgnoreEnvironment(true);

    Vfs.setPwd(args.getRootDirectory());
    ResinConfig resin = new ResinConfig(this,
					args.getResinHome(),
					args.getRootDirectory());

    config.configure(resin,
		     args.getResinConf(),
		     "com/caucho/server/resin/resin.rnc");

    return resin;
  }

  public static void main(String []argv)
    throws Throwable
  {
    try {
      EnvironmentClassLoader.initializeResinEnvironment();
      
      ResinWatchdogManager manager = new ResinWatchdogManager(argv);
      manager.startServer(argv);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //
  // Utility static methods
  //

  static Path calculateResinHome()
  {
    String resinHome = System.getProperty("resin.home");

    if (resinHome != null) {
      return Vfs.lookup(resinHome);
    }

    // find the resin.jar as described by the classpath
    // this may differ from the value given by getURL() because of
    // symbolic links
    String classPath = System.getProperty("java.class.path");

    if (classPath.indexOf("resin.jar") >= 0) {
      int q = classPath.indexOf("resin.jar") + "resin.jar".length();
      int p = classPath.lastIndexOf(File.pathSeparatorChar, q - 1);

      String resinJar;

      if (p >= 0)
	resinJar = classPath.substring(p + 1, q);
      else
	resinJar = classPath.substring(0, q);

      return Vfs.lookup(resinJar).lookup("../..");
    }

    ClassLoader loader = ClassLoader.getSystemClassLoader();

    URL url = loader.getResource("com/caucho/boot/ResinBoot.class");

    String path = url.toString();

    if (! path.startsWith("jar:"))
      throw new RuntimeException(L().l("Resin/{0}: can't find jar for ResinBoot in {1}",
				       Version.VERSION, path));

    int p = path.indexOf(':');
    int q = path.indexOf('!');

    path = path.substring(p + 1, q);

    Path pwd = Vfs.lookup(path).getParent().getParent();

    return pwd;
  }

  static Path calculateResinRoot(Path resinHome)
  {
    String serverRoot = System.getProperty("server.root");

    if (serverRoot != null)
      return Vfs.lookup(serverRoot);

    return resinHome;
  }

  static String calculateClassPath(Path resinHome)
    throws IOException
  {
    ArrayList<String> classPath = new ArrayList<String>();

    Path javaHome = Vfs.lookup(System.getProperty("java.home"));

    if (javaHome.lookup("lib/tools.jar").canRead())
      classPath.add(javaHome.lookup("lib/tools.jar").getNativePath());
    else if (javaHome.getTail().startsWith("jre")) {
      String tail = javaHome.getTail();
      tail = "jdk" + tail.substring(3);
      Path jdkHome = javaHome.getParent().lookup(tail);

      if (jdkHome.lookup("lib/tools.jar").canRead())
	classPath.add(jdkHome.lookup("lib/tools.jar").getNativePath());
    }
    
    if (javaHome.lookup("../lib/tools.jar").canRead())
      classPath.add(javaHome.lookup("../lib/tools.jar").getNativePath());

    Path resinLib = resinHome.lookup("lib");

    if (resinLib.lookup("pro.jar").canRead())
      classPath.add(resinLib.lookup("pro.jar").getNativePath());
    classPath.add(resinLib.lookup("resin.jar").getNativePath());
    classPath.add(resinLib.lookup("jaxrpc-15.jar").getNativePath());
		  
    String []list = resinLib.list();

    for (int i = 0; i < list.length; i++) {
      if (! list[i].endsWith(".jar"))
	continue;
      
      Path item = resinLib.lookup(list[i]);

      String pathName = item.getNativePath();

      if (! classPath.contains(pathName))
	classPath.add(pathName);
    }

    String cp = "";

    for (int i = 0; i < classPath.size(); i++) {
      if (! "".equals(cp))
	cp += File.pathSeparatorChar;

      cp += classPath.get(i);
    }

    return cp;
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(ResinBoot.class);

    return _L;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(ResinBoot.class.getName());

    return _log;
  }

  static class Args {
    private Path _resinHome;
    private Path _rootDirectory;
    private String []_argv;

    private Path _resinConf;
    private Path _logDirectory;

    private String _serverId = "";

    private boolean _isVerbose;
    
    Args(String []argv)
    {
      _resinHome = calculateResinHome();
      _rootDirectory = calculateResinRoot(_resinHome);

      _argv = argv;

      _resinConf = _resinHome.lookup("conf/resin.conf");
      
      parseCommandLine(argv);
    }

    Path getResinHome()
    {
      return _resinHome;
    }

    Path getRootDirectory()
    {
      return _rootDirectory;
    }

    Path getLogDirectory()
    {
      if (_logDirectory != null)
	return _logDirectory;
      else
	return _rootDirectory.lookup("log");
    }

    Path getResinConf()
    {
      return _resinConf;
    }

    String getServerId()
    {
      return _serverId;
    }

    String []getArgv()
    {
      return _argv;
    }

    boolean isVerbose()
    {
      return _isVerbose;
    }

    private void parseCommandLine(String []argv)
    {
      for (int i = 0; i < argv.length; i++) {
	String arg = argv[i];

	if ("-conf".equals(arg)
	    || "--conf".equals(arg)) {
	  _resinConf = _resinHome.lookup(argv[i + 1]);
	  i++;
	}
	else if ("-resin-home".equals(arg)
		 || "--resin-home".equals(arg)) {
	  _resinHome = Vfs.lookup(argv[i + 1]);
	  i++;
	}
        else if ("-root-directory".equals(arg)
                 || "--root-directory".equals(arg)) {
          _rootDirectory = Vfs.lookup(argv[i + 1]);
          i++;
        }
	else if ("-server-root".equals(arg)
		 || "--server-root".equals(arg)) {
	  _rootDirectory = Vfs.lookup(argv[i + 1]);
	  i++;
	}
        else if ("-log-directory".equals(arg)
                 || "--log-directory".equals(arg)) {
          _logDirectory = _rootDirectory.lookup(argv[i + 1]);
          i++;
        }
	else if ("-server".equals(arg)
		 || "--server".equals(arg)) {
	  _serverId = argv[i + 1];
	  i++;
	}
	else if ("-verbose".equals(arg)
		 || "--verbose".equals(arg)) {
	  _isVerbose = true;
	  Logger.getLogger("").setLevel(Level.FINE);
	}
      }
    }
  }
}
