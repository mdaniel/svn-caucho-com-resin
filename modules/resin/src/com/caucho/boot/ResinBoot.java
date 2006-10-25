/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
import com.caucho.server.resin.ResinELContext;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.el.EL;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ResinBoot is the main bootstrap class for Resin.  It parses the
 * resin.conf and looks for the &lt;server> block matching the -server
 * argument.
 *
 * <h3>Start Modes:</h3>
 *
 * The start modes are DIRECT, START, STOP, RESTART.
 *
 * <ul>
 * <li>DIRECT starts a <server> from the command line
 * <li>START starts a <server> with a Watchdog in the background
 * <li>STOP stop the <server> Resin in the background
 * </ul>
 */
public class ResinBoot {
  private static L10N _L;
  private static Logger _log;

  private String []_argv;

  private Path _resinHome;
  private Path _serverRoot;
  private Path _resinConf;
  private String _serverId = "";
  private boolean _isVerbose;
  private boolean _isResinProfessional;

  private Watchdog _server;

  private StartMode _startMode = StartMode.DIRECT;

  ResinBoot(String []argv)
    throws Exception
  {
    _argv = argv;

    calculateResinHome();
    calculateServerRoot();

    _resinConf = _resinHome.lookup("conf/resin.conf");

    parseCommandLine(argv);

    Vfs.setPwd(_serverRoot);

    if (! _resinConf.canRead())
      throw new ConfigException(L().l("Can't open resin.conf file '{0}'",
                                      _resinConf.getNativePath()));

    // XXX: set _isResinProfessional

    Config config = new Config();

    ResinConfig conf = new ResinConfig(_resinHome, _serverRoot);

    ResinELContext elContext = new ResinBootELContext();

    EL.setEnvironment(elContext, conf.getClassLoader());

    /**
     * XXX: the following setVar calls should not be necessary, but the
     * EL.setEnviornment() call above is not effective:
     */
    config.setVar("java", elContext.getJavaVar());
    config.setVar("resin", elContext.getResinVar());
    config.setVar("server", elContext.getServerVar());

    config.configure(conf, _resinConf, "com/caucho/server/resin/resin.rnc");

    _server = conf.findServer(_serverId);

    if (_server == null)
      throw new ConfigException(L().l("-server '{0}' does not match any defined <server id> in {1}.",
                                      _serverId, _resinConf));

    if (_isVerbose)
      _server.setVerbose(_isVerbose);
  }

  private void calculateResinHome()
  {
    String resinHome = System.getProperty("resin.home");

    if (resinHome != null) {
      _resinHome = Vfs.lookup(resinHome);
      return;
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

      _resinHome = Vfs.lookup(resinJar).lookup("../..");
      return;
    }

    ClassLoader loader = ClassLoader.getSystemClassLoader();

    URL url = loader.getResource("com/caucho/boot/ResinBoot.class");

    String path = url.toString();

    if (! path.startsWith("jar:"))
      throw new RuntimeException(L().l("Can't find jar in {0}", path));

    int p = path.indexOf(':');
    int q = path.indexOf('!');

    path = path.substring(p + 1, q);

    Path pwd = Vfs.lookup(path).getParent().getParent();

    _resinHome = pwd;
  }

  private void calculateServerRoot()
  {
    String serverRoot = System.getProperty("server.root");

    if (serverRoot != null) {
      _serverRoot = Vfs.lookup(serverRoot);
      return;
    }

    _serverRoot = _resinHome;
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
      else if ("-server-root".equals(arg)
	       || "--server-root".equals(arg)) {
	_serverRoot = Vfs.lookup(argv[i + 1]);
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
      }
      else if ("start".equals(arg)) {
	_startMode = StartMode.START;
      }
      else if ("stop".equals(arg)) {
	_startMode = StartMode.STOP;
      }
      else if ("restart".equals(arg)) {
	_startMode = StartMode.RESTART;
      }
      else if ("shutdown".equals(arg)) {
	_startMode = StartMode.SHUTDOWN;
      }
    }
  }

  boolean start()
    throws IOException, InterruptedException
  {
    if (_startMode == StartMode.START) {
      if (_server.startWatchdog(_argv)) {
	System.out.println(L().l("Started server '{0}'", _server.getId()));
      }
      else
	System.out.println(L().l("Can't start server '{0}'", _server.getId()));
      
      return false;
    }
    else if (_startMode == StartMode.RESTART) {
      if (_server.restartWatchdog(_argv)) {
	System.out.println(L().l("Restarted server '{0}'", _server.getId()));
      }
      else
	System.out.println(L().l("Can't restart server '{0}'", _server.getId()));
      
      return false;
    }
    else if (_startMode == StartMode.STOP) {
      if (_server.stopWatchdog())
	System.out.println("Stopped server '" + _server.getId() + "'");
      else
	System.out.println("Can't stop server " + _server.getId());
      
      return false;
    }
    else if (_startMode == StartMode.SHUTDOWN) {
      try {
	_server.shutdown();
      } catch (Exception e) {
	log().log(Level.FINE, e.toString(), e);
      }

      System.out.println("Shutdown watchdog");

      return false;
    }
    else {
      return _server.startSingle(_argv, _serverRoot) != 0;
    }
  }

  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.conf  : alternate configuration file
   * -server web-a     : &lt;server> to start
   * <pre>
   */
  public static void main(String []argv)
  {
    try {
      ResinBoot boot = new ResinBoot(argv);

      while (boot.start()) {
	try {
	  synchronized (boot) {
	    boot.wait(5000);
	  }
	} catch (Throwable e) {
	}
      }
    } catch (ConfigException e) {
      System.out.println(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
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

  enum StartMode {
    DIRECT,
    START,
    STOP,
    RESTART,
    SHUTDOWN
  };

  public class ResinBootELContext
    extends ResinELContext
  {
    public Path getResinHome()
    {
      return _resinHome;
    }

    public Path getRootDirectory()
    {
      return _serverRoot;
    }

    public Path getResinConf()
    {
      return _resinConf;
    }

    public String getServerId()
    {
      return _serverId;
    }

    public boolean isResinProfessional()
    {
      return _isResinProfessional;
    }
  }
}
