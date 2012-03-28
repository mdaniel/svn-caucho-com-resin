/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.java.JavaCompileException;
import com.caucho.java.JavaCompilerUtil;
import com.caucho.java.JavacConfig;
import com.caucho.java.WorkDir;
import com.caucho.quercus.gen.QuercusGenerator;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

public class QuercusCompiler
{
  private static final Logger log
    = Logger.getLogger(QuercusCompiler.class.getName());
  
  protected static final L10N L = new L10N(QuercusCompiler.class);
  
  private ProQuercus _quercus;
  private Path _workDir;

  public QuercusCompiler()
  {
    _quercus = new ProQuercus();
    _quercus.init();
    
    setWorkDir(WorkDir.getTmpWorkDir().lookup("WEB-INF/classes"));
  }
  
  /**
   * Sets the destination class directory.
   */
  public void setWorkDir(Path path)
  {
    _workDir = path;
    _quercus.setWorkDir(path);
  }

  /**
   * Gets the destination class directory.
   */
  public Path getWorkDir()
  {
    if (_workDir != null)
      return _workDir;
    else
      return CauchoSystem.getWorkPath();
  }
  
  public void setScriptEncoding(String encoding)
  {
    _quercus.setScriptEncoding(encoding);
  }
  
  public void setRequireSource(boolean isRequireSource)
  {
    _quercus.setRequireSource(isRequireSource);
  }
  
  public static void main(String []args)
    throws Exception
  {
    if (args.length == 0) {
      System.out.println("usage: com.caucho.quercus.QuercusCompiler [flags] php1 php2 ...");
      System.out.println(" -output-dir      : The directory to use for output (default /tmp/caucho).");
      System.out.println(" -compiler        : Sets the javac.");
      System.out.println(" -script-encoding : The encoding of the source files (default ISO-8859-1).");
      System.out.println(" -require-source  : Whether or not the source files are required (default false).");
      System.exit(1);
    }

    try {
      QuercusCompiler compiler = new QuercusCompiler();

      int i = compiler.configureFromArgs(args);
        
      System.out.println("Scanning for PHP files to compile");

      ArrayList<CompileItem> pendingClasses = new ArrayList<CompileItem>();

      if (i == args.length) {
        compiler.generate(pendingClasses, ".");
      }

      for (; i < args.length; i++) {
        String uri = args[i];

        compiler.generate(pendingClasses, uri);
      }

      compiler.compile(pendingClasses);
    } finally {
    }
  }
  
  private int configureFromArgs(String []args)
  {
    int i = 0;
    
    while (i < args.length) {
      if (args[i].equals("-output-dir")) {
        Path path = Vfs.lookup(args[i + 1]);
        
        if (path != null)
          path = path.lookup("WEB-INF/classes");
        
        setWorkDir(path);
        i += 2;
      }
      else if (args[i].equals("-compiler")) {
        JavacConfig.getLocalConfig().setCompiler(args[i + 1]);

        i += 2;
      }
      else if (args[i].equals("-script-encoding")) {
        setScriptEncoding(args[i + 1]);
      }
      else if (args[i].equals("-require-source")) {
        setRequireSource("true".equals(args[i + 1]));
        
        i += 2;
      }
      else
        break;
    }
    
    return i;
  }
  
  public void generate(ArrayList<CompileItem> pendingClasses, String uri)
    throws Exception
  {
    try {
      Path path = Vfs.lookup(uri);

      if (path.isDirectory())
        generateDirectory(path, this, pendingClasses);
      else
        generatePhp(path, this, pendingClasses);
    } finally {
    }
  }
  
  private void generateDirectory(Path path,
                                 QuercusCompiler compiler,
                                 ArrayList<CompileItem> pendingClasses)
    throws Exception
  {
    if (path.isDirectory()) {
      String []list = path.list();

      for (int i = 0; i < list.length; i++) {
        Path subpath = path.lookup(list[i]);

        generateDirectory(subpath, compiler, pendingClasses);
      }
    }
    else if (path.getPath().endsWith(".php")) {
      generatePhp(path, compiler, pendingClasses);
    }
  }
  
  private void generatePhp(Path path,
                           QuercusCompiler compiler,
                           ArrayList<CompileItem> pendingClasses)
    throws Exception
  {
    QuercusProgram program = QuercusParser.parse(_quercus,
                                                 path,
                                                 _quercus.getScriptEncoding(),
                                                 null,
                                                 -1);

    CompileItem item = new CompileItem(program, path);
    
    if (compiler.generateCode(item))
      pendingClasses.add(item);
  }

  private boolean generateCode(CompileItem item)
  {
    QuercusGenerator gen = new QuercusGenerator(_quercus);

    try {
      String relPath = getRelativePath(item.getPath());

      String []files = gen.generate(item.getProgram(),
                                    relPath,
                                    false);

      item.setPendingFiles(files);

      return true;
    } catch (JavaCompileException e) {
      //log.log(Level.FINE, L.l("Quercus[{0}] loading interpreted page instead because of compile error",
      //                        item.getPath()), e);
      
      item.getProgram().setCompilable(false);
      item.getProgram().setCompileException(e);
      
      System.out.println("Cannot generate " + item.getPath() + " : " + e.getMessage());
      
    } catch (Exception e) {
      //log.log(Level.FINE, L.l("Quercus[{0}] loading interpreted page instead because of compile error",
      //    item.getPath()), e);

      item.getProgram().setCompilable(false);
      item.getProgram().setCompileException(e);
      
      System.out.println("Cannot generate " + item.getPath() + " : " + e.getMessage());
    }

    return false;
  }
  
  /**
   * Returns the class name.
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
  
  /**
   * Gets the owning directory.
   */
  public Path getPwd()
  {
    return _quercus.getPwd();
  }
  
  /**
   * Compiles the items, returning the broken items.
   */
  public ArrayList<CompileItem> compile(ArrayList<CompileItem> itemList)
  {
    QuercusGenerator gen = new QuercusGenerator(_quercus);

    ArrayList<String> pendingFileList = new ArrayList<String>();

    for (CompileItem item : itemList) {
      for (String file : item.getPendingFiles())
        pendingFileList.add(file);
    }

    System.out.println("Compiling " + pendingFileList.size() + " files");

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

      System.out.println("Cannot compile " + itemList.get(0).getPath() + " : " + e.getMessage());

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

        System.out.println("Cannot compile " + itemList.get(0).getPath() + " : " + e.getMessage());
        
        item.getProgram().setCompilable(false);
        
        brokenItems.add(item);
      }
    }

    return brokenItems;
  }
  
  private void load(QuercusGenerator gen, CompileItem item)
  {
    try {
      Class pageClass = gen.preload(item.getProgram());

    } catch (ClassFormatError e) {
      item.getProgram().setCompilable(false);
      log.log(Level.WARNING, e.toString(), e);

    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      item.getProgram().setCompilable(false);
    } finally {
      item.getProgram().finishCompiling();
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
