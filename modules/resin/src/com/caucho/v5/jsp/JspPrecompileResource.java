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

import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.*;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.javac.JavaCompilerUtil;
import com.caucho.v5.javac.LineMap;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CompileException;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

import javax.annotation.PostConstruct;
import javax.servlet.jsp.JspFactory;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource for precompiling all the *.jsp files on startup.
 */
public class JspPrecompileResource {
  private static final Logger log
    = Logger.getLogger(JspPrecompileResource.class.getName());
  private static final L10N L = new L10N(JspPrecompileResource.class);

  private FileSetType _fileSet;
  
  private WebApp _webApp;

  private final Lifecycle _lifecycle = new Lifecycle();

  private int _threadCount = 2;

  private int _completeCount;
  
  private long _timeout = 60000L;

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
   * Sets the number of threads to spawn.
   */
  public void setThreadCount(int count)
  {
    if (count < 1)
      count = 1;
    
    _threadCount = count;
  }
  
  /**
   * Set the time to wait for compilation to complete
   */
  public void setTimeoutMs(long timeout)
  {
    _timeout = timeout;
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

    ArrayList<Path> paths = _fileSet.getPaths();
    ArrayList<String> classes = new ArrayList<String>();

    for (int i = 0; i < _threadCount; i++) {
      CompileTask task = new CompileTask(paths, classes);
      
      ThreadPool.getThreadPool().schedule(task);
    }

    long expire = CurrentTime.getCurrentTime() + _timeout;
    synchronized (this) {
      while (_completeCount < _threadCount) {
        try {
          long timeout = expire - CurrentTime.getCurrentTime();

          if (timeout <= 0) {
            log.fine(this.getClass().getSimpleName() + " timeout occured");
            return;
          }

          wait(timeout);
        } catch (Exception e) {
        }
      }
    }
  }

  class CompileTask implements Runnable {
    private int _chunkCount;
    private ArrayList<Path> _paths;
    private ArrayList<String> _classes;
    private JspCompiler _compiler;

    CompileTask(ArrayList<Path> paths,
                ArrayList<String> classes)
    {
      _paths = paths;
      _classes = classes;

      synchronized (_paths) {
        _chunkCount = (_paths.size() + _threadCount) / _threadCount;
      }
      
      _compiler = new JspCompiler();
      _compiler.setWebApp(_webApp);
    }

    public void run()
    {
      try {
        while (compilePath()) {
        }
      
        while (compileClasses()) {
        }
      } finally {
        synchronized (JspPrecompileResource.this) {
          _completeCount++;

          JspPrecompileResource.this.notifyAll();
        }
      }
    }

    private boolean compilePath()
    {
      String contextPath = _webApp.getContextPath();
      if (! contextPath.endsWith("/"))
        contextPath = contextPath + "/";
    
      Path pwd = Vfs.lookup();
      Path path = null;

      synchronized (_paths) {
        if (_paths.size() == 0)
          return false;

        path = _paths.remove(0);
      }

      String uri = path.getPath().substring(pwd.getPath().length());

      if (_webApp.getContext(contextPath + uri) != _webApp)
        return true;

      String className = JspCompiler.urlToClassName(uri);

      try {
        CauchoPage page = (CauchoPage) _compiler.loadClass(className, true);

        page.init(pwd);

        if (! page._caucho_isModified()) {
          log.fine("pre-loaded " + uri);
          return true;
        }
      } catch (ClassNotFoundException e) {
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }

      log.fine("compiling " + uri);
      
      try {
        JspCompilerInstance compilerInst;
        compilerInst = _compiler.getCompilerInstance(path, uri, className);

        JspGenerator generator = compilerInst.generate();

        if (generator.isStatic())
          return true;

        LineMap lineMap = generator.getLineMap();

        synchronized (_classes) {
          _classes.add(className.replace('.', '/') + ".java");
        }
      } catch (Exception e) {
        if (e instanceof CompileException)
          log.warning(e.getMessage());
        else
          log.log(Level.WARNING, e.toString(), e);
      }

      return true;
    }

    private boolean compileClasses()
    {
      String []files;
      
      synchronized (_classes) {
        if (_classes.size() == 0)
          return false;

        files = new String[_classes.size()];
        _classes.toArray(files);
        _classes.clear();
      }

      try {
        JavaCompilerUtil javaCompiler = JavaCompilerUtil.create(null);
        javaCompiler.setClassDir(_compiler.getClassDir());

        javaCompiler.compileBatch(files);
      } catch (Exception e) {
        if (e instanceof CompileException)
          log.warning(e.getMessage());
        else
          log.log(Level.WARNING, e.toString(), e);
      }

      return true;
    }
  }
}
