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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.ant.jaxb;

import java.io.*;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.transform.dom.*;

import com.caucho.java.*;
import com.caucho.jaxb.*;
import com.caucho.loader.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;

/**
 * Command-line tool and ant task to generate Xml Schema from
 * annotated classes.
 */
public class SchemaGenerator extends org.apache.tools.ant.Task {
  private List<String> _sourceFiles = new ArrayList<String>();
  private List<String> _classFiles = new ArrayList<String>();
  private String _outputDir; // == _destDir + _package
  private String _destDir;
  private String _package;
  private boolean _verbose;
  private boolean _debug;
  private boolean _fork;

  /**
   * For ant.
   **/
  public SchemaGenerator()
  {
  }

  public SchemaGenerator(String[] args)
  {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-p")) {
        if (i + 1 == args.length)
          usage();

        _package = args[++i];
      }
      else if (args[i].equals("-d")) {
        if (i + 1 == args.length)
          usage();

        _destDir = args[++i];
      }
      else {
        if (args[i].endsWith(".java"))
          _sourceFiles.add(args[i]);
        else if (args[i].endsWith(".class"))
          _classFiles.add(args[i]);
        else
          usage();
      }
    }

    if (_destDir == null)
      usage();

    if (_package == null)
      usage();

    _outputDir = _destDir + File.separator + 
                 _package.replace('.', File.separatorChar);
  }

  private static void error(String msg)
  {
    System.err.print(msg);
    System.exit(1);
  }

  private static void error(String msg, Exception e)
  {
    System.err.print(msg + ":" );
    e.printStackTrace();
    System.exit(1);
  }

  private static void usage()
  {
    error("usage: schema-gen -p <package> -d <output dir> -cp <classpath> " +
                 "<Java files>");
  }

  public void setDestdir(String destDir)
  {
    _destDir = destDir;
  }

  /*
  public void setClasspath(String classpath)
  {
    _classpath = classpath;
  }

  public void setCp(String classpath)
  {
    setClasspath(classpath);
  }

  public void addClasspath(ClassPath classPath)
  {
    _classpath = classPath.getPath();
  }*/

  public void setVerbose(boolean verbose)
  {
    _verbose = verbose;
  }

  public void setDebug(boolean debug)
  {
    _debug = debug;
  }

  public void setFork(boolean fork)
  {
    _fork = fork;
  }

  /**
   * Executes the ant task.
   **/
  public void execute()
    throws org.apache.tools.ant.BuildException
  {
    FileWriter fileWriter = null;

    try {
      int i = 0;
      Class[] classes = new Class[_sourceFiles.size() + _classFiles.size()];

      ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
      GreedyClassLoader classLoader = new GreedyClassLoader(parentLoader);
      Thread.currentThread().setContextClassLoader(classLoader);

      if (_sourceFiles.size() > 0) {
        Path outputDirPath = Vfs.lookup(_outputDir);

        if (outputDirPath.exists()) {
          if (! outputDirPath.isDirectory())
            throw new org.apache.tools.ant.BuildException("Output path specified is not a directory: " + outputDirPath);
        }
        else if (! outputDirPath.mkdir()) {
          throw new org.apache.tools.ant.BuildException("Unable to create directory at output path: " + outputDirPath);
        }

        JavaCompiler compiler = JavaCompiler.create();
        compiler.setClassDir(outputDirPath);
        compiler.setSourceDir(Vfs.lookup());

        LineMap lineMap = new LineMap();

        for (String sourceFile : _sourceFiles)
          compiler.compile(sourceFile, lineMap);

        classLoader.loadClassFiles(new File(_outputDir));
      }

      /* XXX: TCK doesn't use classes, but schemagen.sh allows them.
      for (String classFile : _classFiles)
        classes[i++] = classLoader.loadClassFile(classFile);
        */

      JAXBContextImpl context = 
        new JAXBContextImpl(classLoader.getLoadedClasses(), null);
      
      XMLOutputFactory factory = XMLOutputFactory.newInstance();

      DOMResult result = new DOMResult();
      XMLStreamWriter out = factory.createXMLStreamWriter(result);

      out.writeStartDocument("UTF-8", "1.0");
      context.generateSchemaWithoutHeader(out);
      out.close();

      fileWriter = new FileWriter(_outputDir + "/schema1.xsd");

      XmlPrinter xmlPrinter = new XmlPrinter(fileWriter);
      xmlPrinter.setPrintDeclaration(true);
      xmlPrinter.setEncoding("UTF-8");
      xmlPrinter.setStandalone("yes");
      xmlPrinter.printPrettyXml(result.getNode());
    }
    catch (Exception e) {
      throw new org.apache.tools.ant.BuildException(e);
    }
    finally {
      try { 
        if (fileWriter != null) 
          fileWriter.close();
      }
      catch (IOException e) {
        throw new org.apache.tools.ant.BuildException(e);
      }
    }
  }
  
  public static void main(String[] args)
  {
    if (args.length < 1)
      usage();

    try {
      new SchemaGenerator(args).execute();
    }
    catch (org.apache.tools.ant.BuildException e) {
      error("Unable to generate schema", e);
    }
  }

  public static class ClassPath {
    private List<PathElement> _pathElements = new ArrayList<PathElement>();
    private String _path;

    public String getPath()
    {
      return _path;
    }

    public void setPath(String path)
    {
      _path = path;

      String[] elements = _path.split(":");

      for (String element : elements)
        _pathElements.add(new PathElement(element));
    }

    public List<PathElement> getPathElements()
    {
      return _pathElements;
    }

    public void addPathelement(PathElement pathElement)
    {
      _pathElements.add(pathElement);
    }

    public static class PathElement {
      private String _path;

      public PathElement()
      {
      }

      public PathElement(String path)
      {
        _path = path;
      }

      public String getPath()
      {
        return _path;
      }

      public void setPath(String path)
      {
        _path = path;
      }
    }
  }
}


