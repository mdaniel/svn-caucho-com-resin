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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop;

import com.caucho.util.L10N;

import org.omg.CORBA.NO_IMPLEMENT;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IiopSkeleton extends DummyObjectImpl {
  private static final L10N L = new L10N(IiopSkeleton.class);
  private static final Logger log
    = Logger.getLogger(IiopSkeleton.class.getName());

  private static HashMap<String,String> _knownClasses;

  private ClassLoader _loader;
  private Class _remoteClass;
  private ArrayList<Class> _apiList;

  private Object _obj;

  public IiopSkeleton(Object obj, ArrayList<Class> apiList, ClassLoader loader,
                      String host, int port, String oid)
  {
    super(new IOR(apiList.get(0), host, port, oid));

    if (obj == null)
      throw new NullPointerException();

    _obj = obj;
    _apiList = apiList;
    _remoteClass = _apiList.get(0);
    _loader = loader;
  }

  Object getObject()
  {
    return _obj;
  }

  ArrayList<Class> getApiList()
  {
    return _apiList;
  }

  void service(Object obj, IiopReader reader, IiopWriter writer)
    throws Throwable
  {
    String op = reader.getOperation().toString();

    Method method = null;
    SkeletonMethod skelMethod = null;

    if (log.isLoggable(Level.FINE))
      log.fine("IIOP-call: " + _remoteClass.getName() + "." + op);

    if ((method = getMethod(op)) != null) {
      boolean isJava = ! _remoteClass.getName().startsWith("com.caucho.iiop");

      skelMethod = new SkeletonMethod(this, method, isJava);
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      try {
        thread.setContextClassLoader(_loader);

        skelMethod.service(obj, reader, writer);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }
    else if (serviceSystemMethod(obj, op, reader, writer)) {
    }
    else {
      throw new NO_IMPLEMENT(L.l("{0}: '{1}' is an unknown method.",
                                 _remoteClass.getName(), op));
    }
  }

  Method getMethod(String name)
  {
    for (int j = 0; j < _apiList.size(); j++) {
      Method []methods = _apiList.get(j).getMethods();

      for (int i = 0; i < methods.length; i++)
        if (methods[i].getName().equals(name))
          return methods[i];
    }

    return null;
  }

  boolean serviceSystemMethod(Object obj, String op, IiopReader reader, IiopWriter writer)
    throws Exception
  {
    if (op.equals("_is_a")) {
      String name = reader.read_string();

      String className = _knownClasses.get(name);

      if (className != null) {
      }
      else if (name.startsWith("RMI:")) {
        className = name.substring(4);
        int p = className.indexOf(':');
        if (p > 0)
          className = className.substring(0, p);
      }
      else
        className = name;

      Class cl = obj.getClass();

      boolean value = isA(cl, className);

      if (log.isLoggable(Level.FINE))
        log.fine("IIOP _is_a: " + obj.getClass() + " " + className + " " + value);

      writer.startReplyOk(reader.getRequestId());
      writer.write_boolean(value);

      return true;
    }
    else
      return false;
  }

  private boolean isA(Class cl, String className)
  {
    for (; cl != null; cl = cl.getSuperclass()) {
      if (cl.getName().equals(className))
        return true;

      Class []ifs = cl.getInterfaces();
      for (int i = 0; i < ifs.length; i++) {
        if (isA(ifs[i], className))
          return true;
      }
    }

    return false;
  }

  public String toString()
  {
    return "IiopSkeleton[" + _remoteClass.getName() + "]";
  }

  static {
    _knownClasses = new HashMap<String,String>();
    _knownClasses.put("IDL:omg.org/CosNaming/NamingContext:1.0",
                      "org.omg.CosNaming.NamingContext");
  }
}
