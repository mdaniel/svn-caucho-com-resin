/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.rmi.*;

import java.sql.*;

import javax.ejb.*;
import javax.sql.*;
import javax.naming.*;
import javax.transaction.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.sql.*;
import com.caucho.ejb.*;
import com.caucho.naming.*;
import com.caucho.loader.*;
import com.caucho.log.Log;
import com.caucho.server.http.*;

import com.caucho.ejb.admin.*;

/**
 * Compiles EJB classes prior to instantiating in the server.
 */
public class EJBCompiler {
  static L10N L = new L10N(EJBCompiler.class);
  protected static Logger log = Log.open(EJBCompiler.class);

  private Path _classDir;
  private Path _appDir;
  private ArrayList<String> _ejbPath = new ArrayList<String>();

  public EJBCompiler(String []args)
    throws Exception
  {
    int i = 0;

    while (i < args.length) {
      if (args[i].equals("-app-dir")) {
        _appDir = Vfs.lookup(args[i + 1]);
        if (_classDir == null)
          _classDir = _appDir.lookup("WEB-INF/work");
        i += 2;
      }
      else if (args[i].equals("-class-dir")) {
        _classDir = Vfs.lookup(args[i + 1]);
        i += 2;
      }
      else
        break;
    }

    if (i == args.length) {
      printUsage();
      throw new Exception("bad args");
    }

    for (; i < args.length; i++)
      _ejbPath.add(args[i]);
  }

  public void compile()
    throws Exception
  {
    ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
    
    // wrap so the EJB code will work.
    EnvironmentClassLoader loader;

    loader = new EnvironmentClassLoader(oldLoader);
    if (_appDir != null) {
      loader.addLoader(new SimpleLoader(_appDir.lookup("WEB-INF/classes"),
					null));
    }

    Thread.currentThread().setContextClassLoader(loader);

    try {
      EjbServerManager container = EjbServerManager.createLocal();
      container.setValidateDatabaseSchema(false);
      if (_classDir == null)
	container.setWorkPath(Vfs.lookup("."));
      else {
	container.setWorkPath(_classDir);
      }

      MergePath mergePath = new MergePath();
      mergePath.addClassPath(loader);
      if (_appDir != null)
	mergePath.addMergePath(_appDir.lookup("WEB-INF"));
      if (_classDir != null)
	mergePath.addMergePath(_classDir);

      // container.setSearchPath(mergePath);

      for (int i = 0; i < _ejbPath.size(); i++)
	container.addEJBPath(mergePath.lookup(_ejbPath.get(i)));
    
      container.build();
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
  }

  public static void main(String []args)
    throws Throwable
  {
    if (args.length == 0) {
      printUsage();
      System.exit(1);
    }

    try {
      new EJBCompiler(args).compile();
    } catch (Throwable e) {
      while (e instanceof ExceptionWrapper &&
             ((ExceptionWrapper) e).getRootCause() != null) {
        e = ((ExceptionWrapper) e).getRootCause();
      }

      throw e;
    }
  }

  private static void printUsage()
  {
    System.out.println("usage: com.caucho.ejb.EJBCompiler [flags] foo.ejb");
    System.out.println(" -class-dir: The directory where the classes will be generated.");
    System.out.println(" -app-dir: The source (web-app) directory.");
  }
}

