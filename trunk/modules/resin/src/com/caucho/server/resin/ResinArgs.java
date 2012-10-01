/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.server.resin;

import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.config.ConfigException;
import com.caucho.log.EnvironmentStream;
import com.caucho.log.RotateStream;
import com.caucho.server.util.JniCauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.QJniServerSocket;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * The parsed Resin command-line arguments
 */
public class ResinArgs
{
  private static final Logger log = Logger.getLogger(ResinArgs.class.getName());
  private static final L10N L = new L10N(ResinArgs.class);

  private String _serverId;

  private Path _resinHome;
  private Path _rootDirectory;
  private Path _dataDirectory;
  private Path _licenseDirectory;
  
  private Path _userProperties;

  private String _resinConf;
  
  private Socket _pingSocket;
  
  private boolean _isElasticServer;
  private String _homeCluster;
  private String _elasticServerAddress;
  private int _elasticServerPort;
  
  private ArrayList<BoundPort> _boundPortList
    = new ArrayList<BoundPort>();

  private boolean _isOpenSource;
  
  private String _stage = "production";
  private String _mode = "default";
  
  private boolean _isDumpHeapOnExit;
  
  public ResinArgs()
  {
    this(new String[0]);
  }
  
  public ResinArgs(String []args)
    throws ConfigException
  {
    try {
      initEnvironmentDefaults();
    
      parseCommandLine(args);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private void initEnvironmentDefaults()
  {
    String resinHome = System.getProperty("resin.home");

    if (resinHome != null)
      _resinHome = Vfs.lookup(resinHome);
    else
      _resinHome = Vfs.getPwd();

    _rootDirectory = _resinHome;

    // server.root backwards compat
    String resinRoot = System.getProperty("server.root");

    if (resinRoot != null)
      _rootDirectory = Vfs.lookup(resinRoot);

    // resin.root backwards compat
    resinRoot = System.getProperty("resin.root");

    if (resinRoot != null)
      _rootDirectory = Vfs.lookup(resinRoot);

    try {
      URL.setURLStreamHandlerFactory(ResinURLStreamHandlerFactory.create());
    } catch (java.lang.Error e) {
      //operation permitted once per jvm; catching for harness.
    }
    
    _userProperties = Vfs.lookup(System.getProperty("user.home") + "/.resin");
  }
  
  public void setServerId(String serverId)
  {
    if ("".equals(serverId)) {
      serverId = "default";
    }
    
    _serverId = serverId;
  }

  public String getServerId()
  {
    return _serverId;
  }

  public Path getResinHome()
  {
    return _resinHome;
  }
  
  /**
   * Gets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }
  
  public void setRootDirectory(Path root)
  {
    _rootDirectory = root;
  }

  public Path getLicenseDirectory()
  {
    return _licenseDirectory;
  }

  public void setLicenseDirectory(Path licenseDirectory)
  {
    _licenseDirectory = licenseDirectory;
  }

  /**
   * Gets the root directory.
   */
  public Path getDataDirectory()
  {
    return _dataDirectory;
  }
  
  /**
   * Sets the root directory.
   */
  public void setDataDirectory(Path path)
  {
    _dataDirectory = path;
  }
  
  public Socket getPingSocket()
  {
    return _pingSocket;
  }
  
  public void setOpenSource(boolean isOpenSource)
  {
    _isOpenSource = isOpenSource;
  }
  
  public boolean isOpenSource()
  {
    return _isOpenSource;
  }

  /**
   * The configuration file used to start the server.
   */
  public String getResinConf()
  {
    return _resinConf;
  }
  
  public void setResinConf(String resinConf)
  {
    _resinConf = resinConf;
  }
  
  public Path getUserProperties()
  {
    return _userProperties;
  }
  
  public String getMode()
  {
    return _mode;
  }

  public Path getResinConfPath()
  {
    Path pwd = Vfs.lookup();

    Path resinConf = null;
    
    String resinConfFile = getResinConf();
    
    if (resinConfFile != null) {
      if (log.isLoggable(Level.FINER))
        log.finer(this + " looking for conf in " +  pwd.lookup(resinConfFile));

      resinConf = pwd.lookup(resinConfFile);
    }
    else if (pwd.lookup("conf/resin.xml").canRead())
      resinConfFile = "conf/resin.xml";
    else { // backward compat
      resinConfFile = "conf/resin.conf";
    }
    
    Path rootDirectory = getRootDirectory();

    if (resinConf == null || ! resinConf.exists()) {
      if (log.isLoggable(Level.FINER))
        log.finer(this + " looking for conf in "
                  +  rootDirectory.lookup(resinConfFile));

      resinConf = _rootDirectory.lookup(resinConfFile);
    }

    if (! resinConf.exists() && ! _resinHome.equals(_rootDirectory)) {
      if (log.isLoggable(Level.FINER))
        log.finer(this + " looking for conf in "
                  +  _resinHome.lookup(resinConfFile));

      resinConf = _resinHome.lookup(resinConfFile);
    }

    // for error messages, show path relative to rootDirectory
    if (! resinConf.exists())
      resinConf = rootDirectory.lookup(resinConfFile);

    return resinConf;
  }
  
  /**
   * Returns the bound port list.
   */
  public ArrayList<BoundPort> getBoundPortList()
  {
    return _boundPortList;
  }
  
  /**
   * Returns the stage to start with.
   */
  public String getStage()
  {
    return _stage;
  }
  
  public void setStage(String stage)
  {
    _stage = stage;
  }
  
  public void setElastic(boolean isElastic)
  {
    _isElasticServer = isElastic;
  }
  
  public boolean isElasticServer()
  {
    return _isElasticServer;
  }
  
  public int getElasticServerPort()
  {
    return _elasticServerPort;
  }
  
  public String getElasticServerAddress()
  {
    return _elasticServerAddress;
  }
  
  public void setHomeCluster(String homeCluster)
  {
    _homeCluster = homeCluster;
  }
  
  public String getHomeCluster()
  {
    return _homeCluster;
  }
  
  public String getServerAddress()
  {
    return _elasticServerAddress;
  }
  
  public void setServerAddress(String address)
  {
    _elasticServerAddress = address;
  }
  
  public int getServerPort()
  {
    return _elasticServerPort;
  }
  
  public void setServerPort(int port)
  {
    _elasticServerPort = port;
  }
  
  public String getUser()
  {
    return null;
  }
  
  public String getPassword()
  {
    return null;
  }
  
  public boolean isDumpHeapOnExit()
  {
    return _isDumpHeapOnExit;
  }

  private void parseCommandLine(String []argv)
    throws Exception
  {
    int len = argv.length;
    int i = 0;

    while (i < len) {
      // RandomUtil.addRandom(argv[i]);
      
      String arg = argv[i];
      
      if (arg.startsWith("-") && ! arg.startsWith("--"))
        arg = "-" + arg;

      if (i + 1 < len && arg.equals("--stdout")) {
        Path path = Vfs.lookup(argv[i + 1]);

        RotateStream stream = RotateStream.create(path);
        stream.init();
        WriteStream out = stream.getStream();
        out.setDisableClose(true);

        EnvironmentStream.setStdout(out);

        i += 2;
      }
      else if (i + 1 < len && arg.equals("--stderr")) {
        Path path = Vfs.lookup(argv[i + 1]);

        RotateStream stream = RotateStream.create(path);
        stream.init();
        WriteStream out = stream.getStream();
        out.setDisableClose(true);

        EnvironmentStream.setStderr(out);

        i += 2;
      }
      else if (i + 1 < len && arg.equals("--conf")) {
        _resinConf = argv[i + 1];
        i += 2;
      }
      else if (arg.equals("--log-directory")) {
        i += 2;
      }
      else if (arg.equals("--config-server")) {
        i += 2;
      }
      else if (arg.equals("--dump-heap-on-exit")) {
        _isDumpHeapOnExit = true;

        i += 1;
      }
      else if (i + 1 < len && arg.equals("--server")) {
        _serverId = argv[i + 1];
   
        if (_serverId.equals(""))
          _serverId = "default";
   
        i += 2;
      }
      else if (arg.equals("--resin-home")) {
        _resinHome = Vfs.lookup(argv[i + 1]);

        i += 2;
      }
      else if (arg.equals("--root-directory")
               || arg.equals("--resin-root")
               || arg.equals("--server-root")) {
        _rootDirectory = _resinHome.lookup(argv[i + 1]);

        i += 2;
      }
      else if (arg.equals("--data-directory")) {
        _dataDirectory = Vfs.lookup(argv[i + 1]);

        i += 2;
      }
      else if (arg.equals("--elastic-server")) {
        _isElasticServer = true;
        i += 1;
      }
      else if (i + 1 < len
               && (arg.equals("--elastic-server-address")
                   || arg.equals("--server-address"))) {
        _elasticServerAddress = argv[i + 1];
   
        i += 2;
      }
      else if (i + 1 < len
               && (arg.equals("--elastic-server-port")
                   || arg.equals("--elastic-server-port"))) {
        _elasticServerPort = Integer.parseInt(argv[i + 1]);
   
        i += 2;
      }
      else if (arg.equals("--elastic-dns")) {
        i += 1;
      }
      else if (arg.equals("--service")) {
        JniCauchoSystem.create().initJniBackground();
        // windows service
        i += 1;
      }
      else if (i + 1 < len
               && (arg.equals("--cluster")
                   || arg.equals("--join-cluster"))) {
        _homeCluster = argv[i + 1];
   
        i += 2;
      }
      else if (arg.equals("--version")) {
        System.out.println(VersionFactory.getFullVersion());
        System.exit(66);
      }
      else if (arg.equals("--watchdog-port")) {
        // watchdog
        i += 2;
      }
      else if (arg.equals("--socketwait")
               || arg.equals("--pingwait")) {
        int socketport = Integer.parseInt(argv[i + 1]);

        Socket socket = null;
        for (int k = 0; k < 15 && socket == null; k++) {
          try {
            socket = new Socket("127.0.0.1", socketport);
          } catch (Throwable e) {
            System.out.println(new Date());
            e.printStackTrace();
          }

          if (socket == null)
            Thread.sleep(1000);
        }

        if (socket == null) {
          System.err.println("Can't connect to parent process through socket " + socketport);
          System.err.println("Resin needs to connect to its parent.");
          System.exit(1);
        }

        /*
        if (argv[i].equals("-socketwait") || argv[i].equals("--socketwait"))
          _waitIn = socket.getInputStream();
        */

        _pingSocket = socket;

        //socket.setSoTimeout(60000);

        i += 2;
      }
      else if ("-port".equals(argv[i]) || "--port".equals(argv[i])) {
        int fd = Integer.parseInt(argv[i + 1]);
        String addr = argv[i + 2];
        if ("null".equals(addr))
          addr = null;
        int port = Integer.parseInt(argv[i + 3]);

        _boundPortList.add(new BoundPort(QJniServerSocket.openJNI(fd, port),
                                         addr,
                                         port));

        i += 4;
      }
      else if ("start".equals(argv[i])
               || "start-all".equals(argv[i])
               || "start-cloud".equals(argv[i])
               || "restart".equals(argv[i])) {
        JniCauchoSystem.create().initJniBackground();
        i++;
      }
      else if (argv[i].equals("-verbose")
               || argv[i].equals("--verbose")) {
        i += 1;
      }
      else if (argv[i].equals("gui")) {
        i += 1;
      }
      else if (argv[i].equals("console")) {
        i += 1;
      }
      else if (argv[i].equals("watchdog")) {
        i += 1;
      }
      else if (argv[i].equals("start-with-foreground")) {
        i += 1;
      }
      else if (argv[i].equals("-fine")
               || argv[i].equals("--fine")) {
        i += 1;
      }
      else if (argv[i].equals("-finer")
               || argv[i].equals("--finer")) {
        i += 1;
      }
      else if (argv[i].startsWith("-D")
               || argv[i].startsWith("-J")
               || argv[i].startsWith("-X")) {
        i += 1;
      }
      else if ("-stage".equals(argv[i])
               || "--stage".equals(argv[i])) {
        _stage = argv[i + 1];
        i += 2;
      }
      else if ("-preview".equals(argv[i])
               || "--preview".equals(argv[i])) {
        _stage = "preview";
        i += 1;
      }
      else if ("-debug-port".equals(argv[i])
               || "--debug-port".equals(argv[i])) {
        i += 2;
      }
      else if ("-jmx-port".equals(argv[i])
               || "--jmx-port".equals(argv[i])) {
        i += 2;
      }
      else if ("-jmx-port".equals(argv[i])
               || "--jmx-port".equals(argv[i])) {
        i += 2;
      }
      else if ("--user-properties".equals(arg)) {
        _userProperties = Vfs.lookup(argv[i + 1]);
        
        i += 2;
      }
      else if ("--mode".equals(arg)) {
        _mode = argv[i + 1];
        
        i += 2;
      }
      else {
        System.out.println(L.l("unknown argument '{0}'", arg));
        System.out.println();
        usage();
        System.exit(66);
      }
    }
  }

  private static void usage()
  {
    System.err.println(L.l("usage: bin/resin.sh [-options] [start | stop | restart]"));
    System.err.println(L.l(""));
    System.err.println(L.l("where options include:"));
    System.err.println(L.l("   -conf <file>          : select a configuration file"));
    System.err.println(L.l("   -data-directory <dir> : select a resin-data directory"));
    System.err.println(L.l("   -log-directory <dir>  : select a logging directory"));
    System.err.println(L.l("   -resin-home <dir>     : select a resin home directory"));
    System.err.println(L.l("   -root-directory <dir> : select a root directory"));
    System.err.println(L.l("   -server <id>          : select a <server> to run"));
    System.err.println(L.l("   -watchdog-port <port> : override the watchdog-port"));
    System.err.println(L.l("   -verbose              : print verbose starting information"));
    System.err.println(L.l("   -preview              : run as a preview server"));
  }

  static class DynamicServer {
    private final String _cluster;
    private final String _address;
    private final int _port;

    DynamicServer(String cluster, String address, int port)
    {
      _cluster = cluster;
      _address = address;
      _port = port;
    }

    String getCluster()
    {
      return _cluster;
    }

    String getAddress()
    {
      return _address;
    }

    int getPort()
    {
      return _port;
    }
  }
  
  static class BoundPort {
    private QServerSocket _ss;
    private String _address;
    private int _port;

    BoundPort(QServerSocket ss, String address, int port)
    {
      if (ss == null)
        throw new NullPointerException();

      _ss = ss;
      _address = address;
      _port = port;
    }

    public QServerSocket getServerSocket()
    {
      return _ss;
    }

    public int getPort()
    {
      return _port;
    }

    public String getAddress()
    {
      return _address;
    }
  }
}
