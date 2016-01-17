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

package com.caucho.v5.jsp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.SingleThreadModel;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.jsp.JspPage;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.xml.ConfigXml;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.http.webapp.WebAppResin;
import com.caucho.v5.javac.JavaCompilerUtil;
import com.caucho.v5.javac.JavacConfig;
import com.caucho.v5.jsp.cfg.JspConfig;
import com.caucho.v5.jsp.cfg.JspPropertyGroup;
import com.caucho.v5.jsp.cfg.JspTaglib;
import com.caucho.v5.loader.CompilingLoader;
import com.caucho.v5.loader.DirectoryLoader;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.loader.SimpleLoader;
import com.caucho.v5.resin.ResinEmbed;
import com.caucho.v5.resin.WebAppEmbed;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * Compilation interface for JSP pages.
 *
 * <pre>
 * com.caucho.v5.jsp.JspCompiler [flags] jsp1 jsp2 ...");
 *     -app-dir  : The directory root of the web-app.");
 *     -class-dir: The working directory to use as output.");
 *     -conf: A configuration file for the compiler.");
 * </pre>
 */
public class JspCompiler implements EnvironmentBean {
  private static final L10N L = new L10N(JspCompiler.class);
  private static final Logger log
    = Logger.getLogger(JspCompiler.class.getName());

  private SystemManager _system;
  private ClassLoader _loader;

  private WebAppResin _webApp;

  private PathImpl _classDir;
  private PathImpl _appDir;

  private JspResourceManager _resourceManager;
  private TaglibManager _taglibManager;
  private final TagFileManager _tagFileManager;

  private JspPropertyGroup _jspPropertyGroup;

  private boolean _isXml;
  private ArrayList<String> _preludeList = new ArrayList<String>();
  private ArrayList<String> _codaList = new ArrayList<String>();

  private HashSet<String> _compilingTags = new HashSet<String>();
  private boolean _hasRecursiveCompile;

  private ArrayList<JspCompilerInstance> _pending =
    new ArrayList<JspCompilerInstance>();
  
  private ResinEmbed _resin;

  public JspCompiler()
  {
    _system = SystemManager.getCurrent();
    
    if (_system == null)
      _system = new SystemManager("jsp-compiler");
    
    _loader = _system.getClassLoader();

    _tagFileManager = new TagFileManager(this);
  }

  /**
   * Returns the classloader for configuration.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
  }
  
  public void setClassLoader(ClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Sets the destination class directory.
   */
  public void setClassDir(PathImpl path)
  {
    _classDir = path;
  }

  /**
   * Sets the destination class directory.
   */
  public void setClassDirectory(PathImpl path)
  {
    setClassDir(path);
  }

  /**
   * Gets the destination class directory.
   */
  public PathImpl getClassDir()
  {
    if (_classDir != null)
      return _classDir;
    else
      return CauchoUtil.getWorkPath();
  }

  /**
   * Sets the source webApp directory.
   */
  public void setAppDir(PathImpl path)
  {
    _appDir = path;
  }

  /**
   * Gets the source webApp directory.
   */
  public PathImpl getAppDir()
  {
    if (_appDir != null)
      return _appDir;
    else if (_webApp != null)
      return _webApp.getRootDirectory();
    else
      return null;
  }

  /**
   * Adds a prelude include.
   */
  public void addPrelude(String prelude)
  {
    _preludeList.add(prelude);
  }

  /**
   * Adds a coda include.
   */
  public void addCoda(String coda)
  {
    _codaList.add(coda);
  }

  /**
   * Set true when XML is the default parser.
   */
  public void setXml(boolean isXml)
  {
    _isXml = isXml;
  }

  /**
   * True when XML is the default parser.
   */
  public boolean isXml()
  {
    return _isXml;
  }

  /**
   * Sets the resource manager.
   */
  public void setResourceManager(JspResourceManager manager)
  {
    _resourceManager = manager;
  }

  /**
   * Gets the resource manager.
   */
  public JspResourceManager getResourceManager()
  {
    return _resourceManager;
  }

  /**
   * Gets the tag file manager.
   */
  public TagFileManager getTagFileManager()
  {
    return _tagFileManager;
  }

  public TaglibManager getTaglibManager()
    throws JspParseException, IOException
  {
    synchronized (this) {
      if (_taglibManager == null) {
        WebAppResin webApp = getWebApp();

        PathImpl appDir = getAppDir();
        if (appDir == null && webApp != null)
          appDir = webApp.getRootDirectory();

        JspResourceManager resourceManager = getResourceManager();
        if (resourceManager != null) {
        }
        else if (webApp != null)
          resourceManager = new AppResourceManager(webApp);
        else {
          resourceManager = new AppDirResourceManager(appDir);
        }

        _taglibManager = new TaglibManager(resourceManager,
                                           webApp,
                                           _tagFileManager);
        _taglibManager.setWebApp(webApp);

        JspConfig jspConfig = null;

        if (webApp != null)
          jspConfig = (JspConfig) webApp.getExtension("jsp-config");

        if (jspConfig != null) {
          ArrayList<JspTaglib> tldMapList = jspConfig.getTaglibList();
          for (int i = 0; i < tldMapList.size(); i++) {
            JspTaglib taglib = tldMapList.get(i);

            _taglibManager.addLocationMap(taglib.getTaglibUri(),
                                          taglib.getTaglibLocation());
          }
        }

        if (webApp != null) {
          ArrayList<JspTaglib> taglibs = webApp.getTaglibList();
          for (int i = 0; taglibs != null && i < taglibs.size(); i++) {
            JspTaglib taglib = taglibs.get(i);

            _taglibManager.addLocationMap(taglib.getTaglibUri(),
                                          taglib.getTaglibLocation());
          }
        }
      }
    }

    return _taglibManager;
  }

  /**
   * Sets the JspPropertyGroup
   */
  public JspPropertyGroup createJsp()
  {
    if (_jspPropertyGroup == null) {
      _jspPropertyGroup = new JspPropertyGroup();
    }

    return _jspPropertyGroup;
  }

  /**
   * Sets the JspPropertyGroup
   */
  public JspPropertyGroup getJspPropertyGroup()
  {
    return _jspPropertyGroup;
  }

  /**
   * Initialize values based on the ServletContext.  When the calling code
   * has the ServletContext available, it can take advantage of it.
   */
  public WebApp createWebApp(PathImpl rootDirectory)
  {
    if (_webApp == null) {
      if (rootDirectory == null)
        rootDirectory = getAppDir();

      _resin = new ResinEmbed();
      _resin.setRootDirectory(rootDirectory.getURL());
      
      WebAppEmbed webAppEmbed = new WebAppEmbed();
      webAppEmbed.setRootDirectory(rootDirectory.getURL());
      webAppEmbed.setDisableStart(true);
      
      _resin.addWebApp(webAppEmbed);
      // jsp/193h, #4397
      _resin.start();
      
      setWebApp(webAppEmbed.getWebApp());
    }

    return _webApp;
  }

  /**
   * Initialize values based on the ServletContext.  When the calling code
   * has the ServletContext available, it can take advantage of it.
   */
  public void setWebApp(WebApp app)
  {
    _webApp = (WebAppResin) app;

    if (_resourceManager == null)
      _resourceManager = new AppResourceManager(_webApp);
  }

  /**
   * Initialize values based on the ServletContext.  When the calling code
   * has the ServletContext available, it can take advantage of it.
   */
  public ApplicationConfig createApplication()
  {
    return new ApplicationConfig();
  }

  /**
   * Returns the app.
   */
  public WebAppResin getWebApp()
  {
    return _webApp;
  }

  /**
   * Adds a new tag being compiled.
   */
  public boolean addTag(String className)
  {
    if (_compilingTags.contains(className)) {
      _hasRecursiveCompile = true;
      return true;
    }

    _compilingTags.add(className);

    return false;
  }

  /**
   * Has recursive compile.
   */
  public boolean hasRecursiveCompile()
  {
    return _hasRecursiveCompile;
  }

  /**
   * Mangles the name.
   */
  public static String urlToClassName(String name)
  {
    return JavaCompilerUtil.mangleName("jsp/" + name);
  }

  /**
   * Adds a pending compilation.
   */
  void addPending(JspCompilerInstance pending)
  {
    _pending.add(pending);
  }

  /**
   * Compiles pending compilations.
   */
  void compilePending()
    throws Exception
  {
    if (_pending.size() == 0)
      return;

    ArrayList<JspCompilerInstance> pendingList;
    pendingList = new ArrayList<JspCompilerInstance>(_pending);

    for (int i = 0; i < pendingList.size(); i++) {
      JspCompilerInstance pending = pendingList.get(i);

      pending.completeTag();
    }

    _pending.clear();
  }

  /**
   * Compiles the JSP file specified with jspFile.
   *
   * @param jspPath the path to the JSP source
   * @param uri the uri for the JSP file
   *
   * @return a JspPage instance
   */
  public Page compile(PathImpl jspPath, String uri)
    throws Exception
  {
    return getCompilerInstance(jspPath, uri).compile();
  }

  /**
   * Returns the compilation instance.
   */
  public JspCompilerInstance getCompilerInstance(PathImpl jspPath,
                                                 String uri)
    throws Exception
  {
    return getCompilerInstance(jspPath, uri, null);
  }

  public void init()
    throws JspParseException, IOException
  {
    getTaglibManager();
  }
  
  /**
   * Returns the compilation instance.
   */
  public JspCompilerInstance getCompilerInstance(PathImpl jspPath,
                                                 String uri,
                                                 String className)
    throws Exception
  {
    JspCompilerInstance instance = new JspCompilerInstance(this);

    instance.setJspPath(jspPath);
    instance.setURI(uri);
    instance.setClassName(className);

    instance.init();

    return instance;
  }
  
  /**
   * Loads an already-compiled JSP class.
   *
   * @param className the mangled classname for the JSP file.
   */
  public Page loadPage(String className, boolean isAutoCompile)
    throws Throwable
  {
    JspPage jspPage = (JspPage) loadClass(className, isAutoCompile);

    Page page;
    if (jspPage instanceof Page)
      page = (Page) jspPage;
    else if (jspPage instanceof SingleThreadModel)
      page = new SingleThreadWrapperPage((HttpJspPage) jspPage);
    else
      page = new WrapperPage((HttpJspPage) jspPage);

    return page;
  }

  /**
   * Loads an already-compiled JSP class.
   *
   * @param className the mangled classname for the JSP file.
   */
  public Object loadClass(String className, boolean autoCompile)
    throws Throwable
  {
    ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader jspLoader = SimpleLoader.create(parentLoader,
                                                getClassDir(), null);

    // If the loading fails, remove the class because it may be corrupted
    try {
      Class<?> cl = Class.forName(className, false, jspLoader);

      readSmap(parentLoader, className);

      return cl.newInstance();
    } catch (Throwable e) {
      if (autoCompile) {
        try {
          String pathName = className.replace('.', '/') + ".class";
          PathImpl classPath = getClassDir().lookup(pathName);

          classPath.remove();
        } catch (IOException e1) {
          log.log(Level.FINE, e1.toString(), e1);
        }
      }

      throw e;
    }
  }

  /**
   * Loads an already-compiled JSP class.
   *
   * @param className the mangled classname for the JSP file.
   */
  public Page loadStatic(String className, boolean isSession)
    throws Exception
  {
    // If the loading fails, remove the class because it may be corrupted
    String staticName = className.replace('.', '/') + ".static";

    PathImpl path = getClassDir().lookup(staticName);

    return new StaticPage(path, isSession);
  }

  private void readSmap(ClassLoader loader, String className)
  {
    if (loader == null)
      return;

    String smapName = className.replace('.', '/') + ".java.smap";

    InputStream is = null;
    try {
      is = loader.getResourceAsStream(smapName);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    }
  }
  
  public void close()
  {
    ResinEmbed resin = _resin;
    
    if (resin != null)
      resin.destroy();
  }

  public static void main(String []args)
    throws Exception
  {
    if (args.length == 0) {
      System.out.println("usage: com.caucho.v5.jsp.JspCompiler [flags] jsp1 jsp2 ...");
      System.out.println(" -app-dir  : The directory root of the web-app.");
      System.out.println(" -class-dir: The working directory to use as output.");
      System.out.println(" -compiler: sets the javac.");
      System.out.println(" -conf: A configuration file for the compiler.");
      System.exit(1);
    }

    // needed at minimum to handle the qa jsp/1933
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      JspCompiler compiler = new JspCompiler();

      int i = compiler.configureFromArgs(args);
    
      ClassLoader loader = compiler.getClassLoader();

      thread.setContextClassLoader(loader);
        
      ArrayList<String> pendingClasses = new ArrayList<String>();

      if (i == args.length) {
        compiler.compilePath(pendingClasses, ".");
      }

      for (; i < args.length; i++) {
        String uri = args[i];

        compiler.compilePath(pendingClasses, uri);
      }

      String files[] = new String[pendingClasses.size()];
      pendingClasses.toArray(files);

      compiler.compileBatch(files);
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
  }

  /**
   * Callable by applications to initialize the compiler.  This call
   * will configure the JspCompiler, but not start any compilations.
   *
   * <pre>
   * com.caucho.v5.jsp.JspCompiler [flags] jsp1 jsp2 ...");
   *     -app-dir  : The directory root of the web-app.");
   *     -class-dir: The working directory to use as output.");
   *     -conf: A configuration file for the compiler.");
   * </pre>
   */
  public int configureFromArgs(String []args)
    throws Exception
  {
    if (args.length == 0) {
      System.out.println("usage: com.caucho.v5.jsp.JspCompiler [flags] jsp1 jsp2 ...");
      System.out.println(" -app-dir  : The directory root of the web-app.");
      System.out.println(" -class-dir: The working directory to use as output.");
      System.out.println(" -conf: A configuration file for the compiler.");
      System.out.println(" -compiler: javac|internal|eclipse|groovyc.");
      System.exit(1);
    }

    // needed at minimum to handle the qa jsp/1933
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      ClassLoader loader = getClassLoader();

      thread.setContextClassLoader(loader);

      JspPropertyGroup jsp = createJsp();
      jsp.setRequireSource(false);

      int i = 0;
      boolean hasConf = false;

      while (i < args.length) {
        if (args[i].equals("-app-dir")) {
          PathImpl appDir = VfsOld.lookup(args[i + 1]);

          WebApp webApp = createWebApp(appDir);

          if (webApp != null)
            setWebApp(webApp);

          setAppDir(appDir);

          i += 2;
        }
        else if (args[i].equals("-class-dir") || args[i].equals("-d")) {
          setClassDirectory(VfsOld.lookup(args[i + 1]));
          i += 2;
        }
        else if (args[i].equals("-compiler")) {
          JavacConfig.getLocalConfig().setCompiler(args[i + 1]);

          i += 2;
        }
        else if (args[i].equals("-conf")) {
          PathImpl path = VfsOld.lookup(args[i + 1]);

          new ConfigXml().configureBean(this, path);
          hasConf = true;

          i += 2;
        }
        else
          break;
      }
      
      WebApp webApp = getWebApp();
      if (webApp != null && ! hasConf) {
        PathImpl appDir = webApp.getRootDirectory();

        DynamicClassLoader dynLoader = webApp.getClassLoader();
        dynLoader.addLoader(new CompilingLoader(dynLoader, appDir.lookup("WEB-INF/classes")));
        dynLoader.addLoader(new DirectoryLoader(dynLoader, appDir.lookup("WEB-INF/lib")));

        PathImpl webXml = appDir.lookup("WEB-INF/web.xml");

        if (webXml.canRead()) {
          try {
            new ConfigXml().configureBean(webApp, webXml);
          } catch (Exception e) {
            log.log(Level.WARNING, e.toString(), e);
          }
        }
      }

      PathImpl appDir = null;

      if (webApp == null && getAppDir() != null) {
        webApp = createWebApp(null);

        setWebApp(webApp);
      }

      if (webApp != null) {
        webApp.setCompileContext(true);
        
        webApp.init();

        appDir = getWebApp().getRootDirectory();
        setClassLoader(getWebApp().getClassLoader());
      }

      if (appDir == null) {
        appDir = VfsOld.lookup();

        if (getAppDir() == null && getWebApp() == null) {
          System.err.println(L.l("-app-dir must be specified for JspCompiler"));
          return 0;
        }
      }

      setResourceManager(new AppDirResourceManager(appDir));

      return i;
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
  }

  public void compilePath(ArrayList<String> pendingClasses, String uri)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());
    
      PathImpl path = VfsOld.lookup(uri);

      if (path.isDirectory())
        compileDirectory(path, getAppDir(), this, pendingClasses);
      else
        compileJsp(path, getAppDir(), this, pendingClasses);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public void compileBatch(String []pendingClasses)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());
    
      JavaCompilerUtil javaCompiler = JavaCompilerUtil.create(getClassLoader());
      javaCompiler.setClassDir(getClassDir());

      javaCompiler.compileBatch(pendingClasses);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private static void compileDirectory(PathImpl path,
                                       PathImpl appDir,
                                       JspCompiler compiler,
                                       ArrayList<String> pendingClasses)
    throws Exception
  {
    if (path.isDirectory()) {
      String []list = path.list();

      for (int i = 0; i < list.length; i++) {
        PathImpl subpath = path.lookup(list[i]);

        compileDirectory(subpath, appDir, compiler, pendingClasses);
      }
    }
    else if (path.getPath().endsWith(".jsp")
             || path.getPath().endsWith(".jsfx")
             || path.getPath().endsWith(".jspx") ||
             path.getPath().endsWith(".jsfx")) {
      compileJsp(path, appDir, compiler, pendingClasses);
    }
  }

  private static void compileJsp(PathImpl path,
                                 PathImpl appDir,
                                 JspCompiler compiler,
                                 ArrayList<String> pendingClasses)
    throws Exception
  {
    String uri;

    uri = path.getPath().substring(appDir.getPath().length());

    if (uri.endsWith("x"))
      compiler.setXml(true);
    else
      compiler.setXml(false);
    
    String className = JspCompiler.urlToClassName(uri);
    JspCompilerInstance compInst;
    compInst = compiler.getCompilerInstance(path, uri, className);
    
    JspGenerator gen = compInst.generate();

    if (! gen.isStatic())
      pendingClasses.add(className.replace('.', '/') + ".java");
  }

  public class ApplicationConfig {
    private PathImpl _rootDir;
    private ContainerProgram _program = new ContainerProgram();

    ApplicationConfig()
    {
      _rootDir = VfsOld.lookup();
    }

    public void setRootDirectory(PathImpl path)
    {
      _rootDir = path;
    }

    public void setDocumentDirectory(PathImpl path)
    {
      _rootDir = path;
    }

    public void setAppDir(PathImpl path)
    {
      _rootDir = path;
    }

    public void addBuilderProgram(ConfigProgram program)
    {
      _program.addProgram(program);
    }

    @PostConstruct
    public void init()
      throws Exception
    {
      WebApp webApp = createWebApp(_rootDir);
      
      if (webApp == null)
        throw new NullPointerException();
      
      _program.configure(webApp);
      ConfigContext.init(webApp);

      webApp.init();
      webApp.start();
    }
  }
}
