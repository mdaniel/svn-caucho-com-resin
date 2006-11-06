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

import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.util.CauchoSystem;

import java.io.File;
import java.io.OutputStream;

/**
 * Command-line tool and ant task to generate WSDLs from WebService 
 * annotated classes.
 */
public class WSDLGenerator extends org.apache.tools.ant.Task {
  private String _sei;
  private String _destDir;
  private String _classpath;
  private String _resourceDestDir;
  private String _sourceDestDir;
  private boolean _keep;
  private boolean _verbose;
  private boolean _genWsdl;
  private boolean _extension;
  private boolean _debug; // not documented in TCK, but is used (!)
  private String _protocol;
  private String _serviceName;
  private String _portName;

  private static void error(String msg)
  {
    System.err.println(msg);
    System.exit(1);
  }

  public void setSei(String sei)
  {
    _sei = sei;
  }

  public void setDestdir(String destDir)
  {
    _destDir = destDir;
  }

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
  }

  public void setResourcedestdir(String resourceDestDir)
  {
    _resourceDestDir = resourceDestDir;
  }

  public void setSourcedestdir(String sourceDestDir)
  {
    _sourceDestDir = sourceDestDir;
  }

  public void setKeep(boolean keep)
  {
    _keep = keep;
  }

  public void setVerbose(boolean verbose)
  {
    _verbose = verbose;
  }

  public void setGenwsdl(boolean genWsdl)
  {
    _genWsdl = genWsdl;
  }

  public void setDebug(boolean debug)
  {
    _debug = debug;
  }

  public void setProtocol(String protocol)
  {
    _protocol = protocol;
  }

  public void setServicename(String serviceName)
  {
    _serviceName = serviceName;
  }

  public void setPortname(String portName)
  {
    _portName = portName;
  }
  
  public void setExtension(boolean extension)
  {
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
      seiClass = CauchoSystem.loadClass(_sei);
    }
    catch (ClassNotFoundException e) {
      throw new org.apache.tools.ant.BuildException(e);
    }

    WebServiceIntrospector introspector = new WebServiceIntrospector();
    
    try {
      // XXX
      introspector.introspect(seiClass, "").dumpWSDL(System.out);
    }
    catch (Exception e) {
      throw new org.apache.tools.ant.BuildException(e);
    }
  }

  public static void main(String[] args)
  {
    if (args.length < 1)
      error("usage: wsdl-gen <SEI>");

    WSDLGenerator generator = new WSDLGenerator();

    generator.setSei(args[0]);

    try {
      generator.execute();
    }
    catch (org.apache.tools.ant.BuildException e) {
      error("Unable to load SEI (" + args[0] + "): " + e);
    }
  }

  public static class ClassPath {
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


