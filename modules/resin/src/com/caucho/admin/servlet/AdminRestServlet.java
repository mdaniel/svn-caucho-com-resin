/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 */

package com.caucho.admin.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.config.ConfigException;
import com.caucho.jmx.Jmx;
import com.caucho.jmx.MXName;
import com.caucho.management.server.ManagementMXBean;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

@SuppressWarnings("serial")
public class AdminRestServlet extends HttpServlet {
  private static final L10N L = new L10N(AdminRestServlet.class);
  
  private static final HashMap<Class<?>,Marshal> _marshalMap
    = new HashMap<Class<?>,Marshal>();
  
  private static final HashMap<String,Action> _actionMap
    = new HashMap<String,Action>();
  
  private boolean _isRequireSecure = true;
  
  public void setRequireSecure(boolean isSecure)
  {
    _isRequireSecure = isSecure;
  }
  
  public void service(HttpServletRequest req, 
                      HttpServletResponse res)
    throws IOException, ServletException
  {
    Principal user = req.getUserPrincipal();
    
    if (user == null) {
      res.setStatus(HttpServletResponse.SC_FORBIDDEN);
      
      PrintWriter out = res.getWriter();
      out.println(L.l("admin requires a valid user"));
      return;
    }
    else if (! req.isUserInRole("resin-admin")) {
      res.setStatus(HttpServletResponse.SC_FORBIDDEN);
      
      PrintWriter out = res.getWriter();
      out.println(L.l("admin requires a user in the resin-admin role"));
      return;
    }
    else if (_isRequireSecure && ! req.isSecure()) {
      res.setStatus(HttpServletResponse.SC_FORBIDDEN);
      
      PrintWriter out = res.getWriter();
      out.println(L.l("admin requires a secure connection"));
      return;
    }
    
    String actionName = req.getParameter("action");
    
    if (actionName == null) {
      res.setStatus(500);
      res.setContentType("text/plain");
      
      PrintWriter out = res.getWriter();
      out.println(L.l("action parameter is required"));
      return;
    }
    
    Action action = _actionMap.get(actionName);
    
    if (action == null) {
      res.setStatus(500);
      res.setContentType("text/plain");
      
      PrintWriter out = res.getWriter();
      out.println(L.l("'{0}' is an unknown action", actionName));
      return;
    }
    
    action.doAction(req, res);
  }
  
  static private void introspectManagementOperations()
  {
    Class<?> cl = ManagementMXBean.class;
    
    for (Method method : cl.getDeclaredMethods()) {
      String name = method.getName();
      
      if (name.startsWith("get") || name.startsWith("set"))
        continue;
      
      Action action = new Action(method);
      
      _actionMap.put(name, action);
    }
  }
  
  static class Action {
    private Method _method;

    private String []_parameterNames;
    
    private Marshal []_parameterMarshal;
    private Marshal _returnMarshal;
    
    private boolean _isPutStream;
    private boolean _isGetStream;
    
    Action(Method method)
    {
      _method = method;
      
      Class<?> []parameterTypes = method.getParameterTypes();
      
      _parameterNames = new String[parameterTypes.length];
      
      Annotation [][]paramAnn = method.getParameterAnnotations();
      
      for (int i = 0; i < _parameterNames.length; i++) {
        _parameterNames[i] = getParamName(paramAnn, i);
      }
      
      _parameterMarshal = new Marshal[parameterTypes.length];
      
      for (int i = 0; i < parameterTypes.length; i++) {
        Marshal marshal = _marshalMap.get(parameterTypes[i]);
        
        if (marshal == null)
          throw new IllegalStateException(method + " has unknown marshal type");
        
        _parameterMarshal[i] = marshal;
      }
      
      _returnMarshal = _marshalMap.get(method.getReturnType());
      
      if (_returnMarshal == null) {
        throw new IllegalStateException(method + " has unknown return type");
      }
       
      if (parameterTypes.length > 0
          && parameterTypes[parameterTypes.length - 1] == InputStream.class) {
        _isPutStream = true;
      }
      
      if (InputStream.class.equals(method.getReturnType())) {
        _isGetStream = true;
      }
    }
    
    private String getParamName(Annotation [][]paramAnn, int i)
    {
      if (paramAnn == null)
        return "p" + i;
      
      for (Annotation ann : paramAnn[i]) {
        if (ann.annotationType().equals(MXName.class)){
          MXName name = (MXName) ann;
          
          return name.value();
        }
      }
      
      return "p" + 1;
    }
    
    String []getParameterNames()
    {
      return _parameterNames;
    }
    
    void doAction(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException
    {
      ManagementMXBean management;
      
      try {
        management = (ManagementMXBean) Jmx.find("resin:type=Management");
      } catch (Exception e) {
        throw ConfigException.create(e);
      }

      Object []param = new Object[_parameterMarshal.length];
      
      for (int i = 0; i < param.length; i++) {
        param[i] = _parameterMarshal[i].marshal(req, _parameterNames[i]);
      }
      
      try {
        Object value = _method.invoke(management, param);
        
        _returnMarshal.unmarshal(res, value);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
  }
  
  abstract static class Marshal {
    abstract public Object marshal(HttpServletRequest request, String name)
      throws IOException;
    
    public void unmarshal(HttpServletResponse response, Object value)
      throws IOException
    {
      PrintWriter out = response.getWriter();
        
      out.println(value);
    }
  }
  
  static class StringMarshal extends Marshal {
    @Override
    public Object marshal(HttpServletRequest request, String name)
    {
      return request.getParameter(name);
    }
  }
  
  static class IntegerMarshal extends Marshal {
    @Override
    public Object marshal(HttpServletRequest request, String name)
    {
      String param = request.getParameter(name);
      
      if (param != null)
        return Integer.parseInt(param);
      else
        return 0;
    }
  }
  
  static class LongMarshal extends Marshal {
    @Override
    public Object marshal(HttpServletRequest request, String name)
    {
      String param = request.getParameter(name);
      
      if (param != null)
        return Long.parseLong(param);
      else
        return 0;
    }
  }
  
  static class VoidMarshal extends Marshal {
    @Override
    public Object marshal(HttpServletRequest request, String name)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
  
  static class InputStreamMarshal extends Marshal {
    @Override
    public Object marshal(HttpServletRequest request, String name)
      throws IOException
    {
      return request.getInputStream();
    }

    @Override
    public void unmarshal(HttpServletResponse response, Object value)
      throws IOException
    {
      InputStream is = (InputStream) value;
      
      OutputStream os = response.getOutputStream();
      
      WriteStream out = Vfs.openWrite(os);
      
      try {
        out.writeStream(is);
      } finally {
        out.close();
      }
    }
  }
  
  static {
    _marshalMap.put(void.class, new VoidMarshal());
    _marshalMap.put(String.class, new StringMarshal());
    _marshalMap.put(Integer.class, new IntegerMarshal());
    _marshalMap.put(Long.class, new LongMarshal());
    _marshalMap.put(InputStream.class, new InputStreamMarshal());
    
    introspectManagementOperations();
  }
}
