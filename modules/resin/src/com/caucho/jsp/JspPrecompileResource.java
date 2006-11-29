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

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.java.JavaCompiler;
import com.caucho.java.LineMap;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.log.Log;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.CompileException;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import javax.servlet.jsp.JspFactory;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource for precompiling all the *.jsp files on startup.
 */
public class JspPrecompileResource {
  private static final Logger log = Log.open(JspPrecompileResource.class);
  private static final L10N L = new L10N(JspPrecompileResource.class);

  private FileSetType _fileSet;
  
  private WebApp _webApp;

  private final Lifecycle _lifecycle = new Lifecycle();

  /**
   * Sets the webApp.
   */
  public void setWebApp(WebApp app)
  {
    _webApp = app;
  }

  /**
   * Add a pattern.
   */
  public FileSetType createFileset()
  {
    if (_fileSet == null) {
      _fileSet = new FileSetType();
      _fileSet.setDir(Vfs.lookup());
    }

    return _fileSet;
  }

  /**
   * @deprecated
   */
  public FileSetType createFileSet()
  {
    return createFileset();
  }

  /**
   * Initialize the resource.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    Path pwd = Vfs.lookup();

    if (_fileSet == null) {
      createFileset().addInclude(new PathPatternType("**/*.jsp"));
    }

    if (_webApp == null) {
      _webApp = WebApp.getLocal();
    }

    if (_webApp == null)
      throw new ConfigException(L.l("JspPrecompileResource must be used in a web-app context."));

    start();
  }

  /**
   * Starts the resource.
   */
  public void start()
  {
    if (! _lifecycle.toActive())
      return;
    
    // JspManager manager = new JspManager(_webApp);

    if (JspFactory.getDefaultFactory() == null)
      JspFactory.setDefaultFactory(new QJspFactory());
    
    JspCompiler compiler = new JspCompiler();
    compiler.setWebApp(_webApp);

    ArrayList<Path> paths = _fileSet.getPaths();
    ArrayList<String> classes = new ArrayList<String>();

    String contextPath = _webApp.getContextPath();
    if (! contextPath.endsWith("/"))
      contextPath = contextPath + "/";
    
    Path pwd = Vfs.lookup();
    
    for (int i = 0; i < paths.size(); i++) {
      Path path = paths.get(i);

      String uri = path.getPath().substring(pwd.getPath().length());

      if (_webApp.getContext(contextPath + uri) != _webApp)
	continue;

      String className = JspCompiler.urlToClassName(uri);

      try {
	CauchoPage page = (CauchoPage) compiler.loadClass(className, true);

	page.init(pwd);

	if (! page._caucho_isModified()) {
	  log.fine("pre-loaded " + uri);
	  continue;
	}
      } catch (ClassNotFoundException e) {
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      }

      log.fine("compiling " + uri);
      
      try {
	JspCompilerInstance compilerInst;
	compilerInst = compiler.getCompilerInstance(path, uri, className);

	JspGenerator generator = compilerInst.generate();
	
	if (generator.isStatic())
	  continue;
	
	LineMap lineMap = generator.getLineMap();

	classes.add(className.replace('.', '/') + ".java");
      } catch (Exception e) {
	if (e instanceof CompileException)
	  log.warning(e.getMessage());
	else
	  log.log(Level.WARNING, e.toString(), e);

	continue;
      }
    }

    if (classes.size() == 0)
      return;

    try {
      JavaCompiler javaCompiler = JavaCompiler.create(null);
      javaCompiler.setClassDir(compiler.getClassDir());

      String files[] = new String[classes.size()];
      classes.toArray(files);

      javaCompiler.compileBatch(files);
    } catch (Exception e) {
      if (e instanceof CompileException)
	log.warning(e.getMessage());
      else
	log.log(Level.WARNING, e.toString(), e);
    }
  }
}
