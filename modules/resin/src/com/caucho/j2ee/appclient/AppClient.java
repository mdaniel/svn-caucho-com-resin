/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits modification and use of this file in
 * source and binary form ("the Software") subject to the Caucho
 * Developer Source License 1.1 ("the License") which accompanies
 * this file.  The License is also available at
 *   http://www.caucho.com/download/cdsl1-1.xtp
 *
 * In addition to the terms of the License, the following conditions
 * must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Each copy of the Software in source or binary form must include 
 *    an unmodified copy of the License in a plain ASCII text file named
 *    LICENSE.
 *
 * 3. Caucho reserves all rights to its names, trademarks and logos.
 *    In particular, the names "Resin" and "Caucho" are trademarks of
 *    Caucho and may not be used to endorse products derived from
 *    this software.  "Resin" and "Caucho" may not appear in the names
 *    of products derived from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind. 
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.      
 *
 * @author Scott Ferguson
 */

package com.caucho.j2ee.appclient;

import com.caucho.config.BuilderProgram;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.j2ee.InjectIntrospector;
import com.caucho.j2ee.J2EEVersion;
import com.caucho.java.WorkDir;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.soa.client.WebServiceClient;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
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

  private Path _rootDirectory;
  private Path _workDirectory;
  private String _mainClassName;
  private Path _clientJar;

  private Lifecycle _lifecycle = new Lifecycle(log);
  private Method _mainMethod;
  private String[] _mainArgs = new String[] {};

  private Hashtable _ejbEnv = new Hashtable();
  private Context _ejbContext;

  private AppClient()
  {
    _loader = new EnvironmentClassLoader();
    _local.set(this, _loader);
  }

  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  public static AppClient getLocal()
  {
    return _local.get();
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
  public WebServiceClient createWebServiceClient()
  {
    return new WebServiceClient();
  }

  private void addConfig(Path path)
    throws Exception
  {
    new Config().configureBean(this, path);
  }

  public void setClientJar(Path clientJar)
  {
    _clientJar = clientJar;
  }

  public void setMainClass(String mainClassName)
  {
    _mainClassName = mainClassName;
  }

  public void setMainArgs(String[] mainArgs)
  {
    _mainArgs = mainArgs;
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

  public ClientEjbRef createEjbRef()
  {
    return new ClientEjbRef(_ejbContext);
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
      String name = _clientJar.getTail();

      int lastDot = name.lastIndexOf(".");

      if (lastDot > -1)
        name = name.substring(0, lastDot);

      _rootDirectory = WorkDir.getLocalWorkDir(_loader).lookup("_appclient").lookup("_" + name);
    }

    if (_workDirectory == null)
      _workDirectory = _rootDirectory.lookup("META-INF/work");

    WorkDir.setLocalWorkDir(_workDirectory, _loader);

    _loader.setId(toString());
    _loader.addJar(_clientJar);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("root-directory is {0}", _rootDirectory));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("work-directory is {0}", WorkDir.getLocalWorkDir()));

      _ejbContext = new InitialContext(_ejbEnv);

      JarPath jarPath = JarPath.create(_clientJar);

      configureFrom(jarPath.lookup("META-INF/application-client.xml"), true, true);
      configureFrom(jarPath.lookup("META-INF/resin-application-client.xml"), true, false);

      if (_mainClassName == null)
        throw new ConfigException(L.l("'main-class' is required"));

      Class<?> mainClass = Class.forName(_mainClassName, false, _loader);

      ArrayList<BuilderProgram> programList
        = InjectIntrospector.introspectStatic(mainClass);

      for (BuilderProgram program : programList) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, "configure: " + program);

        program.configure((Object) null);
      }

      _mainMethod = mainClass.getMethod("main", String[].class);

      _lifecycle.setName(toString());
      _lifecycle.toInit();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void configureFrom(Path xml, boolean optional, boolean validate)
    throws Exception
  {
    if (xml.canRead()) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("reading configuration file {0}", xml));

      if (validate)
        new Config().configureBean(this, xml, "com/caucho/server/e_app/app-client.rnc");
      else
        new Config().configureBean(this, xml);
    }
    else {
      if (!optional)
        throw new ConfigException(L.l("missing required configuration file {0}", xml));

      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, L.l("no configuration file {0}", xml));
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
    String main = null;
    String conf = null;
    String workDir = null;
    String []mainArgs = null;

    EnvironmentClassLoader.initializeEnvironment();

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
}

