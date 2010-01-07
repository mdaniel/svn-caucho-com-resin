/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.j2ee.appclient;

import com.caucho.config.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.j2ee.InjectIntrospector;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.*;
import com.caucho.ejb.cfg.PostConstructConfig;
import com.caucho.ejb.cfg.PreDestroyConfig;
import com.caucho.el.*;
import com.caucho.j2ee.J2EEVersion;
import com.caucho.java.WorkDir;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
//import com.caucho.soa.client.WebServiceClient;
import com.caucho.server.e_app.EnterpriseApplication;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.el.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppClient implements EnvironmentBean
{
  private static L10N L = new L10N(AppClient.class);
  private static Logger log = Logger.getLogger(AppClient.class.getName());

  private static final EnvironmentLocal<AppClient> _local
    = new EnvironmentLocal<AppClient>();

  private final EnvironmentClassLoader _loader;

  private J2EEVersion _j2eeVersion = J2EEVersion.RESIN;

  private Path _home;

  private boolean _isMetadataComplete;
  private Path _rootDirectory;
  private Path _workDirectory;
  private String _mainClassName;
  private Path _clientJar;
  private Path _earFile;

  private ArrayList<Path> _configList = new ArrayList<Path>();

  private Lifecycle _lifecycle = new Lifecycle(log);
  private Method _mainMethod;
  private String[] _mainArgs = new String[] {};

  private Hashtable _ejbEnv = new Hashtable();
  private Context _ejbContext;

  private PreDestroyConfig _preDestroyConfig;
  private PostConstructConfig _postConstructConfig;

  ArrayList<EjbRef> _ejbRefList = new ArrayList<EjbRef>();

  private AppClient()
  {
    _loader = EnvironmentClassLoader.create();
    _local.set(this, _loader);

    _home = CauchoSystem.getResinHome();
  }

  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  public static AppClient getLocal()
  {
    return _local.get();
  }

  public PostConstructConfig getPostConstruct()
  {
    return _postConstructConfig;
  }

  public PreDestroyConfig getPreDestroy()
  {
    return _preDestroyConfig;
  }

  /**
   * Used to distinguish the version of the configuration file.
   */
  public void setConfigNode(Node node)
  {
    _j2eeVersion = J2EEVersion.getJ2EEVersion((Element) node);
  }

  public J2EEVersion getJ2EEVersion()
  {
    return _j2eeVersion;
  }

  public void setMetadataComplete(boolean isComplete)
  {
    _isMetadataComplete = isComplete;
  }

  public void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
    Vfs.setPwd(_rootDirectory);
  }

  public void setWorkDirectory(Path workDirectory)
  {
    _workDirectory = workDirectory;
  }

  public void setId(String id)
  {
  }

  public void setDescription(String value)
  {
  }

  public void setIcon(com.caucho.config.types.Icon icon)
  {
  }

  /**
   * Adds a web service client.
   */
  /*
  public WebServiceClient createWebServiceClient()
  {
    return new WebServiceClient();
  }
  */

  private void addConfig(Path path)
    throws Exception
  {
    _configList.add(path);
  }

  public void setClientJar(Path clientJar)
  {
    _clientJar = clientJar;
  }

  public void setEarFile(Path earFile)
  {
    _earFile = earFile;
  }

  public void setMainClass(String mainClassName)
  {
    _mainClassName = mainClassName;
  }

  public void setMainArgs(String[] mainArgs)
  {
    _mainArgs = mainArgs;
  }

  public void setPostConstruct(PostConstructConfig postConstruct)
  {
    _postConstructConfig = postConstruct;
  }

  public void setPreDestroy(PreDestroyConfig preDestroy)
  {
    _preDestroyConfig = preDestroy;
  }

  public void setSchemaLocation(String schemaLocation)
  {
    // not needed
  }


  public void setVersion(String version)
  {
    // not needed
  }

  public void setDisplayName(String displayName)
  {
    // not needed
  }

  public void setCallbackHandler(Class<CallbackHandler> callbackHandler)
    throws Exception
  {
    CallbackManager callback = new CallbackManager();

    CallbackHandler handler = callbackHandler.newInstance();

    callback.handle(handler);

    System.setProperty(Context.SECURITY_PRINCIPAL, callback.getName());
    System.setProperty(Context.SECURITY_CREDENTIALS, callback.getPassword());
  }

  public EjbRef createEjbRef()
  {
    EjbRef ejbRef = new EjbRef(_ejbContext);

    _ejbRefList.add(ejbRef);

    ejbRef.setClientClassName(_mainClassName);

    return ejbRef;
  }

  public void init()
    throws Exception
  {
    if (!_lifecycle.toInitializing())
      return;

    if (_clientJar == null)
      throw new ConfigException(L.l("'client-jar' is required"));

    // corba needs for RMI(?)

    //EnvironmentClassLoader.initializeEnvironment();
    //System.setSecurityManager(new SecurityManager());

    if (_rootDirectory == null) {
      /*
        String name = _clientJar.getTail();

        int lastDot = name.lastIndexOf(".");

        if (lastDot > -1)
        name = name.substring(0, lastDot);

        Path root = WorkDir.getLocalWorkDir(_loader).lookup("_appclient").lookup("_" + name);

        _rootDirectory = root;
      */

      setRootDirectory(_clientJar.getParent());
    }

    if (_workDirectory == null)
      _workDirectory = _rootDirectory.lookup("META-INF/work");

    WorkDir.setLocalWorkDir(_workDirectory, _loader);

    _loader.setId(toString());
    _loader.addJar(_clientJar);

    /*
    // ejb/0fa2
    if (_earFile != null) {
      Path deployDir = _earFile.getParent();

      String s = _earFile.getTail();

      s = "_ear_" + s.substring(0, s.length() - 4);

      s = deployDir + "/" + s + "/";

      String libDir = "lib";

      Path applicationXml = Vfs.lookup(s + "META-INF/application.xml");

      EnterpriseApplication eapp = new EnterpriseApplication();

      InputStream is = applicationXml.openRead();

      new Config().configure(eapp, is,
                             "com/caucho/server/e_app/ear.rnc");

      // ejb/0fa0
      libDir = eapp.getLibraryDirectory();

      Path lib = Vfs.lookup(s + libDir);

      for (String file : lib.list()) {
        if (file.endsWith(".jar")) {
          Path sharedLib = lib.lookup(file);

          _loader.addJar(sharedLib);
        }
      }
    }
    */

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("root-directory is {0}", _rootDirectory));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("work-directory is {0}", WorkDir.getLocalWorkDir()));

      InjectManager webBeans = InjectManager.create();
      //webBeans.addSingleton(new ResinVar(), "resin");

      _ejbContext = new InitialContext(_ejbEnv);

      JarPath jarPath = JarPath.create(_clientJar);

      configureFrom(jarPath.lookup("META-INF/application-client.xml"), true);

      configureFrom(jarPath.lookup("META-INF/resin-application-client.xml"), true);

      for (Path configPath : _configList) {
        configureFrom(configPath, true);
      }

      // Merge duplicated <ejb-ref>'s
      mergeEjbRefs();

      // jpa/0s37
      Environment.addChildLoaderListener(new com.caucho.amber.manager.PersistenceEnvironmentListener());

      if (_mainClassName == null)
        throw new ConfigException(L.l("'main-class' is required"));

      Class<?> mainClass = Class.forName(_mainClassName, false, _loader);

      ArrayList<ConfigProgram> injectList = new ArrayList<ConfigProgram>();
      // XXX: static
      InjectIntrospector.introspectInject(injectList, mainClass);

      for (ConfigProgram inject : injectList) {
        inject.inject((Object) null, null);
      }

      _mainMethod = mainClass.getMethod("main", String[].class);

      _lifecycle.setName(toString());
      _lifecycle.toInit();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void configureFrom(Path xml, boolean optional)
    throws Exception
  {
    if (xml.canRead()) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("reading configuration file {0}", xml));

      HashMap<String,Object> variableMap = new HashMap<String,Object>();
      variableMap.put("resin", new ResinVar());

      ELResolver varResolver = new MapVariableResolver(variableMap);
      ConfigELContext elContext = new ConfigELContext(varResolver);

      EL.setEnvironment(elContext, _loader);
      EL.setVariableMap(variableMap, _loader);

      Config config = new Config();

      config.configureBean(this, xml, "com/caucho/server/e_app/app-client.rnc");
    }
    else {
      if (!optional)
        throw new ConfigException(L.l("missing required configuration file {0}", xml));

      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, L.l("no configuration file {0}", xml));
    }
  }

  private void mergeEjbRefs()
    throws Exception
  {
    for (int i = 0; i < _ejbRefList.size(); i++) {
      EjbRef ref = _ejbRefList.get(i);
      String refName = ref.getEjbRefName();

      for (int j = i + 1; j < _ejbRefList.size(); j++) {
        EjbRef other = _ejbRefList.get(j);

        if (refName.equals(other.getEjbRefName())) {
          ref.mergeFrom(other);
          _ejbRefList.remove(j);
          j--;
        }
      }

      // After we merge all the information, the <ejb-ref> can be initialized.
      ref.bind();
    }
  }

  public void run()
    throws Exception
  {
    init();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      _mainMethod.invoke(null, new Object[] { _mainArgs });
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public String toString()
  {
    return "AppClient[" + _clientJar + "," + _mainClassName + "]";
  }

  public static void main(String []args)
    throws Throwable
  {
    String clientJar = null;
    String earFile = null;
    String main = null;
    String conf = null;
    String workDir = null;
    String []mainArgs = null;

    Environment.init();
    
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      if (arg.startsWith("-")) {
        String option = arg.substring((arg.startsWith("--")) ? 2 : 1);

        if (option.equals("conf")) {
          conf = args[++i];
          continue;
        }
        else if (option.equals("client-jar")) {
          clientJar = args[++i];
          continue;
        }
        else if (option.equals("ear-file")) {
          earFile = args[++i];
          continue;
        }
        else if (option.equals("work-dir")) {
          workDir = args[++i];
          continue;
        }
        else if (option.equals("main")) {
          main = args[++i];

          mainArgs = new String[args.length - i - 1];
          System.arraycopy(args, i + 1, mainArgs, 0, mainArgs.length);
          break;
        }
      }

      throw new ConfigException(L.l("unknown arg '{0}'", args[i]));
    }

    AppClient appClient = new AppClient();

    if (workDir != null)
      appClient.setWorkDirectory(Vfs.lookup(workDir));

    if (clientJar != null)
      appClient.setClientJar(Vfs.lookup(clientJar));

    if (earFile != null)
      appClient.setEarFile(Vfs.lookup(earFile));

    if (conf != null)
      appClient.addConfig(Vfs.lookup(conf));

    if (main != null)
      appClient.setMainClass(main);

    if (mainArgs != null)
      appClient.setMainArgs(mainArgs);

    appClient.run();
  }

  public class CallbackManager
  {
    private final NameCallback _nameCallback;
    private final PasswordCallback _passwordCallback;

    public CallbackManager()
    {
      _nameCallback = new NameCallback(L.l("Name"));
      _passwordCallback = new PasswordCallback(L.l("Password"), false);

    }

    public void handle(CallbackHandler handler)
      throws IOException, UnsupportedCallbackException
    {
      Callback[] callbacks = new Callback[]{
        _nameCallback,
        _passwordCallback
      };

      handler.handle(callbacks);
    }

    public String getName()
    {
      return _nameCallback.getName();
    }

    public String getPassword()
    {
      return new String(_passwordCallback.getPassword());
    }
  }

  public class ResinVar {
    public Path getHome()
    {
      System.out.println("GET_HOME: " + _home);
      return _home;
    }

    public Path getRoot()
    {
      return _rootDirectory;
    }
  }
}
