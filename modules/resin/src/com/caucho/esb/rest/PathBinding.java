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
 * @author Emil Ong
 */

package com.caucho.esb.rest;

import java.io.IOException;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/**
 * A binding for REST services.
 */
public class PathBinding implements RestBinding {
  private static final Logger log = 
    Logger.getLogger(PathBinding.class.getName());

  public boolean invoke(HttpServletRequest req, HttpServletResponse resp, 
                        RestSkeleton skeleton, Object service)
    throws ServletException, IOException
  {
    String pathInfo = req.getPathInfo();

    if (pathInfo == null)
      return false;

    if (pathInfo.length() > 0 && pathInfo.charAt(0) == '/')
      pathInfo = pathInfo.substring(1);

    String methodName;
    String[] arguments;

    String[] methodArguments = pathInfo.split("/", 2);
    methodName = methodArguments[0];

    if (methodArguments.length > 1) {
      arguments = methodArguments[1].split("/");

      // Hack to allow paths like
      //   http://www.foo.com/method/
      // to invoke method(void) instead of method(String st) as method("")
      if (arguments.length == 1 && arguments[0].length() == 0)
        arguments = new String[0];
    }
    else
      arguments = new String[0];

    try {
      Object result = skeleton.invoke(service, req.getMethod(), methodName, 
                                      arguments, req.getInputStream());

      JAXBContext context = JAXBContext.newInstance("");
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      marshaller.marshal(result, resp.getOutputStream());
    } 
    catch (NoSuchMethodException e) {
      return false;
    }
    catch (Throwable e) {
      throw new ServletException(e);
    }

    return true;
  }
}
