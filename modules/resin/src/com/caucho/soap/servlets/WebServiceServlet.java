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
 * @author Adam Megacz
 */

package com.caucho.soap.servlets;

import java.lang.reflect.*;
import java.io.*;

import com.caucho.vfs.*;
import com.caucho.log.*;
import javax.xml.namespace.*;
import javax.xml.bind.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;
import javax.xml.soap.*;
import javax.xml.stream.*;
import com.caucho.soap.reflect.*;
import com.caucho.soap.skeleton.*;

/**
 * An HTTP Servlet to dispatch SOAP requests
 */
public class WebServiceServlet extends HttpServlet {

  public final static String XMLNS =
    "http://www.w3.org/2000/xmlns/";
  public final static String SOAP_ENVELOPE =
    "http://www.w3.org/2003/05/soap-envelope";
  public final static String SOAP_ENCODING =
    "http://schemas.xmlsoap.org/soap/encoding/";

  private Object _object;
  private Class _class;
  private DirectSkeleton _skeleton;
  private String _namespace;
  private boolean _wrapped;

  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
    // XXX: show that nifty debugging screen like Sun does
    
    if (req.getParameter("wsdl") != null) {
      resp.setContentType("text/xml");
      skeleton().dumpWSDL(Vfs.openWrite(resp.getOutputStream()),
			  req.getRequestURL().toString());
      return;
    }
  }
           
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
    try {

      XMLInputFactory inputFactory = XMLInputFactory.newInstance();

      XMLStreamReader xmlReader =
        inputFactory.createXMLStreamReader(req.getInputStream());
      
      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

      XMLStreamWriter xmlWriter =
        outputFactory.createXMLStreamWriter(resp.getOutputStream());
      
      skeleton().invoke(_object, xmlReader, xmlWriter);
      xmlWriter.flush();
    }
    catch (XMLStreamException e) {
      throw new ServletException(e);
    }
  }

  public void setService(Object o)
  {
    _object = o;
    if (_class==null)
      _class = o.getClass();
  }

  public void setImplementationClass(String s)
    throws Exception
  {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    
    setService(classLoader.loadClass(s).newInstance());
  }

  public void setInterfaceClass(String s)
    throws Exception
  {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    
    setInterface(classLoader.loadClass(s));
  }

  public void setNamespace(String namespace)
  {
    // only used for WSDL generation
    _namespace = namespace;
  }

  public void setWrapped(boolean wrapped)
  {
    // XXX: use this
    _wrapped = wrapped;
  }

  public void setInterface(Class c)
  {
    _class = c;
  }

  private DirectSkeleton skeleton()
  {
    if (_skeleton == null)
      _skeleton = new WebServiceIntrospector().introspect(_class);
    return _skeleton;
  }

}
