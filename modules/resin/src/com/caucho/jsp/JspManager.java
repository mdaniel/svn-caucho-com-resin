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

package com.caucho.jsp;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.SingleThreadModel;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.jsp.JspFactory;

import com.caucho.java.LineMap;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.SimpleLoader;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.Vfs;

/**
 * Manages JSP templates.
 */
public class JspManager extends PageManager {
  private final static L10N L = new L10N(JspManager.class);
  private final static Logger log
    = Logger.getLogger(JspManager.class.getName());

  private static int _count;

  private boolean _isXml;
  private boolean _isLoadTldOnInit;
  private boolean _precompile = true;
  
  private DynamicClassLoader _tagsLoader;

  public JspManager()
  {
    if (JspFactory.getDefaultFactory() == null)
      JspFactory.setDefaultFactory(new QJspFactory());
  }

  /**
   * Returns true if JSP pages are precompiled.
   */
  public boolean getPrecompile()
  {
    return _precompile;
  }

  /**
   * Set true if xml is default.
   */
  public void setXml(boolean isXml)
  {
    _isXml = isXml;
  }

  /**
   * Set true if tld should be loaded on init.
   */
  public void setLoadTldOnInit(boolean isLoadOnInit)
  {
    _isLoadTldOnInit = isLoadOnInit;
  }

  public static long getCheckInterval()
  {
    WebApp webApp = WebApp.getCurrent();

    if (webApp == null)
      return -1;

    JspPropertyGroup jsp = webApp.getJsp();
    
    if (jsp != null) {
      return jsp.getDependencyCheckInterval();
    }
    else {
      return -1;
    }
  }

  /**
   * Initialize the manager.
   */
  public void init()
  {
    WebApp app = getWebApp();

    app.getJspApplicationContext().setPageManager(this);

    if (_isLoadTldOnInit) {
      try {
        TldManager tld = TldManager.create(new AppResourceManager(app), app);

        // must be initialized at startup for <listeners>, e.g. JSF
        tld.init();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Creates a JSP page.  The calling servlet will execute the page by
   * calling the service method.
   *
   * @param path path to the JSP file
   * @param uri the JSP's uri
   * @param uriPwd the parent of the JSP's uri
   *
   * @return the compiled JSP page
   */
  Page createGeneratedPage(Path path, String uri, String className,
                           ServletConfig config,
                           ArrayList<PersistentDependency> dependList)
    throws Exception
  {
    return createPage(path, uri, className, config, dependList, true);
  }

  /**
   * Creates a JSP page.  The calling servlet will execute the page by
   * calling the service method.
   *
   * @param path path to the JSP file
   * @param uri the JSP's uri
   * @param uriPwd the parent of the JSP's uri
   *
   * @return the compiled JSP page
   */
  @Override
  Page createPage(Path path, String uri, String className,
                  ServletConfig config,
                  ArrayList<PersistentDependency> dependList)
    throws Exception
  {
    return createPage(path, uri, className, config, dependList, false);
  }

  /**
   * Creates a JSP page.  The calling servlet will execute the page by
   * calling the service method.
   *
   * @param path path to the JSP file
   * @param uri the JSP's uri
   * @param uriPwd the parent of the JSP's uri
   *
   * @return the compiled JSP page
   */
  private Page createPage(Path path, String uri, String className,
                          ServletConfig config,
                          ArrayList<PersistentDependency> dependList,
                          boolean isGenerated)
    throws Exception
  {
    Page page = compile(path, uri, className, config, dependList, isGenerated);

    if (page == null)
      return null;

    // need to load class, too

    //Page page = loadPage(jspClass, parseState.getLineMap(), req);
    //page = loadPage(page, config, null);

    boolean alwaysModified = false;
    if (alwaysModified)
      page._caucho_setAlwaysModified();

    return page;
  }

  protected void initPageManager()
  {
    JspCompiler compiler = new JspCompiler();

    compiler.setWebApp(_webApp);
    compiler.setXml(_isXml);

    try {
      compiler.init();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  Page compile(Path path, String uri, String className,
               ServletConfig config,
               ArrayList<PersistentDependency> dependList,
               boolean isGenerated)
    throws Exception
  {
    WebApp webApp = getWebApp();
    JspCompiler compiler = new JspCompiler();
    
    compiler.setJspManager(this);
    compiler.setWebApp(_webApp);
    compiler.setXml(_isXml);

    Page page = null;

    try {
      if (_precompile || _autoCompile) {
        page = preload(className, getParentLoader(), webApp.getRootDirectory(),
                       config);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    if (page != null) {
      if (log.isLoggable(Level.FINE))
        log.fine("loading pre-compiled page for " + uri);

      return page;
    }

    if (path == null || path.isDirectory() || ! _autoCompile)
      return null;

    Path jspJarPath = null;
    boolean isPathReadable = path.canRead();

    if (! isPathReadable) {
      String resource = "META-INF/resources" + uri;
      _webApp.getClassLoader().getResource(resource);

      URL url = _webApp.getClassLoader().getResource(resource);

      if (url != null)
        jspJarPath = Vfs.lookup(url);
    }

    if (! isPathReadable && jspJarPath == null) {
      return null;
    }

    if (jspJarPath != null)
      path = jspJarPath;

    JspCompilerInstance compilerInst =
      compiler.getCompilerInstance(path, uri, className);

    compilerInst.setGeneratedSource(isGenerated);
    compilerInst.addDependList(dependList);

    //compiler.setParentLoader(getParentLoader());
    
    //page = compilerInst.compile();
    page = compilerInst.compile();
    
    // second set required because tag class might change
    //compiler.setParentLoader(getParentLoader());
    
    //page = compilerInst.load();

    Path classPath = getClassDir().lookup(className.replace('.', '/') +
                                          ".class");

    loadPage(page, config, null, uri);

    if (classPath.canRead())
      page._caucho_addDepend(classPath.createDepend());

    return page;
  }

  void killPage(CauchoRequest request, CauchoResponse response, Page page)
  {
  }

  /**
   * True if the pre-load would load a valid class.
   */
  Page preload(String className,
               ClassLoader parentLoader,
               Path appDir,
               ServletConfig config)
    throws Exception
  {
    DynamicClassLoader loader;

    String fullClassName = className;
    String mangledName = fullClassName.replace('.', '/');

    Path classPath = getClassDir().lookup(mangledName + ".class");

    /*
    if (! classPath.exists())
      return preloadStatic(mangledName);
    */
    
    loader = SimpleLoader.create(parentLoader, getClassDir(), null);
    Class<?> cl = null;

    // If the loading fails, remove the class because it may be corrupted
    try {
      cl = Class.forName(fullClassName, false, loader);
    } catch (ClassNotFoundException e) {
      log.finest(e.toString());

      return null;
    } catch (NoClassDefFoundError e) {
      log.finest(e.toString());

      return null;
    } catch (OutOfMemoryError e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);

      if (_autoCompile) {
        try {
          log.warning("removing generated file " +
                      classPath + " due to " + e.toString());

          classPath.remove();
        } catch (IOException e1) {
          log.log(Level.FINE, e1.toString(), e1);
        }
      }

      return null;
    }

    HttpJspPage jspPage = (HttpJspPage) cl.newInstance();
    Page page = null;

    if (jspPage instanceof CauchoPage) {
      CauchoPage cPage = (CauchoPage) jspPage;

      cPage.init(appDir);

      if (cPage instanceof Page)
        ((Page) cPage)._caucho_setJspManager(this);

      if (_autoCompile && cPage._caucho_isModified())
        return null;
      else if (cPage instanceof Page)
        page = (Page) cPage;
      else
        page = new WrapperPage(jspPage);
    }
    else if (jspPage instanceof SingleThreadModel)
      page = new SingleThreadWrapperPage((HttpJspPage) jspPage);
    else
      page = new WrapperPage(jspPage);

    page._caucho_addDepend(classPath.createDepend());

    loadPage(page, config, null, className);

    return page;
  }

  /**
   * Loads an already-compiled JSP class.
   *
   * @param jspClass the class object of the JSP file.
   * @param lineMap the java to JSP line map.
   */
  private Page loadPage(Page page, ServletConfig config, LineMap lineMap,
                        String servletName)
    throws Exception
  {
    page.init(_webApp.getRootDirectory());

    page._caucho_setJspManager(this);

    if (config == null)
      config = new JspServletConfig(_webApp, null, servletName);

    page.caucho_init(config);

    return page;
  }
  
  ClassLoader getParentLoader()
  {
    return getTagsLoader();
  }
  
  ClassLoader getTagsLoader()
  {
    DynamicClassLoader tagsLoader = _tagsLoader;
    
    if (tagsLoader != null && ! tagsLoader.isModifiedNow()) {
      return tagsLoader;
    }
    
    ClassLoader parentLoader = getWebApp().getClassLoader();
    
    tagsLoader = SimpleLoader.create(parentLoader, getClassDir(), 
                                     "_jsp/_web_22dinf/_tags");
    
    _tagsLoader = tagsLoader;
    
    return tagsLoader;
  }

  void unload(Page page)
  {
  }
}
