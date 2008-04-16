/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.page;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.util.LruCache;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.*;

/**
 * Each "page" refers to a quercus file.
 */
public class PageManager
{
  private static final Logger log
    = Logger.getLogger(PageManager.class.getName());

  private final Quercus _quercus;
  
  private Path _pwd;
  private boolean _isLazyCompile;
  private boolean _isCompile;

  protected LruCache<Path,QuercusProgram> _programCache
    = new LruCache<Path,QuercusProgram>(1024);

  private boolean _isClosed;
  
  /**
   * Constructor.
   */ 
  public PageManager(Quercus quercus)
  {
    _quercus = quercus;
  }

  public Quercus getQuercus()
  {
    return _quercus;
  }

  /**
   * Gets the owning directory.
   */
  public Path getPwd()
  {
    return _quercus.getPwd();
  }

  /**
   * true if the pages should be compiled.
   */
  public boolean isCompile()
  {
    return _isCompile;
  }

  /**
   * true if the pages should be compiled.
   */
  public void setCompile(boolean isCompile)
  {
    _isCompile = isCompile;
  }

  /**
   * true if the pages should be compiled lazily.
   */
  public boolean isLazyCompile()
  {
    return _isLazyCompile;
  }

  /**
   * true if the pages should be compiled.
   */
  public void setLazyCompile(boolean isCompile)
  {
    _isLazyCompile = isCompile;
  }

  /**
   * true if the manager is active.
   */
  public boolean isActive()
  {
    return ! _isClosed;
  }

  /**
   * Returns the relative path.
   */
  /*
  public String getClassName(Path path)
  {
    if (path == null)
      return "tmp.eval";
    
    String pathName = path.getFullPath();
    String pwdName = getPwd().getFullPath();

    String relPath;

    if (pathName.startsWith(pwdName))
      relPath = pathName.substring(pwdName.length());
    else
      relPath = pathName;

    return "_quercus." + JavaCompiler.mangleName(relPath);
  }
  */
  
  /**
   * Returns a parsed or compiled quercus program.
   *
   * @param path the source file path
   *
   * @return the parsed program
   *
   * @throws IOException
   */
  public QuercusPage parse(Path path)
    throws IOException
  {
    return parse(path, null, -1);
  }
  
  /**
   * Returns a parsed or compiled quercus program.
   *
   * @param path the source file path
   *
   * @return the parsed program
   *
   * @throws IOException
   */
  public QuercusPage parse(Path path, String fileName, int line)
    throws IOException
  {
    try {
      QuercusProgram program;

      program = _programCache.get(path);

      if (program == null || program.isModified()) {
        clearProgram(path, program);
	
        program = QuercusParser.parse(_quercus,
                                      path,
                                      _quercus.getScriptEncoding(),
                                      fileName,
                                      line);
        
        _programCache.put(path, program);
      }

      if (program.getCompiledPage() != null) {
        return program.getCompiledPage();
      }

      return compilePage(program, path);
    } catch (IOException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new IOExceptionWrapper(e);
    }
  }

  protected void clearProgram(Path path, QuercusProgram program)
  {
    _programCache.remove(path);

    // php/0b36
    if (program != null)
      _quercus.clearDefinitionCache();
  }

  protected QuercusPage compilePage(QuercusProgram program, Path path)
  {
    if (log.isLoggable(Level.FINE))
      log.fine("Quercus[" + path + "] loading interpreted page");
    
    return new InterpretedPage(program);
  }

  public void close()
  {
    _isClosed = true;
  }
}

