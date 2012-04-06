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

import com.caucho.config.ConfigException;
import com.caucho.jmx.Jmx;
import com.caucho.jmx.MXAction;
import com.caucho.jmx.MXContentType;
import com.caucho.jmx.MXParam;
import com.caucho.json.JsonOutput;
import com.caucho.management.server.ManagementMXBean;
import com.caucho.management.server.StatServiceValue;
import com.caucho.server.admin.AddUserQueryReply;
import com.caucho.server.admin.JmxCallQueryReply;
import com.caucho.server.admin.JmxSetQueryReply;
import com.caucho.server.admin.JsonQueryReply;
import com.caucho.server.admin.ListJmxQueryReply;
import com.caucho.server.admin.ListUsersQueryReply;
import com.caucho.server.admin.PdfReportQueryReply;
import com.caucho.server.admin.RemoveUserQueryReply;
import com.caucho.server.admin.StatServiceValuesQueryReply;
import com.caucho.server.admin.StringQueryReply;
import com.caucho.server.admin.UserQueryReply;
import com.caucho.server.deploy.DeployControllerState;
import com.caucho.server.deploy.DeployTagResult;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class AdminRestServlet extends HttpServlet
{
  private static final L10N L = new L10N(AdminRestServlet.class);
  private static final Logger log
    = Logger.getLogger(AdminRestServlet.class.getName());

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
    else if (!req.isUserInRole("resin-admin")) {
      res.setStatus(HttpServletResponse.SC_FORBIDDEN);

      PrintWriter out = res.getWriter();
      out.println(L.l("admin requires a user in the resin-admin role"));
      return;
    }
    else if (_isRequireSecure && !req.isSecure()) {
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

    if (!action.getHttpMethod().equals(req.getMethod())) {
      res.setStatus(500);
      res.setContentType("text/plain");

      PrintWriter out = res.getWriter();

      out.println(L.l("http method {0} is expected", action.getHttpMethod()));

      return;
    }

    try {
      action.doAction(req, res);
    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);

      Throwable cause = e;
      while (cause.getCause() != null)
        cause = cause.getCause();

      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

      PrintWriter out = res.getWriter();

      if (cause instanceof ConfigException)
        out.println(cause.getMessage());
      else
        out.println(cause);
    }
  }

  static private void introspectManagementOperations()
  {
    Class<?> cl = ManagementMXBean.class;

    for (Method method : cl.getDeclaredMethods()) {
      MXAction mxAction = method.getAnnotation(MXAction.class);

      if (mxAction == null)
        continue;

      String name = mxAction.value();
      String httpMethod = mxAction.method();

      Action action = new Action(method, httpMethod);

      _actionMap.put(name, action);
    }
  }

  static class Action
  {
    private Method _method;

    private String _httpMethod;

    private String []_parameterNames;
    private Serializable []_parameterDefaults;
    private boolean []_parameterRequired;

    private Marshal []_parameterMarshal;
    private Marshal _returnMarshal;

    private boolean _isPutStream;
    private boolean _isGetStream;
    
    private String _contentType;

    Action(Method method, String httpMethod)
    {
      _method = method;
      _httpMethod = httpMethod;
      
      MXContentType contentType = method.getAnnotation(MXContentType.class);
      
      if (contentType != null)
        _contentType = contentType.value();

      Class<?> []parameterTypes = method.getParameterTypes();

      _parameterNames = new String[parameterTypes.length];
      _parameterDefaults = new Serializable[parameterTypes.length];
      _parameterRequired = new boolean[parameterTypes.length];

      Annotation [][]paramAnn = method.getParameterAnnotations();

      for (int i = 0; i < _parameterNames.length; i++) {
        MXParam mxParam = null;
        for (Annotation a : paramAnn[i]) {
          if (a.annotationType().equals(MXParam.class)) {
            mxParam = (MXParam) a;

            break;
          }
        }

        if (mxParam == null) {
          _parameterNames[i] = "p" + i;
          _parameterDefaults[i] = null;
          _parameterRequired[i] = false;
        }
        else {
          _parameterNames[i] = mxParam.name();
          _parameterRequired[i] = mxParam.required();

          String defaultValue = mxParam.defaultValue();
          defaultValue = MXParam.NULL.equals(defaultValue) ?
            null :
            defaultValue;

          _parameterDefaults[i]
            = toValue(parameterTypes[i], defaultValue);
        }
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

    String []getParameterNames()
    {
      return _parameterNames;
    }

    public String getHttpMethod()
    {
      return _httpMethod;
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

        if (_contentType != null)
          res.setContentType(_contentType);

        _returnMarshal.unmarshal(res, value);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    protected static Serializable toValue(Class type, String value)
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

    public void unmarshal(HttpServletResponse response, K value)
      throws IOException
    {
      JsonOutput out = new JsonOutput(response.getWriter());

      out.writeObject((Serializable) value);

      out.flush();
    }
  }

  static class BooleanMarshal extends Marshal<Boolean>
  {
    @Override
    public Boolean marshal(HttpServletRequest request,
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

  static class StringMarshal extends Marshal<String>
  {
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

  static class IntegerMarshal extends Marshal<Integer>
  {
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

  static class LongMarshal extends Marshal<Long>
  {
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

  static class VoidMarshal extends Marshal<Void>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          Void defaultValue)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }

  static class InputStreamMarshal extends Marshal<InputStream>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          InputStream defaultValue)
      throws IOException
    {
      return request.getInputStream();
    }

    @Override
    public void unmarshal(HttpServletResponse response, InputStream value)
      throws IOException
    {
      OutputStream os = response.getOutputStream();

      WriteStream out = Vfs.openWrite(os);

      try {
        out.writeStream(value);
      } finally {
        out.close();
      }
    }
  }

  static class StringQueryReplyMarshal
    extends Marshal<StringQueryReply>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          StringQueryReply defaultValue)
      throws IOException
    {
      throw new AbstractMethodError();
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          StringQueryReply value)
      throws IOException
    {
      JsonOutput out = new JsonOutput(response.getWriter());
      
      out.writeString(value.getValue());
      
      out.flush();
    }
  }

  static class JsonQueryReplyMarshal
    extends Marshal<JsonQueryReply>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          JsonQueryReply defaultValue)
      throws IOException
    {
      throw new AbstractMethodError();
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          JsonQueryReply value)
      throws IOException
    {
      PrintWriter out = response.getWriter();

      out.write(value.getValue());
    }
  }
  
  static class ControllerStateActionQueryReplyMarshal
    extends Marshal<DeployControllerState>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          DeployControllerState defaultValue)
      throws IOException
    {
      throw new AbstractMethodError();
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          DeployControllerState value)
      throws IOException
    {
      JsonOutput out = new JsonOutput(response.getWriter());

      out.writeObject(value, true);

      out.flush();
    }
  }

  static class AddUserQueryReplyMarshal
    extends Marshal<AddUserQueryReply>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          AddUserQueryReply defaultValue)
      throws IOException
    {
      throw new AbstractMethodError();
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          AddUserQueryReply value)
      throws IOException
    {
      JsonOutput out = new JsonOutput(response.getWriter());

      out.writeObject(value.getUser(), true);

      out.flush();
    }
  }

  static class RemoveUserQueryReplyMarshal
    extends Marshal<RemoveUserQueryReply>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          RemoveUserQueryReply defaultValue)
      throws IOException
    {
      throw new AbstractMethodError();
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          RemoveUserQueryReply value)
      throws IOException
    {
      JsonOutput out = new JsonOutput(response.getWriter());

      out.writeObject(value.getUser(), true);
      
      out.flush();
    }
  }

  static class ListUsersQueryResultMarshal
    extends Marshal<ListUsersQueryReply>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          ListUsersQueryReply defaultValue)
      throws IOException
    {
      throw new AbstractMethodError();
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          ListUsersQueryReply value)
      throws IOException
    {
      UserQueryReply.User []users = value.getUsers();

      JsonOutput out = new JsonOutput(response.getWriter());

      out.writeObject(users);

      out.flush();
    }
  }

  static class PdfReportQueryReplyMarshal
    extends Marshal<PdfReportQueryReply>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          PdfReportQueryReply defaultValue)
      throws IOException
    {
      throw new AbstractMethodError();
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          PdfReportQueryReply value)
      throws IOException
    {
      if (value.getPdf() != null) {
        response.setContentType("application/pdf");

        WriteStream out = Vfs.openWrite(response.getOutputStream());

        out.writeStream(value.getPdf().getInputStream());

        out.flush();
      }
      else {
        JsonOutput out = new JsonOutput(response.getWriter());

        out.writeObject(value.getFileName());
        
        out.flush();
      }
    }
  }

  static class TagReplyMarshal extends Marshal<DeployTagResult[]>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          DeployTagResult []defaultValue) throws IOException
    {
      throw new AbstractMethodError(getClass().getName());
    }

    @Override
    public void unmarshal(HttpServletResponse response, DeployTagResult []value)
      throws IOException
    {
      JsonOutput out = new JsonOutput(response.getWriter());

      out.writeObject(value, true);

      out.flush();
    }
  }

  static class StringArrayMarshal extends Marshal<String[]>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          String []value) throws IOException
    {
      throw new AbstractMethodError(getClass().getName());
    }
  }

  static class DateArrayMarshal extends Marshal<Date[]>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          Date []value) throws IOException
    {
      throw new AbstractMethodError(getClass().getName());
    }
  }

  static class ListJmxQueryReplyMarshal extends Marshal<ListJmxQueryReply>
  {
    @Override 
    public Object marshal(HttpServletRequest request,
                                    String name,
                                    ListJmxQueryReply defaultValue)
      throws IOException
    {
      throw new AbstractMethodError(getClass().getName());
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          ListJmxQueryReply value)
      throws IOException
    {
      PrintWriter writer = response.getWriter();
      
      JsonOutput out = new JsonOutput(response.getWriter());
      
      out.writeObject((Serializable)value.getBeans(), true);
      
      out.flush();
      
      writer.flush();
    }
  }

  static class JmxSetQueryReplyMarshal extends Marshal<JmxSetQueryReply>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          JmxSetQueryReply defaultValue)
      throws IOException
    {
      throw new AbstractMethodError(getClass().getName());
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          JmxSetQueryReply value)
      throws IOException
    {
      JsonOutput out = new JsonOutput(response.getWriter());

      out.writeObject(value, true);

      out.flush();
    }
  }

  static class JmxCallQueryReplyMarshal extends Marshal<JmxCallQueryReply>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          JmxCallQueryReply defaultValue)
      throws IOException
    {
      throw new AbstractMethodError(getClass().getName());
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          JmxCallQueryReply value)
      throws IOException
    {
      JsonOutput out = new JsonOutput(response.getWriter());

      out.writeObject(value, true);

      out.flush();
    }
  }

  static class StatServiceValuesQueryReplytMarshal
    extends Marshal<StatServiceValuesQueryReply>
  {
    @Override
    public Object marshal(HttpServletRequest request,
                          String name,
                          StatServiceValuesQueryReply defaultValue)
      throws IOException
    {
      return null;
    }

    @Override
    public void unmarshal(HttpServletResponse response,
                          StatServiceValuesQueryReply stats)
      throws IOException
    {
      PrintWriter out = response.getWriter();

      String []names = stats.getNames();
      StatServiceValue [][]data = stats.getData();

      out.println("[");
      for (int i = 0; i < names.length; i++) {
        String name = names[i];
        StatServiceValue []values = data[i];

        out.println("  {");
        out.print("    \"label\":");
        out.print('"' + name + '"');
        out.println(',');
        out.println("    \"data\": [");

        for (int j = 0; j < values.length; j++) {
          StatServiceValue value = values[j];
          out.print("              ["
                    + value.getTime()
                    + ","
                    + value.getValue()
                    + "]");
          if ((j + 1) < values.length)
            out.print(",");
          out.println();
        }
        out.println("            ]");
        out.print("  }");
        if ((i + 1) < names.length)
          out.print(",");
        out.println();
      }

      out.println("]");
    }
  }

  static {
    _marshalMap.put(void.class, new VoidMarshal());
    _marshalMap.put(String.class, new StringMarshal());
    _marshalMap.put(Integer.class, new IntegerMarshal());
    _marshalMap.put(Long.class, new LongMarshal());
    _marshalMap.put(boolean.class, new BooleanMarshal());
    _marshalMap.put(InputStream.class, new InputStreamMarshal());
    _marshalMap.put(String[].class, new StringArrayMarshal());
    _marshalMap.put(InputStream.class, new InputStreamMarshal());
    _marshalMap.put(Date[].class,
                    new DateArrayMarshal());

    _marshalMap.put(StringQueryReply.class, new StringQueryReplyMarshal());
    _marshalMap.put(JsonQueryReply.class, new JsonQueryReplyMarshal());
    _marshalMap.put(AddUserQueryReply.class, new AddUserQueryReplyMarshal());
    _marshalMap.put(DeployTagResult[].class, new TagReplyMarshal());

    _marshalMap.put(StatServiceValuesQueryReply.class,
                    new StatServiceValuesQueryReplytMarshal());

    _marshalMap.put(RemoveUserQueryReply.class,
                    new RemoveUserQueryReplyMarshal());

    _marshalMap.put(ListUsersQueryReply.class,
                    new ListUsersQueryResultMarshal());

    _marshalMap.put(ListJmxQueryReply.class,
                    new ListJmxQueryReplyMarshal());

    _marshalMap.put(JmxSetQueryReply.class,
                    new JmxSetQueryReplyMarshal());

    _marshalMap.put(JmxCallQueryReply.class,
                    new JmxCallQueryReplyMarshal());

    _marshalMap.put(DeployControllerState.class,
                    new ControllerStateActionQueryReplyMarshal());

    _marshalMap.put(PdfReportQueryReply.class,
                    new PdfReportQueryReplyMarshal());

    introspectManagementOperations();
  }
}
