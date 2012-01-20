  /*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
import com.caucho.jmx.MXAction;
import com.caucho.jmx.MXDefaultValue;
import com.caucho.jmx.MXName;
import com.caucho.jmx.MXValueRequired;
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

    String pathInfo = req.getPathInfo();

    if (pathInfo == null || "/".equals(pathInfo)) {
      res.setStatus(500);
      res.setContentType("text/plain");

      PrintWriter out = res.getWriter();
      out.println(L.l("action is required"));

      return;
    }

    final String actionName = pathInfo.substring(1);
    
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
      MXAction mxAction = method.getAnnotation(MXAction.class);

      if (mxAction == null)
        continue;

      String name = mxAction.value();

      Action action = new Action(method);
      
      _actionMap.put(name, action);
    }
  }
  
  static class Action {
    private Method _method;

    private String []_parameterNames;
    private Object [] _parameterDefaults;
    private boolean [] _parameterRequired;

    private Marshal []_parameterMarshal;
    private Marshal _returnMarshal;
    
    private boolean _isPutStream;
    private boolean _isGetStream;
    
    Action(Method method)
    {
      _method = method;
      
      Class<?> []parameterTypes = method.getParameterTypes();
      
      _parameterNames = new String[parameterTypes.length];
      _parameterDefaults = new Object[parameterTypes.length];
      _parameterRequired = new boolean[parameterTypes.length];

      Annotation [][]paramAnn = method.getParameterAnnotations();
      
      for (int i = 0; i < _parameterNames.length; i++) {
        _parameterNames[i] = getParamName(paramAnn, i);
        _parameterDefaults[i] = getParameterDefault(paramAnn, i, parameterTypes[i]);
        _parameterRequired[i] = isParameterRequired(paramAnn, i);
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

    private Object getParameterDefault(Annotation [][]paramAnn,
                                       int i,
                                       Class type)
    {
      for (Annotation ann : paramAnn[i]) {
        if (ann.annotationType().equals(MXDefaultValue.class)){
          MXDefaultValue value = (MXDefaultValue) ann;

          return toValue(type, value.value());
        }
      }

      return null;
    }

    private boolean isParameterRequired(Annotation [][]paramAnn,
                                       int i)
    {
      for (Annotation ann : paramAnn[i]) {
        if (ann.annotationType().equals(MXValueRequired.class))
          return true;
      }

      return false;
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

      Object []params = new Object[_parameterMarshal.length];
      
      for (int i = 0; i < params.length; i++) {
        String requestParam = req.getParameter(_parameterNames[i]);

        if (requestParam == null && _parameterRequired[i]) {
          res.setStatus(500);
          res.setContentType("text/plain");

          PrintWriter out = res.getWriter();

          out.println(L.l("parameter `{0}' is requried.",
                                                 _parameterNames[i]));

          return;
        }

        params[i] = _parameterMarshal[i].marshal(req,
                                                _parameterNames[i],
                                                _parameterDefaults[i]);
      }
      
      try {
        Object value = _method.invoke(management, params);
        
        _returnMarshal.unmarshal(res, value);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    protected static Object toValue(Class type, String value)
    {
      if (boolean.class.equals(type) || Boolean.class.equals(type)) {
        return Boolean.parseBoolean(value);
      }
      else if (byte.class.equals(type) || Byte.class.equals(type)) {
        return Byte.parseByte(value);
      }
      else if (short.class.equals(type) || Short.class.equals(type)) {
        return Short.parseShort(value);
      }
      else if (char.class.equals(type) || Character.class.equals(type)) {
        return new Character((char) Integer.parseInt(value));
      }
      else if (int.class.equals(type) || Integer.class.equals(type)) {
        return Integer.parseInt(value);
      }
      else if (long.class.equals(type) || Long.class.equals(type)) {
        return Long.parseLong(value);
      }
      else if (float.class.equals(type) || Float.class.equals(type)) {
        return Float.parseFloat(value);
      }
      else if (double.class.equals(type) || Double.class.equals(type)) {
        return Double.parseDouble(value);
      }
      else if (type.isEnum()) {
        return Enum.valueOf(type, value);
      }
      else {
        return value;
      }
    }
  }

  abstract static class Marshal<K>
  {
    abstract public Object marshal(HttpServletRequest request,
                                   String name,
                                   K defaultValue)
      throws IOException;

    public void unmarshal(HttpServletResponse response, Object value)
      throws IOException
    {
      PrintWriter out = response.getWriter();

      out.println(value);
    }
  }

  static class BooleanMarshal extends Marshal<Boolean>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          Boolean defaultValue)
    {
      String param = request.getParameter(name);

      if (param != null)
        return Boolean.parseBoolean(param);
      else if (defaultValue != null)
        return defaultValue;
      else
        return Boolean.FALSE;
    }
  }

  static class StringMarshal extends Marshal<String> {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          String defaultValue)
    {
      String param = request.getParameter(name);

      if (param != null)
        return param;
      else
        return defaultValue;
    }
  }
  
  static class IntegerMarshal extends Marshal<Integer> {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          Integer defaultValue)
    {
      String param = request.getParameter(name);
      
      if (param != null)
        return Integer.parseInt(param);
      else if (defaultValue != null)
        return defaultValue;
      else
        return 0;
    }
  }
  
  static class LongMarshal extends Marshal<Long> {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          Long defaultValue)
    {
      String param = request.getParameter(name);

      if (param != null)
        return Long.parseLong(param);
      else if (defaultValue != null)
        return defaultValue;
      else
        return 0;
    }
  }
  
  static class VoidMarshal extends Marshal<Void> {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          Void defaultValue)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
  
  static class InputStreamMarshal extends Marshal<InputStream> {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          InputStream defaultValue)
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
    _marshalMap.put(boolean.class, new BooleanMarshal());

    introspectManagementOperations();
  }
}
