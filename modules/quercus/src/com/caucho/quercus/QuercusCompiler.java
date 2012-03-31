/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
  
  private ArrayList<String> _pathList = new ArrayList<String>();

  private Pattern _includePattern = Pattern.compile(".*\\.php$");
  
  private Level _loggingLevel = Level.FINE;
  
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
  
  public void setIncludePattern(String pattern)
  {
    _includePattern = Pattern.compile(pattern);
  }
  
  public void addCompilePath(String path) {
    _pathList.add(path);
  }
  
  public void setVerbose() {
    _loggingLevel = Level.INFO;
  }
  
  public static void main(String []args)
    throws Exception
  {
    if (args.length == 0) {
      System.out.println("usage: java -Xmx1024m " + QuercusCompiler.class + " [flags] php1 php2 ...");
      System.out.println(" -output-dir      : the directory to use for output (default /tmp/<user>).");
      System.out.println(" -compiler        : sets the javac.");
      System.out.println(" -script-encoding : the encoding of the source files (default ISO-8859-1).");
      System.out.println(" -require-source  : whether or not the source files are required (default false).");
      System.out.println(" -include-pattern : compile files that match this Java regexp Pattern (default '.*\\.php$').");
      System.out.println(" -verbose         : sets logging level to INFO.");
      
      System.exit(1);
    }

    QuercusCompiler compiler = new QuercusCompiler();
    configure(compiler, args);
    
    ArrayList<CompileItem> brokenItems = compiler.compile();
  }
  
  public ArrayList<CompileItem> compile()
    throws IOException
  {
    long start = System.currentTimeMillis();

    if (log.isLoggable(_loggingLevel)) {
      log.log(_loggingLevel, L.l("Parsing files matching Java Pattern '{0}'",
                                 _includePattern));
    }
    
    ArrayList<CompileItem> pendingClasses = new ArrayList<CompileItem>();

    for (String uri : _pathList) {      
      generate(pendingClasses, uri);
    }

    if (log.isLoggable(_loggingLevel)) {
      log.log(_loggingLevel, L.l("Compiling {0} PHP files.",
                                 pendingClasses.size()));
    }
    
    ArrayList<CompileItem> brokenList = compile(pendingClasses);
    
    long end = System.currentTimeMillis();
    
    if (log.isLoggable(_loggingLevel)) {
      log.log(_loggingLevel, L.l("Compilation finished in {0} ms.",
                                 (end - start)));
    }
    
    return brokenList;
  }
  
  private static void configure(QuercusCompiler compiler, String []args)
  {
    int i = 0;
    
    while (i < args.length) {
      if (args[i].equals("-output-dir")) {
        Path path = Vfs.lookup(args[i + 1]);
        
        if (path != null)
          path = path.lookup("WEB-INF/classes");
        
        compiler.setWorkDir(path);
        i += 2;
      }
      else if (args[i].equals("-compiler")) {
        JavacConfig.getLocalConfig().setCompiler(args[i + 1]);

        i += 2;
      }
      else if (args[i].equals("-script-encoding")) {
        compiler.setScriptEncoding(args[i + 1]);
        
        i += 2;
      }
      else if (args[i].equals("-require-source")) {
        compiler.setRequireSource("true".equals(args[i + 1]));
        
        i += 2;
      }
      else if (args[i].equals("-include-pattern")) {
        String pattern = args[i + 1];
        
        compiler.setIncludePattern(pattern);
        
        i += 2;
      }
      else if (args[i].equals("-verbose")) {
        compiler.setVerbose();
        
        i++;
      }
      else {
        break;
      }
    }
    
    if (i == args.length) {
      compiler.addCompilePath(".");
    }
    else {
      for (; i < args.length; i++) {
        compiler.addCompilePath(args[i]);
      }
    }
  }
  
  public void generate(ArrayList<CompileItem> pendingClasses, String uri)
    throws IOException
  {
    Path path = Vfs.lookup(uri);

    if (path.isDirectory())
      generateDirectory(pendingClasses, path);
    else {
      generateFile(pendingClasses, path);
    }
  }
  
  private void generateDirectory(ArrayList<CompileItem> pendingClasses,
                                 Path path)
    throws IOException
  {
    String []list = path.list();

    for (int i = 0; i < list.length; i++) {
      Path subPath = path.lookup(list[i]);

      if (subPath.isDirectory()) {
        generateDirectory(pendingClasses, subPath);
      }
      else {
        generateFile(pendingClasses, subPath);
      }
    }
  }
  
  private void generateFile(ArrayList<CompileItem> pendingClasses,
                            Path path)
    throws IOException
  {
    if (! _includePattern.matcher(path.getPath()).matches()) {
      return;
    }
    
    QuercusProgram program = QuercusParser.parse(_quercus,
                                                 path,
                                                 _quercus.getScriptEncoding(),
                                                 null,
                                                 -1);

    CompileItem item = new CompileItem(program, path);
    
    if (generateCode(item)) {
      pendingClasses.add(item);
    }
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
    }
    catch (Exception e) {
      log.log(Level.WARNING, L.l("Cannot generate code for {0} : {1}", item.getPath(), e.getMessage()));
      
      return false;
    }
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
    ArrayList<CompileItem> brokenItems = new ArrayList<CompileItem>();

    for (CompileItem item : itemList) {
      for (String file : item.getPendingFiles())
        pendingFileList.add(file);
    }

    String []pendingFiles = new String[pendingFileList.size()];
    pendingFileList.toArray(pendingFiles);

    // compile the files in one batch
    try {
      gen.compile(pendingFiles);

      for (CompileItem item : itemList) {
        // ensure class file is there and valid
        load(gen, item);
      }
      
      return brokenItems;
    }
    catch (Throwable e) {
    }
    
    // need to find the files that failed to compile
    for (CompileItem item : itemList) {
      try {
        gen.compile(item.getPendingFiles());

        // ensure class file is there and valid
        load(gen, item);
      }
      catch (Throwable e) {
        log.log(Level.WARNING, L.l("Cannot compile {0} : {1}", itemList.get(0).getPath(), e.getMessage()));
                
        brokenItems.add(item);
      }
    }

    return brokenItems;
  }
  
  private void load(QuercusGenerator gen, CompileItem item)
    throws Exception
  {
    Class<?> pageClass = gen.preload(item.getProgram());
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
