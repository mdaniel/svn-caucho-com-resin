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

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.EjbRef;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentBean;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Logger;

public class AppClient
  implements EnvironmentBean
{
  private static L10N L = new L10N(AppClient.class);
  private static Logger log = Logger.getLogger(AppClient.class.getName());

  private final EnvironmentClassLoader _loader;

  private String _mainClassName;
  private Path _clientJar;

  private Lifecycle _lifecycle = new Lifecycle(log);
  private Method _mainMethod;
  private String[] _mainArgs = new String[] {};
  private ArrayList<EjbRef> _ejbRefs = new ArrayList<EjbRef>();

  private AppClient()
  {
    _loader = new EnvironmentClassLoader();
  }

  public ClassLoader getClassLoader()
  {
    return _loader;
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

  public void addEjbRef(EjbRef ejbRef)
  {
    _ejbRefs.add(ejbRef);
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

  public void init()
    throws Exception
  {
    if (!_lifecycle.toInitializing())
      return;

    if (_clientJar == null)
      throw new ConfigException(L.l("`{0}' is required", "client-jar"));

    _loader.setId(toString());
    _loader.addJar(_clientJar);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);
      
      JarPath jarPath = JarPath.create(_clientJar);

      Path xml = jarPath.lookup("META-INF/application-client.xml");

      if (xml.canRead())
        new Config().configureBean(this, xml, "com/caucho/server/e_app/app-client.rnc");

      if (_mainClassName == null)
        throw new ConfigException(L.l("`{0}' is required", "main-class"));

      Class<?> mainClass = Class.forName(_mainClassName, false, _loader);
      _mainMethod = mainClass.getMethod("main", String[].class);

      _lifecycle.setName(toString());
      _lifecycle.toInit();
    } finally {
      thread.setContextClassLoader(oldLoader);
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
    String ear = null;
    String main = null;
    String conf = null;
    String []mainArgs = null;

    EnvironmentClassLoader.initializeEnvironment();

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-conf")) {
        conf = args[i + 1];
        i++;
      }
      else if (args[i].equals("-client-jar")) {
        clientJar = args[i + 1];
        i++;
      }
      else if (args[i].equals("-main")) {
        main = args[i + 1];
        i++;

        mainArgs = new String[args.length - i - 1];
        System.arraycopy(args, i + 1, mainArgs, 0, mainArgs.length);
        break;
      }
      else
        throw new ConfigException(L.l("unknown arg `{0}'", args[i]));
    }

    AppClient appClient = new AppClient();

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

}

