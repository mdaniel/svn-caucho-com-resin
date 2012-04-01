/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.page;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import com.caucho.env.thread.ThreadPool;
import com.caucho.java.JavaCompileException;
import com.caucho.java.JavaCompilerUtil;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.ProfilePage;
import com.caucho.quercus.gen.QuercusGenerator;
import com.caucho.quercus.program.QuercusProgram;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Each "page" refers to a quercus file.
 */
public class ProPageManager extends PageManager
{
  private static final Logger log
    = Logger.getLogger(PageManager.class.getName());

  protected static final L10N L = new L10N(ProPageManager.class);

  private ArrayList<CompileItem> _pendingGenerate
    = new ArrayList<CompileItem>();

  private ArrayList<CompileItem> _pendingCompile
    = new ArrayList<CompileItem>();

  private final Semaphore _generatorSemaphore = new Semaphore(2);
  private final Semaphore _compileSemaphore = new Semaphore(2);

  private boolean _isRemoveClassOnError;

  /**
   * Constructor.
   */
  public ProPageManager(QuercusContext quercus)
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

    // php/3b23
    if (! relPath.startsWith("/"))
      relPath = "/" + relPath;

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

        if (program.isCompilable() && program.startCompiling()) {
          CompileItem item = new CompileItem(program, path);
          boolean isSpawn = false;

          synchronized (_pendingGenerate) {
            _pendingGenerate.add(item);

            isSpawn = _generatorSemaphore.tryAcquire();
          }

          if (isSpawn)
            ThreadPool.getThreadPool().schedule(new GenerateThread());

          if (isCompile()) {
            program.waitForCompile();
          }
        }
      }
    } catch (Exception e) {
      throw new QuercusException(e);
    }

    if ((! isCompileFailover())
        && isCompile()
        && program.getCompileException() != null) {
      throw new QuercusException(program.getCompileException());
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

    return gen.preloadExists(className);
  }

  protected QuercusPage preloadPage(QuercusProgram program, Path path)
  {
    QuercusGenerator gen = new QuercusGenerator(getQuercus());

    try {
      String className = getClassName(path);

      Class<?> pageClass = gen.preload(program);

      if (pageClass == null)
        return null;

      QuercusPage page = createPage(path, program, pageClass);

      if (page == null)
        return null;

      if (getQuercus().isProfile()) {
        Class<?> profileClass = gen.preload(className + "__prof");

        QuercusPage profilePage = createPage(path, profileClass);
        String name = page.getUserPath();
        if (name == null)
          name = path.getUserPath();

        int profileIndex = getQuercus().getProfileIndex(name);
        profilePage = new ProfilePage(profilePage, profileIndex);

        program.setProfilePage(profilePage);
        page.setProfilePage(profilePage);
      }

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

      if (getQuercus().isProfile()) {
        Class<?> profileClass = gen.preload(className + "__prof");

        QuercusPage profilePage = createPage(path, profileClass);
        String name = page.getUserPath();
        if (name == null)
          name = path.getUserPath();
        int profileIndex = getQuercus().getProfileIndex(name);
        profilePage = new ProfilePage(profilePage, profileIndex);

        program.setProfilePage(profilePage);
        page.setProfilePage(profilePage);
      }

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

  class GenerateThread implements Runnable {
    public void run()
    {
      while (isActive()) {
        CompileItem item = null;

        synchronized (_pendingGenerate) {
          if (_pendingGenerate.size() == 0) {
            _generatorSemaphore.release();
            return;
          }

          item = _pendingGenerate.remove(0);
        }

        QuercusProgram program = item.getProgram();

        try {
          if (generateCode(item)) {
            boolean isSpawn = false;

            synchronized (_pendingCompile) {
              _pendingCompile.add(item);
              program = null;

              isSpawn = _compileSemaphore.tryAcquire();
            }

            if (isSpawn)
              ThreadPool.getThreadPool().schedule(new CompileThread());
          }
        } finally {
          if (program != null) {
            program.finishCompiling();
          }
        }
      }
    }

    private boolean generateCode(CompileItem item)
    {
      QuercusGenerator gen = new QuercusGenerator(getQuercus());

      try {
        String relPath = getRelativePath(item.getPath());

        String []files = gen.generate(item.getProgram(),
                                      relPath,
                                      isLazyCompile());

        item.setPendingFiles(files);

        if (getQuercus().isProfile()) {
          String []profileFiles
            = gen.generateProfile(item.getProgram(), relPath);

          ArrayList<String> fileList = new ArrayList<String>();

          for (String file : files)
            fileList.add(file);

          for (String file : profileFiles)
            fileList.add(file);

          files = new String[fileList.size()];
          fileList.toArray(files);

          item.setPendingFiles(files);
        }

        return true;
      } catch (JavaCompileException e) {
        log.log(Level.FINE, L.l("Quercus[{0}] loading interpreted page instead because of compile error",
                                item.getPath()), e);

        item.getProgram().setCompilable(false);
        item.getProgram().setCompileException(e);

      } catch (Exception e) {
        log.log(Level.FINE, L.l("Quercus[{0}] loading interpreted page instead because of compile error",
                                item.getPath()), e);

        item.getProgram().setCompilable(false);
        item.getProgram().setCompileException(e);
      }

      return false;
    }
  }

  class CompileThread implements Runnable {
    public void run()
    {
      while (isActive()) {
        ArrayList<CompileItem> itemList = new ArrayList<CompileItem>();

        synchronized (_pendingCompile) {
          if (_pendingCompile.size() == 0) {
            _compileSemaphore.release();
            return;
          }

          itemList.addAll(_pendingCompile);
          _pendingCompile.clear();
        }

        try {
          itemList = compile(itemList);
        } finally {
          if (itemList != null) {
            for (CompileItem item : itemList) {
              item.getProgram().finishCompiling();
            }
          }
        }
      }
    }

    /**
     * Compiles the items, returning the broken items.
     */
    private ArrayList<CompileItem> compile(ArrayList<CompileItem> itemList)
    {
      QuercusGenerator gen = new QuercusGenerator(getQuercus());

      ArrayList<String> pendingFileList = new ArrayList<String>();

      for (CompileItem item : itemList) {
        for (String file : item.getPendingFiles()) {
          pendingFileList.add(file);
        }
      }

      String []pendingFiles = new String[pendingFileList.size()];
      pendingFileList.toArray(pendingFiles);

      ArrayList<CompileItem> brokenItems = new ArrayList<CompileItem>();

      try {
        gen.compile(pendingFiles);

        for (CompileItem item : itemList) {
          load(gen, item);
        }

        return brokenItems;
      } catch (Exception e) {
        log.log(Level.FINE, L.l("Quercus compilation failed because of compile error"), e);

        itemList.get(0).getProgram().setCompileException(e);

        if (itemList.size() == 1) {
          itemList.get(0).getProgram().setCompilable(false);
          return itemList;
        }
      }

      for (CompileItem item : itemList) {
        try {
          gen.compile(item.getPendingFiles());

          load(gen, item);
        } catch (Exception e) {
          log.log(Level.FINE, L.l("Quercus compilation failed because of compile error"), e);

          item.getProgram().setCompilable(false);

          brokenItems.add(item);
        }
      }

      return brokenItems;
    }

    private void load(QuercusGenerator gen, CompileItem item)
    {
      try {
        Class<?> pageClass = gen.load(item.getProgram());

        if (pageClass == null)
          throw new IllegalStateException(L.l("can't load '{0}'", item.getProgram()));

        QuercusPage page = createPage(item.getPath(), pageClass);

        item.getProgram().setCompiledPage(page);

        if (getQuercus().isProfile()) {
          pageClass = gen.preloadProfile(item.getProgram());

          QuercusPage profilePage = createPage(item.getPath(), pageClass);

          String name = profilePage.getUserPath();
          if (name == null)
            name = item.getPath().getPath();
          int profileIndex = getQuercus().getProfileIndex(name);
          profilePage = new ProfilePage(profilePage, profileIndex);

          page.setProfilePage(profilePage);
          item.getProgram().setProfilePage(profilePage);
        }
      } catch (ClassFormatError e) {
        item.getProgram().setCompilable(false);
        log.log(Level.WARNING, e.toString(), e);

        try {
          if (_isRemoveClassOnError)
            gen.getClassFilePath(item.getProgram()).remove();
        } catch (Exception e1) {
        }
      } catch (Throwable e) {
        // need to catch errors as well
        log.log(Level.WARNING, e.toString(), e);
        item.getProgram().setCompilable(false);
      } finally {
        item.getProgram().finishCompiling();
      }
    }
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

