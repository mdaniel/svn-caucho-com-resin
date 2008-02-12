package com.caucho.boot;

import com.caucho.Version;
import com.caucho.server.resin.ResinELContext;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

class WatchdogArgs
{
  private static L10N _L;

  private Path _javaHome;
  private Path _resinHome;
  private Path _rootDirectory;
  private String[] _argv;
  private Path _resinConf;
  private Path _logDirectory;
  private String _serverId = "";
  private int _watchdogPort;
  private boolean _isVerbose;

  WatchdogArgs(String[] argv)
  {
    super();
    
    _resinHome = calculateResinHome();
    _rootDirectory = calculateResinRoot(_resinHome);
    
    _javaHome = Vfs.lookup(System.getProperty("java.home"));

    _argv = argv;

    _resinConf = _resinHome.lookup("conf/resin.conf");

    parseCommandLine(argv);
  }

  Path getJavaHome()
  {
    return _javaHome;
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
    if (_logDirectory != null) {
      return _logDirectory;
    } else {
      return _rootDirectory.lookup("log");
    }
  }

  Path getResinConf()
  {
    return _resinConf;
  }

  String getServerId()
  {
    return _serverId;
  }

  String[] getArgv()
  {
    return _argv;
  }

  boolean isVerbose()
  {
    return _isVerbose;
  }

  int getWatchdogPort()
  {
    return _watchdogPort;
  }

  void setResinHome(Path resinHome)
  {
    _resinHome = resinHome;
  }

  public ResinELContext getELContext()
  {
    return new ResinBootELContext();
  }

  private void parseCommandLine(String[] argv)
  {
    for (int i = 0; i < argv.length; i++) {
      String arg = argv[i];

      if ("-conf".equals(arg) || "--conf".equals(arg)) {
        _resinConf = _resinHome.lookup(argv[i + 1]);
        i++;
      } else if ("-log-directory".equals(arg) || "--log-directory".equals(arg)) {
        _logDirectory = _rootDirectory.lookup(argv[i + 1]);
        i++;
      } else if ("-resin-home".equals(arg) || "--resin-home".equals(arg)) {
        _resinHome = Vfs.lookup(argv[i + 1]);
        i++;
      } else if ("-root-directory".equals(arg) || "--root-directory".equals(arg)) {
        _rootDirectory = Vfs.lookup(argv[i + 1]);
        i++;
      } else if ("-server-root".equals(arg) || "--server-root".equals(arg)) {
        _rootDirectory = Vfs.lookup(argv[i + 1]);
        i++;
      } else if ("-server".equals(arg) || "--server".equals(arg)) {
        _serverId = argv[i + 1];
        i++;
      } else if ("-watchdog-port".equals(arg) || "--watchdog-port".equals(arg)) {
        _watchdogPort = Integer.parseInt(argv[i + 1]);
        i++;
      } else if ("-verbose".equals(arg) || "--verbose".equals(arg)) {
        _isVerbose = true;
        Logger.getLogger("").setLevel(Level.FINE);
      }
    }
  }
  
  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(WatchdogArgs.class);
    
    return _L;
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

  public class ResinBootELContext
    extends ResinELContext
  {
    public Path getResinHome()
    {
      return WatchdogArgs.this.getResinHome();
    }

    public Path getRootDirectory()
    {
      return WatchdogArgs.this.getRootDirectory();
    }

    public Path getResinConf()
    {
      return WatchdogArgs.this.getResinConf();
    }

    public String getServerId()
    {
      return WatchdogArgs.this.getServerId();
    }

    public boolean isResinProfessional()
    {
      return false;
    }
  }
}
