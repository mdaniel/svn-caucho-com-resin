/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.page;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import com.caucho.java.JavaCompilerUtil;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.gen.QuercusGenerator;
import com.caucho.quercus.program.QuercusProgram;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Each "page" refers to a quercus file.
 */
public class ProGooglePageManager extends PageManager
{
  private static final Logger log
    = Logger.getLogger(ProGooglePageManager.class.getName());

  protected static final L10N L = new L10N(ProGooglePageManager.class);

  /**
   * Constructor.
   */
  public ProGooglePageManager(QuercusContext quercus)
  {
    super(quercus);
  }

  /**
   * Returns the relative path.
   */
  public String getClassName(Path path)
  {
    String relPath = getRelativePath(path);

    return "_quercus." + JavaCompilerUtil.mangleName(relPath);
  }

  /**
   * Returns the relative path.
   */
  public String getRelativePath(Path path)
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

    return relPath;
  }

  @Override
  protected QuercusPage compilePage(QuercusProgram program, Path path)
  {
    try {
      if (isLazyCompile() || isCompile()) {
        QuercusPage page = preloadPage(program, path);

        if (page != null) {
          if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, L.l("Quercus[{0}] loading precompiled page", path));

          return page;
        }

        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, L.l("Quercus[{0}] cannot find precompiled page", path));

        return new InterpretedPage(program);
      }
    } catch (Exception e) {
      throw new QuercusException(e);
    }

    return new InterpretedPage(program);
  }

  @Override
  protected void clearProgram(Path path, QuercusProgram program)
  {
    super.clearProgram(path, program);

    getQuercus().setCompileClassLoader(null);
  }

  @Override
  public boolean precompileExists(Path path)
  {
    String className = getClassName(path);

    QuercusGenerator gen = new QuercusGenerator(getQuercus());

    // preloadExists() checks to see if the class file is in the work dir
    // (usually WEB-INF/work)
    if (gen.preloadExists(className)) {
      return true;
    }

    // files may also be precompiled offline and included into the classpath
    Class<?> pageClass = null;

    try {
      pageClass = gen.preload(className);
    }
    catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    return pageClass != null;
  }

  protected QuercusPage preloadPage(QuercusProgram program, Path path)
  {
    QuercusGenerator gen = new QuercusGenerator(getQuercus());

    try {
      Class<?> pageClass = gen.preload(program);

      if (pageClass == null)
        return null;

      QuercusPage page = createPage(path, program, pageClass);

      if (page == null)
        return null;

      return page;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    return null;
  }

  @Override
  protected QuercusProgram preloadProgram(Path path, String fileName)
  {
    String className = getClassName(path);

    QuercusGenerator gen = new QuercusGenerator(getQuercus());

    try {
      Class<?> pageClass = gen.preload(className);

      if (pageClass == null)
        return null;

      QuercusPage page = createPage(path, pageClass);

      QuercusProgram program = new QuercusProgram(getQuercus(), path, page);

      return program;
    }
    catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    }
  }

  private QuercusPage createPage(Path path,
                                 QuercusProgram program,
                                 Class<?> pageClass)
  {
    try {
      QuercusPage page = createPage(path, pageClass);

      program.setCompiledPage(page);

      return page;
    } catch (RuntimeException e) {
      throw e;
    } catch (ClassFormatError e) {
      throw e;
    } catch (Exception e) {
      throw new QuercusException(e);
    }
  }

  private QuercusPage createPage(Path path, Class<?> pageClass)
  {
    try {
      QuercusPage page = (QuercusPage) pageClass.newInstance();

      page.init(getQuercus());

      Method selfPath = pageClass.getMethod("quercus_setSelfPath",
                                            new Class[] { Path.class });

      selfPath.invoke(null, path);

      return page;
    } catch (RuntimeException e) {
      throw e;
    } catch (ClassFormatError e) {
      throw e;
    } catch (Exception e) {
      throw new QuercusException(e);
    }
  }

  public void close()
  {
  }

  static class CompileItem {
    private QuercusProgram _program;
    private Path _path;
    private String []_javaFiles;

    CompileItem(QuercusProgram program, Path path)
    {
      _program = program;
      _path = path;
    }

    QuercusProgram getProgram()
    {
      return _program;
    }

    Path getPath()
    {
      return _path;
    }

    public void setPendingFiles(String []javaFiles)
    {
      _javaFiles = javaFiles;
    }

    public String []getPendingFiles()
    {
      return _javaFiles;
    }
  }
}

