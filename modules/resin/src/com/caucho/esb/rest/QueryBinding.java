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

/**
 * A binding for REST services.
 */
public class QueryBinding implements RestBinding {
  private static final Logger log = 
    Logger.getLogger(QueryBinding.class.getName());

  public boolean invoke(HttpServletRequest req, HttpServletResponse resp, 
                        RestSkeleton skeleton, Object service)
    throws ServletException, IOException
  {
    if (req.getQueryString() == null)
      return false;

    Map<String,String> arguments = queryToMap(req.getQueryString());

    String methodName = arguments.remove("method");

    if (methodName == null)
      return false;

    try {
      Object result = 
        skeleton.invoke(service, req.getMethod(), methodName, arguments);

      PrintStream out = new PrintStream(resp.getOutputStream());

      /* // XXX Reinstate when marshalling starts working
      JAXBContext context = JAXBContext.newInstance("");
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      marshaller.marshal(result, out);
      */

      out.print(result.toString());

      out.flush();
    } 
    catch (NoSuchMethodException e) {
      return false;
    }
    catch (Throwable e) {
      throw new ServletException(e);
    }

    return true;
  }

  private static Map<String,String> queryToMap(String query)
  {
    HashMap<String,String> parameterMap = new HashMap<String,String>();
    String[] entries = query.split("&");

    for (String entry : entries) {
      if (entry.indexOf("=") < 0)
        continue;

      String[] nameValue = entry.split("=", 2);

      parameterMap.put(nameValue[0], nameValue[1]);
    }

    return parameterMap;
  }
}
