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
import com.caucho.el.EL;
import com.caucho.util.CompileException;
import com.caucho.loader.Environment;
import com.caucho.server.resin.ResinELContext;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.Version;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.lang.management.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.*;

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
  private Path _rootDirectory;
  private Path _logDirectory;
  private Path _resinConf;
  private String _serverId = "";
  private boolean _isVerbose;
  private boolean _isResinProfessional;

  private ResinWatchdog _server;

  private StartMode _startMode = StartMode.DIRECT;

  ResinBoot(String []argv)
    throws Exception
  {
    _argv = fillArgv(argv);
    
    calculateResinHome();
    
    ClassLoader loader = ProLoader.create(_resinHome);
    if (loader != null) {
      System.setProperty("resin.home", _resinHome.getNativePath());
      
      Thread.currentThread().setContextClassLoader(loader);

      Vfs.initJNI();

      _resinHome = Vfs.lookup(_resinHome.getFullPath());
    }
    
    Environment.init();

    calculateRootDirectory();

    parseCommandLine(argv);

    // required for license check
    System.setProperty("resin.home", _resinHome.getNativePath());

    if (_resinConf == null) {
      _resinConf = _rootDirectory.lookup("conf/resin.conf");

      if (!_resinConf.exists()) {
        Path resinHomeConf = _resinHome.lookup("conf/resin.conf");

        if (resinHomeConf.exists())
          _resinConf = resinHomeConf;
      }
    }

    // watchdog/0210
    // Vfs.setPwd(_rootDirectory);

    if (! _resinConf.canRead()) {
      throw new ConfigException(L().l("Resin/{0} can't open configuration file '{1}'",
                                      Version.VERSION, _resinConf.getNativePath()));
    }

    // XXX: set _isResinProfessional

    Config config = new Config();
    config.setIgnoreEnvironment(true);

    ResinWatchdogManager manager = null;
    ResinConfig conf = new ResinConfig(manager, _resinHome, _rootDirectory);

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
      throw new ConfigException(L().l("Resin/{0}: -server '{1}' does not match any defined <server>\nin {2}.",
                                      Version.VERSION, _serverId, _resinConf));

    Path logDirectory = getLogDirectory();
    if (! logDirectory.exists()) {
      logDirectory.mkdirs();

      if (_server.getUserName() != null)
	logDirectory.changeOwner(_server.getUserName());
      
      if (_server.getGroupName() != null)
	logDirectory.changeOwner(_server.getGroupName());
    }
    
    if (_isVerbose)
      _server.setVerbose(_isVerbose);
  }

  private String []fillArgv(String []argv)
  {
    ArrayList<String> args = new ArrayList<String>();

    try {
      MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
      ObjectName name = new ObjectName("java.lang:type=Runtime");
      
      String []jvmArgs = (String []) mbeanServer.getAttribute(name,
                                                              "InputArguments");

      if (jvmArgs != null) {
        for (int i = 0; i < jvmArgs.length; i++) {
          String arg = jvmArgs[i];

          if (arg.startsWith("-D"))
            args.add("-J" + arg);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    for (int i = 0; i < argv.length; i++)
      args.add(argv[i]);

    argv = new String[args.size()];

    args.toArray(argv);

    return argv;
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
      throw new RuntimeException(L().l("Resin/{0}: can't find jar for ResinBoot.class in {1}",
				       Version.VERSION, path));

    int p = path.indexOf(':');
    int q = path.indexOf('!');

    path = path.substring(p + 1, q);

    _resinHome = Vfs.lookup(path).getParent().getParent();
  }

  private void calculateRootDirectory()
  {
    String rootDirectory = System.getProperty("server.root");

    if (rootDirectory != null) {
      _rootDirectory = Vfs.lookup(rootDirectory);
      return;
    }

    _rootDirectory = _resinHome;
  }

  private Path getLogDirectory()
  {
    if (_logDirectory != null)
      return _logDirectory;
    else
      return _rootDirectory.lookup("log");
  }

  private void parseCommandLine(String []argv)
  {
    String resinConf = null;

    for (int i = 0; i < argv.length; i++) {
      String arg = argv[i];

      if ("-conf".equals(arg)
	  || "--conf".equals(arg)) {
	resinConf = argv[i + 1];
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
	Logger.getLogger("").setLevel(Level.CONFIG);
      }
      else if ("-finer".equals(arg)
	       || "--finer".equals(arg)) {
	_isVerbose = true;
	Logger.getLogger("").setLevel(Level.FINER);
      }
      else if ("-fine".equals(arg)
	       || "--fine".equals(arg)) {
	_isVerbose = true;
	Logger.getLogger("").setLevel(Level.FINE);
      }
      else if (arg.startsWith("-J")
	       || arg.startsWith("-D")
	       || arg.startsWith("-X")) {
      }
      else if ("-service".equals(arg)) {
	// windows service
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
      else {
        System.out.println(L().l("unknown argument '{0}'", argv[i]));
        System.out.println();
	usage();
	System.exit(66);
      }
    }

    if (resinConf != null) {
      _resinConf = Vfs.getPwd().lookup(resinConf);

      if (! _resinConf.exists() && _rootDirectory != null)
        _resinConf = _rootDirectory.lookup(resinConf);

      if (! _resinConf.exists() && _resinHome != null)
        _resinConf = _resinHome.lookup(resinConf);

      if (! _resinConf.exists())
        throw new ConfigException(L().l("Resin/{0} can't find configuration file '{1}'", Version.VERSION, _resinConf.getNativePath()));
    }
  }

  private static void usage()
  {
    System.err.println(L().l("usage: java -jar resin.jar [-options] [start | stop | restart]"));
    System.err.println(L().l(""));
    System.err.println(L().l("where options include:"));
    System.err.println(L().l("   -conf <file>       select a configuration file"));
    System.err.println(L().l("   -log-directory <dir>  select a logging directory"));
    System.err.println(L().l("   -resin-home <dir>  select a resin home directory"));
    System.err.println(L().l("   -root-directory <dir>  select a root directory"));
    System.err.println(L().l("   -server <id>   select a <server> to run"));
    System.err.println(L().l("   -verbose       print verbose starting information"));
  }

  boolean start()
    throws Exception
  {
    if (_startMode == StartMode.START) {
      try {
	_server.startWatchdog(_argv);
	
	System.out.println(L().l("Resin/{0} started -server '{1}'.",
				 Version.VERSION, _server.getId()));
      } catch (Exception e) {
	System.out.println(L().l("Resin/{0} can't start -server '{1}'.\n{2}",
				 Version.VERSION, _server.getId(),
				 e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }
      
      return false;
    }
    else if (_startMode == StartMode.STOP) {
      try {
	_server.stopWatchdog();
	
	System.out.println(L().l("Resin/{0} stopped -server '{1}'.",
				 Version.VERSION, _server.getId()));
      } catch (Exception e) {
	System.out.println(L().l("Resin/{0} can't stop -server '{1}'.\n{2}",
				 Version.VERSION, _server.getId(),
				 e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }
      
      return false;
    }
    else if (_startMode == StartMode.RESTART) {
      try {
	_server.restartWatchdog(_argv);
	
	System.out.println(L().l("Resin/{0} restarted -server '{1}'.",
				 Version.VERSION, _server.getId()));
      } catch (Exception e) {
	System.out.println(L().l("Resin/{0} can't restart -server '{1}'.\n{2}",
				 Version.VERSION, _server.getId(),
				 e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }
      
      return false;
    }
    else if (_startMode == StartMode.SHUTDOWN) {
      try {
	_server.shutdown();

	System.err.println(L().l("Resin/{0} shutdown ResinWatchdogManager",
				 Version.VERSION));
      } catch (Exception e) {
	System.err.println(L().l("Resin/{0} can't shutdown ResinWatchdogManager.\n{1}",
				 Version.VERSION, e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }

      return false;
    }
    else {
      return _server.startSingle(_argv, _rootDirectory) != 0;
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
    } catch (Exception e) {
      if (e instanceof CompileException) {
	System.out.println(e.getMessage());

	System.exit(2);
      }
      else {
	e.printStackTrace();
      
	System.exit(3);
      }
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
      return _rootDirectory;
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
