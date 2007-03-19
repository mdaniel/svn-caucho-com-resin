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

package com.caucho.ant.jaxws;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.server.util.CauchoSystem;

/**
 * Command-line tool and ant task to generate WSDLs from WebService 
 * annotated classes.
 */
public class WSDLGenerator extends org.apache.tools.ant.Task {
  private static final Logger log 
    = Logger.getLogger(WSDLGenerator.class.getName());

  private String _sei;
  private String _destDir;
  private String _classpath;
  private String _resourceDestDir;
  private String _sourceDestDir;
  private boolean _keep;
  private boolean _verbose;
  private boolean _genWsdl;
  private boolean _extension;
  private boolean _debug; // undocumented in TCK
  private boolean _fork;// undocumented in TCK
  private String _protocol;
  private String _serviceName;
  private String _portName;

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

  public void setSei(String sei)
  {
    System.out.println("sei = " + sei);

    _sei = sei;
  }

  public void setDestdir(String destDir)
  {
    System.out.println("destDir = " + destDir);

    _destDir = destDir;
  }

  public void setClasspath(String classpath)
  {
    System.out.println("classpath = " + classpath);

    _classpath = classpath;
  }

  public void setCp(String classpath)
  {
    System.out.println("classpath = " + classpath);

    setClasspath(classpath);
  }

  public void addClasspath(ClassPath classPath)
  {
    System.out.println("added classpath, " + classPath);

    _classpath = classPath.getPath();
  }

  public void addJvmarg(Jvmarg jvmarg)
  {
  }

  public void setResourcedestdir(String resourceDestDir)
  {
    System.out.println("resourceDestDir = " + resourceDestDir);

    _resourceDestDir = resourceDestDir;
  }

  public void setSourcedestdir(String sourceDestDir)
  {
    System.out.println("sourceDestDir = " + sourceDestDir);

    _sourceDestDir = sourceDestDir;
  }

  public void setKeep(boolean keep)
  {
    System.out.println("keep = " + keep);

    _keep = keep;
  }

  public void setVerbose(boolean verbose)
  {
    System.out.println("verbose = " + verbose);

    _verbose = verbose;
  }

  public void setGenwsdl(boolean genWsdl)
  {
    System.out.println("genWsdl = " + genWsdl);

    _genWsdl = genWsdl;
  }

  public void setDebug(boolean debug)
  {
    System.out.println("debug = " + debug);

    _debug = debug;
  }

  public void setFork(boolean fork)
  {
    System.out.println("fork = " + fork);

    _fork = fork;
  }

  public void setProtocol(String protocol)
  {
    System.out.println("protocol = " + protocol);

    _protocol = protocol;
  }

  public void setServicename(String serviceName)
  {
    System.out.println("serviceName = " + serviceName);

    _serviceName = serviceName;
  }

  public void setPortname(String portName)
  {
    System.out.println("portName = " + portName);

    _portName = portName;
  }
  
  public void setExtension(boolean extension)
  {
    System.out.println("extension = " + extension);

    _extension = extension;
  }

  /**
   * Executes the ant task.
   **/
  public void execute()
    throws org.apache.tools.ant.BuildException
  {
    Class seiClass = null;

    try {
      seiClass = Class.forName(_sei);
    }
    catch (ClassNotFoundException e) {
      throw new org.apache.tools.ant.BuildException(e);
    }

    WebServiceIntrospector introspector = new WebServiceIntrospector();
    
    try {
      if (_resourceDestDir != null && ! "".equals(_resourceDestDir))
        introspector.introspect(seiClass, "").dumpWSDL(_resourceDestDir);
      else
        introspector.introspect(seiClass, "").dumpWSDL(System.out);
    }
    catch (Exception e) {
      throw new org.apache.tools.ant.BuildException(e);
    }
  }

  public static void main(String[] args)
  {
    if (args.length < 1)
      error("usage: WSDLGenerator <SEI>");

    WSDLGenerator generator = new WSDLGenerator();

    generator.setSei(args[0]);

    try {
      generator.execute();
    }
    catch (org.apache.tools.ant.BuildException e) {
      error("Unable to load SEI (" + args[0] + ")", e);
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

  public static class Jvmarg {
    public void setLine(String line)
    {
    }

    public String getLine()
    {
      return "";
    }
  }
}


