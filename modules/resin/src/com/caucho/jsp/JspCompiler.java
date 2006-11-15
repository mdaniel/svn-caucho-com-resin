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

package com.caucho.jsp;

import com.caucho.config.Config;
import com.caucho.java.JavaCompiler;
import com.caucho.java.LineMap;
import com.caucho.jsp.cfg.*;
import com.caucho.loader.*;
import com.caucho.log.Log;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppController;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;

import com.caucho.vfs.*;

import javax.servlet.SingleThreadModel;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.jsp.JspPage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compilation interface for JSP pages.
 */
public class JspCompiler implements EnvironmentBean {
  private static final L10N L = new L10N(JspCompiler.class);
  private static final Logger log = Log.open(JspCompiler.class);

  private ClassLoader _loader;

  private WebApp _app;

  private Path _classDir;
  private Path _appDir;

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

  public JspCompiler()
  {
    _loader = new EnvironmentClassLoader();

    _tagFileManager = new TagFileManager(this);
  }

  /**
   * Returns the classloader for configuration.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Sets the destination class directory.
   */
  public void setClassDir(Path path)
  {
    _classDir = path;
  }

  /**
   * Sets the destination class directory.
   */
  public void setClassDirectory(Path path)
  {
    setClassDir(path);
  }

  /**
   * Gets the destination class directory.
   */
  public Path getClassDir()
  {
    if (_classDir != null)
      return _classDir;
    else
      return CauchoSystem.getWorkPath();
  }

  /**
   * Sets the source webApp directory.
   */
  public void setAppDir(Path path)
  {
    _appDir = path;
  }

  /**
   * Gets the source webApp directory.
   */
  public Path getAppDir()
  {
    if (_appDir != null)
      return _appDir;
    else if (_app != null)
      return _app.getAppDir();
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
	WebApp app = getWebApp();
	
	Path appDir = getAppDir();
	if (appDir == null && app != null)
	  appDir = app.getAppDir();

	JspResourceManager resourceManager = getResourceManager();
	if (resourceManager != null) {
	}
	else if (app != null)
	  resourceManager = new AppResourceManager(app);
	else {
	  resourceManager = new AppDirResourceManager(appDir);
	}
	
	_taglibManager = new TaglibManager(resourceManager,
					   app,
					   _tagFileManager);
	_taglibManager.setWebApp(app);

	JspConfig jspConfig = null;

	if (app != null)
	  jspConfig = (JspConfig) app.getExtension("jsp-config");

	if (jspConfig != null) {
	  ArrayList<JspTaglib> tldMapList = jspConfig.getTaglibList();
	  for (int i = 0; i < tldMapList.size(); i++) {
	    JspTaglib taglib = tldMapList.get(i);

	    _taglibManager.addLocationMap(taglib.getTaglibUri(),
					  taglib.getTaglibLocation());
	  }
	}

	if (app != null) {
	  ArrayList<JspTaglib> taglibs = app.getTaglibList();
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
  public WebApp createWebApp()
  {
    if (_app == null) {
      WebAppController controller =
	new WebAppController("",  getAppDir(), null);

      _app = controller.getDeployInstance();
    }

    return _app;
  }

  /**
   * Initialize values based on the ServletContext.  When the calling code
   * has the ServletContext available, it can take advantage of it.
   */
  public void setWebApp(WebApp app)
  {
    _app = app;

    if (_resourceManager == null)
      _resourceManager = new AppResourceManager(_app);
  }

  /**
   * Initialize values based on the ServletContext.  When the calling code
   * has the ServletContext available, it can take advantage of it.
   */
  public WebApp createApplication()
  {
    return createWebApp();
  }

  /**
   * Initialize values based on the ServletContext.  When the calling code
   * has the ServletContext available, it can take advantage of it.
   */
  public void setApplication(WebApp webApp)
  {
    setWebApp(webApp);
  }

  /**
   * Returns the app.
   */
  public WebApp getWebApp()
  {
    return _app;
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
    return JavaCompiler.mangleName("jsp/" + name);
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
  public Page compile(Path jspPath, String uri)
    throws Exception
  {
    return getCompilerInstance(jspPath, uri).compile();
  }

  /**
   * Returns the compilation instance.
   */
  public JspCompilerInstance getCompilerInstance(Path jspPath,
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
  public JspCompilerInstance getCompilerInstance(Path jspPath,
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
      Class cl = CauchoSystem.loadClass(className, false, jspLoader);

      readSmap(parentLoader, className);

      return cl.newInstance();
    } catch (Throwable e) {
      if (autoCompile) {
        try {
          String pathName = className.replace('.', '/') + ".class";
          Path classPath = getClassDir().lookup(pathName);

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
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    // If the loading fails, remove the class because it may be corrupted
    String staticName = className.replace('.', '/') + ".static";

    Path path = getClassDir().lookup(staticName);

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

  public static void main(String []args)
    throws Exception
  {
    if (args.length == 0) {
      System.out.println("usage: com.caucho.jsp.JspCompiler [flags] jsp1 jsp2 ...");
      System.out.println(" -app-dir  : The directory root of the web-app.");
      System.out.println(" -class-dir: The working directory to use as output.");
      System.out.println(" -conf: A configuration file for the compiler.");
      System.exit(1);
    }

    // needed at minimum to handle the qa jsp/1933
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      JspCompiler compiler = new JspCompiler();

      ClassLoader loader = compiler.getClassLoader();

      thread.setContextClassLoader(loader);

      JspPropertyGroup jsp = compiler.createJsp();
      jsp.setRequireSource(false);

      int i = 0;
      boolean hasConf = false;

      while (i < args.length) {
	if (args[i].equals("-app-dir")) {
	  Path appDir = Vfs.lookup(args[i + 1]);

	  WebApp app = compiler.createWebApp();
	  app.setDocumentDirectory(appDir);

	  compiler.setWebApp(app);
	  compiler.setAppDir(appDir);

	  i += 2;
	}
	else if (args[i].equals("-class-dir") || args[i].equals("-d")) {
	  compiler.setClassDirectory(Vfs.lookup(args[i + 1]));
	  i += 2;
	}
	else if (args[i].equals("-conf")) {
	  Path path = Vfs.lookup(args[i + 1]);

	  new Config().configureBean(compiler, path);
	  hasConf = true;

	  i += 2;
	}
	else
	  break;
      }

      WebApp app = compiler.getWebApp();
      if (app != null && ! hasConf) {
	Path appDir = app.getAppDir();

	DynamicClassLoader dynLoader = app.getEnvironmentClassLoader();
	dynLoader.addLoader(new CompilingLoader(appDir.lookup("WEB-INF/classes")));
	dynLoader.addLoader(new DirectoryLoader(appDir.lookup("WEB-INF/lib")));

	Path webXml = appDir.lookup("WEB-INF/web.xml");

	if (webXml.canRead()) {
	  try {
	    new Config().configureBean(app, webXml);
	  } catch (Exception e) {
	    log.log(Level.WARNING, e.toString(), e);
	  }
	}
      }

      Path appDir = null;

      if (app == null && compiler.getAppDir() != null) {
	app = compiler.createWebApp();

	app.setDocumentDirectory(compiler.getAppDir());
	compiler.setWebApp(app);
      }

      if (app != null) {
	app.init();

	appDir = compiler.getWebApp().getAppDir();
	loader = compiler.getWebApp().getClassLoader();
      }

      if (appDir == null) {
	appDir = Vfs.lookup();

	if (compiler.getAppDir() == null && compiler.getWebApp() == null) {
	  System.err.println(L.l("-app-dir must be specified for JspCompiler"));
	  return;
	}
      }

      thread.setContextClassLoader(loader);

      ArrayList<String> pendingClasses = new ArrayList<String>();

      for (; i < args.length; i++) {
	String uri = args[i];

	Path path = Vfs.lookup(uri);

	if (path.isDirectory())
	  compileDirectory(path, appDir, compiler, pendingClasses);
	else
	  compileJsp(path, appDir, compiler, pendingClasses);
      }

      JavaCompiler javaCompiler = JavaCompiler.create(loader);
      javaCompiler.setClassDir(compiler.getClassDir());

      String files[] = new String[pendingClasses.size()];
      pendingClasses.toArray(files);

      javaCompiler.compileBatch(files);
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
  }

  private static void compileDirectory(Path path,
				       Path appDir,
				       JspCompiler compiler,
				       ArrayList<String> pendingClasses)
    throws Exception
  {
    if (path.isDirectory()) {
      String []list = path.list();

      for (int i = 0; i < list.length; i++) {
	Path subpath = path.lookup(list[i]);

	compileDirectory(subpath, appDir, compiler, pendingClasses);
      }
    }
    else if (path.getPath().endsWith(".jsp") ||
	     path.getPath().endsWith(".jsfx") ||
	     path.getPath().endsWith(".jspx") ||
	     path.getPath().endsWith(".jsfx")) {
      compileJsp(path, appDir, compiler, pendingClasses);
    }
  }

  private static void compileJsp(Path path,
				 Path appDir,
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
}
