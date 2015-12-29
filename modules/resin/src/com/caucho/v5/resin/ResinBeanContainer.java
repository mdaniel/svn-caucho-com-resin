/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.resin;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;
import java.util.logging.Logger;

import com.caucho.v5.amp.Amp;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.inject.InjectManager;
import com.caucho.v5.javac.WorkDir;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.CompilingLoader;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.loader.ResourceLoader;
import com.caucho.v5.server.cdi.CdiProducerResin;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

/**
 * Embeddable Resin context for testing of bean container components (CDI
 * managed beans, EJBs, JPA) in an environment that mirrors a production runtime
 * but without the overhead of the Resin server. The ResinBeanContainer can be
 * embedded into any Java SE environment, including a JUnit test. Note, the bean
 * container does not support Servlet-based APIs. In order to test the web tier,
 * <code>ResinEmbed</code> should be used.
 * 
 * <code><pre>
 * static void main(String []args)
 * {
 *   ResinBeanContainer beans = new ResinBeanContainer();
 * 
 *   beans.addModule("test.jar");
 *   beans.start();
 * 
 *   RequestContext req = beans.beginRequest();
 *   try {
 *     MyMain main = beans.getInstance(MyMain.class);
 * 
 *     main.main(args);
 *   } finally {
 *     req.close();
 *   }
 * 
 *   beans.close();
 * }
 * </pre></code>
 * 
 * <h2>Configuration File</h2>
 * 
 * The optional configuration file for the ResinContext allows the same
 * environment and bean configuration as the resin-web.xml, but without the
 * servlet-specific configuration.
 * 
 * <pre>
 * <code>
 * &lt;beans xmlns="http://caucho.com/ns/resin"
 *              xmlns:resin="urn:java:com.caucho.resin">
 * 
 *    &lt;resin:import path="${__DIR__}/my-include.xml"/>
 * 
 *    &lt;database name="my-database">
 *      &lt;driver ...>
 *        ...
 *      &lt;/driver>
 *    &lt;/database>
 * 
 *    &lt;mypkg:MyBean xmlns:mypkg="urn:java:com.mycom.mypkg">
 *      &lt;my-property>my-data&lt;/my-property>
 *    &lt;/mypkg:MyBean>
 * &lt;/beans>
 * </code>
 * </pre>
 */
// TODO Add JNDI look-up and well as direct access to JNDI/CDI beans manager.
public class ResinBeanContainer {
  private static final Logger log
    = Logger.getLogger(ResinBeanContainer.class.getName());
  private static final L10N L = new L10N(ResinBeanContainer.class);

  private EnvironmentClassLoader _classLoader;
  private InjectManager _cdiManager;

  //private ThreadLocal<BeanContainerRequest> _localContext = new ThreadLocal<BeanContainerRequest>();

  // Path to the current module (typically the current directory)
  private Path _modulePath;
  
  private Lifecycle _lifecycle = new Lifecycle();

  /**
   * Creates a new Resin context.
   */
  public ResinBeanContainer()
  {
    _classLoader = EnvironmentClassLoader.create("resin-context");
    _cdiManager = InjectManager.create(_classLoader);

    // ioc/0b07
    //_cdiManager.replaceContext(new RequestScope());
    //_cdiManager.replaceContext(ThreadContext.getContext());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      // ioc/0p62
      //EjbManager.create(_classLoader);
      // XXX: currently this would cause a scanning of the class-path even
      // if there's no ejb-jar.xml
      // EjbManager.setScanAll();

      EnvLoader.init();

      //Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
      //Environment.addChildLoaderListener(new EjbEnvironmentListener());

      EnvLoader.addCloseListener(this);

    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public void setId(String id)
  {
    _classLoader.setId(id);
  }
  
  public ResinBeanContainer scanRoot()
  {
    _classLoader.addScanRoot();
    
    return this;
  }

  public InjectManager getCdiManager()
  {
    return _cdiManager;
  }
  
  public void setModule(String modulePath)
  {
    _modulePath = Vfs.lookup(modulePath);    
  }  

  /**
   * Adds a new module (jar or exploded classes directory)
   */
  public void addClassPath(String classPath)
  {
    Path path = Vfs.lookup(classPath);

    if (classPath.endsWith(".jar")) {
      _classLoader.addJar(path);
    } else {
      CompilingLoader loader = new CompilingLoader(_classLoader);
      loader.setPath(path);
      loader.init();
    }
  }

  /**
   * Adds a package as module root.
   * 
   * @param packageName
   *          the name of the package to be treated as a virtual module root.
   */
  public void addPackageModule(String modulePath, String packageName)
  {
    Path root = Vfs.lookup(modulePath);

    try {
      URL url = new URL(root.getURL());

      _classLoader.addScanPackage(url, packageName);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Adds a package in the classpath as module root.
   * 
   * @param packageName
   *          the name of the package to be treated as a virtual module root.
   */
  public void addPackageModule(String packageName)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Enumeration<URL> e = loader.getResources(packageName.replace('.', '/'));

      URL bestUrl = null;

      while (e.hasMoreElements()) {
        URL url = e.nextElement();

        if (bestUrl == null) {
          bestUrl = url;
          continue;
        }

        URL urlA = bestUrl;

        Path pathA = Vfs.lookup(urlA);
        Path pathB = Vfs.lookup(url);

        for (String name : pathA.list()) {
          if (name.endsWith(".class")) {
            bestUrl = urlA;
            break;
          }
        }

        for (String name : pathB.list()) {
          if (name.endsWith(".class")) {
            bestUrl = url;
            break;
          }
        }
      }

      Objects.requireNonNull(bestUrl, packageName);

      Path path = Vfs.lookup(bestUrl);

      String moduleName = path.getNativePath();

      if (moduleName.endsWith(packageName.replace('.', '/'))) {
        int prefixLength = moduleName.length() - packageName.length();
        moduleName = moduleName.substring(0, prefixLength);
      }

      addResourceRoot(path);
      addPackageModule(moduleName, packageName);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Adds a Resin beans configuration file, allowing creation of databases or
   * bean configuration.
   * 
   * @param pathName
   *          URL/path to the configuration file
   */
  public void addBeansXml(String pathName)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(getClassLoader());

      Path path = Vfs.lookup(pathName);

      // support/041a
      /*
      if (_modulePath != null)
        _cdiManager.addBeansXmlOverride(_modulePath, path);
      else
        _cdiManager.addXmlPath(path);
        */
      
      //_cdiManager.addConfigPath(path);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public void addResourceRoot(Path path)
  {
    ResourceLoader loader = new ResourceLoader(_classLoader, path);
    loader.init();
  }

  /**
   * Sets the work directory for Resin to use when generating temporary files.
   */
  public void setWorkDirectory(String path)
  {
    WorkDir.setLocalWorkDir(Vfs.lookup(path), _classLoader);
  }

  /**
   * Initializes the context.
   */
  public void start()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);
      
      ServiceManagerAmp ampManager = null;
      
      if (_lifecycle.toActive()) {
        addCdiProducer();

        ampManager = Amp.newManagerBuilder()
                        .name("resin-bean-container")
                        .classLoader(_classLoader)
                        .autoStart(false)
                        .start();
        
        Amp.setContextManager(ampManager);
        
        EnvLoader.addCloseListener(ampManager);
      }

      // env/0e81 vs env/0e3b
      // _cdiManager.update();
      
      // baratine/1182
      InjectManager.create();
      
      _classLoader.start();
      
      if (ampManager != null) {
        // baratine/1923
        ampManager.start();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns a new instance of the given type with optional bindings. If the
   * type is a managed bean, it will be injected before returning.
   * 
   * @param className
   *          the className of the bean to instantiate
   * @param qualifier
   *          optional @Qualifier annotations to select the bean
   */
  public Object getInstance(String className, Annotation... qualifiers)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      Class<?> cl = Class.forName(className, false, _classLoader);
      
      return getInstance(cl, qualifiers);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns a new instance of the given type with optional qualifiers.
   */
  @SuppressWarnings("unchecked")
  public <T> T getInstance(Class<T> type, Annotation... qualifiers)
  {
    if (type == null)
      throw new NullPointerException();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      return _cdiManager.instance(type, qualifiers);
      /*
      Set<Bean<?>> beans = _cdiManager.getBeans(type, qualifiers);

      if (beans.size() > 0) {
        Bean<?> bean = _cdiManager.resolve(beans);

        return (T) _cdiManager.getReference(bean);
      }

      return type.newInstance();
      */
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns an instance of the bean with the given name. If the type is a
   * managed bean, it will be injected before returning.
   * 
   * @param name
   *          the @Named of the bean to instantiate
   */
  public Object getBeanByName(String name)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      /*
      Set<Bean<?>> beans = _cdiManager.getBeans(name);

      if (beans.size() > 0) {
        Bean<?> bean = _cdiManager.resolve(beans);

        return _cdiManager.getReference(bean);
      }
      */
      
      return _cdiManager.createByName(name);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Executes code in the Resin bean classloader and creates a request scope.
   * 
   * <code><pre>
   * resinBean.request (new Runnable() {
   *   doMyCode();
   * });
   * </pre></code>
   * 
   * @return the RequestContext which must be passed to
   *         <code>completeContext</code>
   */
  public void request(Runnable runnable)
  {
    Thread thread = Thread.currentThread();

    ClassLoader oldLoader = thread.getContextClassLoader();

    //BeanContainerRequest oldContext = _localContext.get();

    //BeanContainerRequest context = new BeanContainerRequest(this, oldLoader,
      //  oldContext);

    thread.setContextClassLoader(_classLoader);

    //_localContext.set(context);

    try {
      runnable.run();
    } finally {
      thread.setContextClassLoader(oldLoader);
      //context.close();
    }
  }

  /**
   * Enters the Resin context and begins a new request on the thread. The the
   * returned context must be passed to the completeRequest. To ensure the
   * request is properly closed, use the following pattern:
   * 
   * <code><pre>
   * ResinContext resinContext = ...;
   * 
   * RequestContext cxt = resinContext.beginRequest();
   * 
   * try {
   *    // ... actions inside the Resin request context
   * } finally {
   *   resinContext.completeRequest(cxt);
   * }
   * </pre></code>
   * 
   * @return the RequestContext which must be passed to
   *         <code>completeContext</code>
   */
  /*
  public BeanContainerRequest beginRequest()
  {
    Thread thread = Thread.currentThread();

    ClassLoader oldLoader = thread.getContextClassLoader();

    BeanContainerRequest oldContext = _localContext.get();

    BeanContainerRequest context = new BeanContainerRequest(this, oldLoader,
        oldContext);

    thread.setContextClassLoader(_classLoader);

    _localContext.set(context);

    return context;
  }
  */

  /**
   * Completes the thread's request and exits the Resin context.
   */
  /*
  public void completeRequest(BeanContainerRequest context)
  {
    Thread thread = Thread.currentThread();

    thread.setContextClassLoader(context.getOldClassLoader());
    _localContext.set(context.getOldContext());
  }
  */

  /**
   * Shuts the context down.
   */
  public void close()
  {
    EnvironmentClassLoader loader = _classLoader;
    _classLoader = null;

    if (loader != null) {
      loader.destroy();
    }
  }

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }
  
  private void addCdiProducer()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      Class<?> resinCdiProducer = CdiProducerResin.class;

      /*
      if (resinCdiProducer != null)
        _cdiManager.addManagedBean(_cdiManager.createNew(resinCdiProducer));
        */

      Class<?> resinValidatorClass
      = CdiProducerResin.createResinValidatorProducer();

      /*
      if (_cdiManager != null && resinValidatorClass != null)
        _cdiManager.addManagedBean(_cdiManager.createNew(resinValidatorClass));
        */
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getName() + "[]";
  }

  /*
  private class RequestScope implements Context {
    @Override
    public <T> T get(Contextual<T> bean)
    {
      BeanContainerRequest cxt = _localContext.get();

      if (cxt == null)
        throw new IllegalStateException(L.l("No RequestScope is active"));

      return cxt.get(bean);
    }

    @Override
    public <T> T get(Contextual<T> bean, CreationalContext<T> creationalContext)
    {
      BeanContainerRequest cxt = _localContext.get();

      if (cxt == null)
        throw new IllegalStateException(L.l("No RequestScope is active"));

      return cxt.get(bean, creationalContext, _cdiManager);
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
      return RequestScoped.class;
    }

    @Override
    public boolean isActive()
    {
      return _localContext.get() != null;
    }
  }
  */
}
