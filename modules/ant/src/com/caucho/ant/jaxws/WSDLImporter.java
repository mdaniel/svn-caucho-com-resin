/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

import javax.xml.bind.*;
import javax.xml.stream.*;

import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.soap.wsdl.*;

import com.caucho.server.util.CauchoSystem;

import com.caucho.xml.schema.*;

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

  public String toString()
  {
    return "wsdl = " + _wsdl + "\n" +
           "destDir = " + _destDir + "\n" +
           "sourceDestDir = " + _sourceDestDir + "\n" +
           "keep = " + _keep + "\n" +
           "verbose = " + _verbose + "\n" +
           "extension = " + _extension + "\n" +
           "debug = " + _debug + "\n" +
           "fork = " + _fork + "\n" +
           "wsdlLocation = " + _wsdlLocation + "\n" +
           "catalog = " + _catalog + "\n" +
           "package = " + _package + "\n" +
           "bindingPath = " + _bindingPath + "\n" +
           "binding = " + _binding + "\n";
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
  
  public void addJvmarg(Jvmarg jvmarg)
  {
  }

  /**
   * Executes the ant task.
   **/
  public void execute()
    throws org.apache.tools.ant.BuildException
  {
    try {
      JAXBContext context = 
        JAXBContext.newInstance("com.caucho.soap.wsdl:com.caucho.xml.schema");
      Unmarshaller u = context.createUnmarshaller();
      WSDLDefinitions wsdl = (WSDLDefinitions) u.unmarshal(new File(_wsdl));

      if (_package == null)
        _package = _binding.getPackage();

      wsdl.generateJava(u, new File(_sourceDestDir), new File(_destDir), 
                        _package);
    }
    catch (Exception e) {
      throw new org.apache.tools.ant.BuildException(e);
    }
  }

  public static void main(String[] args)
    throws Exception 
  {
    if (args.length < 3)
      error("usage: WSDLImporter <WSDL> <destination directory> <package>");

    WSDLImporter importer = new WSDLImporter();

    importer.setWsdl(args[0]);
    importer.setDestdir(args[1]);
    importer.setPackage(args[2]);

    try {
      importer.execute();
    }
    catch (org.apache.tools.ant.BuildException e) {
      error("Unable to load WSDL (" + args[0] + ")", e);
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

  public static class Binding {
    private File _dir;
    private String _includes;
    private String _excludes; // undocumented in tck

    public File getDir()
    {
      return _dir;
    }

    public void setDir(String dir)
    {
      _dir = new File(dir);
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

    public String toString()
    {
      return "Binding[dir=" + _dir + "," +
                     "includes=" + _includes + "," +
                     "excludes=" + _excludes + "]";
    }

    public String getPackage()
    {
      XMLStreamReader in = null;

      try {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        String[] includes = _includes.split(" ");

        for (int i = 0; i < includes.length; i++) {
          File include = new File(_dir, includes[i]);

          if (! include.exists())
            continue;

          in = factory.createXMLStreamReader(new FileInputStream(include));

          while (in.hasNext()) {
            in.next();

            if (in.getEventType() == in.START_ELEMENT &&
                in.getName() != null && 
                in.getName().getLocalPart().equals("package")) {
              String pkg = in.getAttributeValue(null, "name");

              if (pkg != null)
                return pkg;
            }
          }
        }
      }
      catch (IOException e) {
      }
      catch (XMLStreamException e) {
      }
      finally {
        try {
          if (in != null)
            in.close();
        }
        catch (XMLStreamException e) {
        }
      }

      return null;
    }
  }
}


