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
 * Command-line tool and ant task to generate Java from WSDLs.
 */
public class WSDLImporter extends org.apache.tools.ant.Task {
  private String _wsdl;
  private String _destDir;
  private String _sourceDestDir;
  private boolean _keep;
  private boolean _verbose;
  private boolean _extension;
  private boolean _debug;
  private boolean _fork;
  private String _wsdlLocation;
  private String _catalog;
  private String _package;
  private String _bindingPath;
  private Binding _binding;

  private static void error(String msg)
  {
    System.err.println(msg);
    System.exit(1);
  }

  public void setWsdl(String wsdl)
  {
    _wsdl = wsdl;
  }

  public void setDestdir(String destDir)
  {
    _destDir = destDir;
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

  public void setExtension(boolean extension)
  {
    _extension = extension;
  }

  public void setDebug(boolean debug)
  {
    _debug = debug;
  }

  public void setFork(boolean fork)
  {
    _fork = fork;
  }

  public void setWsdllocation(String wsdlLocation)
  {
    _wsdlLocation = wsdlLocation;
  }

  public void setCatalog(String catalog)
  {
    _catalog = catalog;
  }

  public void setPackage(String pkg)
  {
    _package = pkg;
  }

  public void addBinding(Binding binding)
  {
    _binding = binding;
  }
  
  public void setBinding(String bindingPath)
  {
    _bindingPath = bindingPath;
  }
  
  /**
   * Executes the ant task.
   **/
  public void execute()
    throws org.apache.tools.ant.BuildException
  {
    // XXX
  }

  public static void main(String[] args)
    throws Exception 
  {
  }

  public static class Binding {
    private String _dir;
    private String _includes;
    private String _excludes; // undocumented in tck

    public String getDir()
    {
      return _dir;
    }

    public void setDir(String dir)
    {
      _dir = dir;
    }

    public String getIncludes()
    {
      return _includes;
    }

    public void setIncludes(String includes)
    {
      _includes = includes;
    }

    public String getExcludes()
    {
      return _excludes;
    }

    public void setExcludes(String excludes)
    {
      _excludes = excludes;
    }
  }
}


