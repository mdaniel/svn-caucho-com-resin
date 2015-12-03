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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.java;

import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.DisplayableException;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Compiles Java source, returning the loaded class.
 */
abstract public class AbstractJavaCompiler implements Runnable {
  protected static final L10N L = new L10N(AbstractJavaCompiler.class);
  protected static final Logger log
    = Logger.getLogger(AbstractJavaCompiler.class.getName());

  private ClassLoader _loader;
  
  protected JavaCompilerUtil _compiler;
  private final AtomicBoolean _isDone = new AtomicBoolean();

  // path of source files
  private String []_path;
  private LineMap _lineMap;

  private Throwable _exception;
  
  public AbstractJavaCompiler(JavaCompilerUtil compiler)
  {
    _loader = Thread.currentThread().getContextClassLoader();
    
    _compiler = compiler;
  }

  /**
   * Sets the path of files to compile.
   */
  public void setPath(String []path)
  {
    _path = path;
  }

  /**
   * Sets the LineMap for the file
   */
  public void setLineMap(LineMap lineMap)
  {
    _lineMap = lineMap;
  }

  /**
   * Returns any compile exception.
   */
  public Throwable getException()
  {
    return _exception;
  }

  /**
   * Returns true when the compilation is done.
   */
  public boolean isDone()
  {
    return _isDone.get();
  }

  /**
   * runs the compiler.
   */
  @Override
  public void run()
  {
    Thread thread = Thread.currentThread();
    
    try {
      thread.setContextClassLoader(_loader);

      compileInt(_path, _lineMap);
    } catch (final Throwable e) {
      // env/0203 vs env/0206
      if (e instanceof DisplayableException) {
        log.fine(e.getMessage());
      }
      else {
        log.fine(e.toString());
      }
      log.log(Level.FINEST, e.toString(), e);
      
      _exception = e;
    } finally {
      thread.setContextClassLoader(null);

      notifyComplete();
    }
  }

  protected final void waitForComplete(long timeout)
  {
    long endTime = CurrentTime.getCurrentTimeActual() + timeout;

    synchronized (_isDone) {
      while (! isDone()
             && CurrentTime.getCurrentTimeActual() <= endTime) {
        Thread.interrupted();
        
        try {
          _isDone.wait(endTime - CurrentTime.getCurrentTimeActual());
        } catch (Exception e) {
        }
      }
    }
  }

  protected void notifyComplete()
  {
    synchronized (_isDone) {
      _isDone.set(true);
      _isDone.notifyAll();
    }
  }

  /**
   * Quit the compilation.
   */
  public void abort()
  {
  }

  /**
   * Compile the configured file.
   *
   * @param path the path to the java source.
   * @param lineMap mapping from the generated source to the original files.
   */
  abstract protected void compileInt(String []path, LineMap lineMap)
    throws IOException;
}
