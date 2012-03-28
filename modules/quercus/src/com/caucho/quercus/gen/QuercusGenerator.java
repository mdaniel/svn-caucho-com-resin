/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.gen;

import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.make.VersionDependency;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.vfs.Path;
  
/**
 * Generator.
 */
public class QuercusGenerator {
  private final QuercusContext _quercus;

  public QuercusGenerator(QuercusContext quercus)
  {
    _quercus = quercus;
  }

  public Class<?> preload(GenClass cl)
    throws Exception
  {
    return preload(cl.getFullClassName());
  }


  public Class<?> preload(String className)
    throws Exception
  {
    JavaClassGenerator gen = createGenerator(true);

    return gen.preload(className);
  }

  public boolean preloadExists(String className)
  {
    JavaClassGenerator gen = new JavaClassGenerator();
    gen.setSearchPath(_quercus.getPwd());
    gen.setLoader(_quercus.getCompileClassLoader());

    return gen.preloadExists(className);
  }

  public Path getClassFilePath(QuercusProgram program)
  {
    String className = _quercus.getClassName(program.getSourcePath());
    
    JavaClassGenerator gen = createGenerator(true);

    return gen.getClassFilePath(className);
  }

  public Class<?> preload(QuercusProgram program)
    throws Exception
  {
    JavaClassGenerator gen = createGenerator(true);

    String className = _quercus.getClassName(program.getSourcePath());

    Class<?> pageClass = gen.preload(className);
    
    return pageClass;
  }

  public Class<?> load(QuercusProgram program)
    throws Exception
  {
    JavaClassGenerator gen = createGenerator(false);

    String className = _quercus.getClassName(program.getSourcePath());

    Class<?> pageClass = gen.load(className);
    
    return pageClass;
  }

  public Class<?> preloadProfile(QuercusProgram program)
    throws Exception
  {
    JavaClassGenerator gen = createGenerator(true);

    String className = _quercus.getClassName(program.getSourcePath());

    Class<?> pageClass = gen.preload(className + "__prof");

    return pageClass;
  }

  public String []generate(QuercusProgram program,
                           String userPath,
                           boolean isLazy)
    throws Exception
  {
    return generate(program, userPath, isLazy, false);
  }

  public String []generateProfile(QuercusProgram program, String userPath)
    throws Exception
  {
    return generate(program, userPath, false, true);
  }

  private String []generate(QuercusProgram program,
                            String userPath,
                            boolean isLazy,
                            boolean isProfile)
    throws Exception
  {
    if (isLazy)
      program.waitForRuntimeFunctionList(2000);
    
    JavaClassGenerator gen = createGenerator(false);

    String className = _quercus.getClassName(program.getSourcePath());

    if (isProfile)
      className = className + "__prof";
    
    GenClass cl = new GenClass(className);
    
    // Java generation code
    cl.setSuperClassName("com.caucho.quercus.page.QuercusPage");

    cl.addImport("com.caucho.quercus.*");
    cl.addImport("com.caucho.quercus.classes.*");
    cl.addImport("com.caucho.quercus.env.*");
    cl.addImport("com.caucho.quercus.expr.*");
    cl.addImport("com.caucho.quercus.function.*");
    cl.addImport("com.caucho.quercus.program.*");
    cl.addImport("com.caucho.quercus.lib.*");
    
    QuercusMain main = new QuercusMain(program, className);
    main.setProfile(isProfile);
    main.setUserPath(userPath);
    
    cl.addComponent(main);

    cl.addDependencyComponent().addDependency(new VersionDependency());
    cl.addDependencyComponent().addDependencyList(program.getDependencyList());

    // gen.setEncoding("JAVA");
    
    gen.generate(cl);

    return gen.getPendingFiles();
  }

  public void compile(String []files)
    throws Exception
  {
    JavaClassGenerator gen = createGenerator(false);

    gen.addPendingFiles(files);

    gen.compilePendingJava();
  }

  private JavaClassGenerator createGenerator(boolean isPreload)
  {
    JavaClassGenerator gen = new JavaClassGenerator();
    if (! isPreload)
      gen.setLoader(_quercus.getCompileClassLoader());
    gen.setSearchPath(_quercus.getPwd());
    gen.setWorkDir(_quercus.getWorkDir());
    
    return gen;
  }
}

