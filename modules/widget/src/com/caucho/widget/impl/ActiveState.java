/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.widget.impl;

import com.caucho.widget.*;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.XmlWriter;

import javax.servlet.http.Cookie;
import java.io.*;
import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;

public class ActiveState
{
  private static final L10N L = new L10N(ActiveState.class);
  private static final Logger log = Log.open(ActiveState.class);

  protected static final String NAMESPACE_SEPARATOR = "-";
  protected static final String NAMED_PARAMETER_SEPARATOR = ".";

  // inner classes implement interfaces and delegate to the outer class
  private final WidgetInvocationImpl _widgetInvocation = new WidgetInvocationImpl();
  private final WidgetRequestImpl _widgetRequest = new WidgetRequestImpl();
  private final WidgetResponseImpl _widgetResponse = new WidgetResponseImpl();

  private InitState _initState;
  private ActiveStateWalker _activeStateWalker;

  private VarHolder _varHolder = new VarHolder();

  private PrefixMap _namedParameterMap;
  private boolean _isInit;

  protected Widget _widget;
  protected ActiveState _parentState;
  protected XmlWriter _writer;
  protected OutputStream _outputStream;
  protected PrefixMap _namespaceParameterMap;
  protected PrefixMap _localParameterMap;

  public void setInitState(InitState initState)
  {
    _initState = initState;
  }

  protected InitState getInitState()
  {
    return _initState;
  }

  public void setActiveStateWalker(ActiveStateWalker activeStateWalker)
  {
    _activeStateWalker = activeStateWalker;
  }

  protected ActiveStateWalker getActiveStateWalker()
  {
    return _activeStateWalker;
  }

  public void init()
  {
    if (_isInit)
      throw new IllegalStateException("already init");

    assert _namedParameterMap == null || !_namedParameterMap.isInit();

    assert _namespaceParameterMap == null || !_namespaceParameterMap.isInit();
    assert _localParameterMap == null || !_localParameterMap.isInit();
    assert _namedParameterMap == null || !_namedParameterMap.isInit();

    assert _writer == null;
    assert _outputStream == null;

    if (_activeStateWalker == null)
      throw new IllegalStateException(L.l("`{0}' is required", "state-walker"));

    if (_initState == null)
      throw new IllegalStateException(L.l("`{0}' is required", "init-state"));

    _varHolder.setWidgetInit(_initState);

    ActiveState parent = getParentState();

    if (parent != null)
      _varHolder.setParent(parent.getVarHolder());

    _varHolder.setSibling(_initState.getVarHolder());

    _isInit = true;
  }

  public void destroy()
  {
    _activeStateWalker = null;
    _initState = null;

    _writer = null;
    _outputStream = null;

    _parentState = null;

    PrefixMap namespaceParameterMap = _namespaceParameterMap;
    PrefixMap localParameterMap = _localParameterMap;
    PrefixMap namedParameterMap = _namedParameterMap;

    _namespaceParameterMap = null;
    _localParameterMap = null;
    _namedParameterMap = null;

    if (namespaceParameterMap != null) {
      namespaceParameterMap.destroy();
      _namespaceParameterMap = namespaceParameterMap; // recycle it
    }

    if (localParameterMap != null) {
      localParameterMap.destroy();
      _localParameterMap = localParameterMap; // recycle it
    }

    if (namedParameterMap != null) {
      namedParameterMap.destroy();
      _namedParameterMap = namedParameterMap; // recycle it
    }

    _varHolder.destroy();

    _isInit = false;
  }

  protected String getId()
  {
    return _widget.getId();
  }

  public String getLocalParameterMapPrefix()
  {
    getLocalParameterMap();

    return _localParameterMap.getPrefix();
  }

  public String getNamedParameterMapPrefix()
  {
    getNamedParameterMap();

    return _namedParameterMap.getPrefix();
  }

  public String getNamespaceParameterMapPrefix()
  {
    getNamespaceParameterMap();

    return _namespaceParameterMap.getPrefix();
  }

  protected ActiveRootState getWidgetRootState()
  {
    return getParentState().getWidgetRootState();
  }

  protected WidgetInvocation getWidgetInvocation()
  {
    return _widgetInvocation;
  }

  protected WidgetRequest getWidgetRequest()
  {
    return _widgetRequest;
  }

  protected WidgetResponse getWidgetResponse()
  {
    return _widgetResponse;
  }

  public WidgetInitChain getInitChain()
  {
    return _initState.getInitChain();
  }

  private WidgetInvocationChain getInvocationChain()
  {
    return _activeStateWalker;
  }

  private WidgetRequestChain getRequestChain()
  {
    return _activeStateWalker;
  }

  private WidgetResponseChain getResponseChain()
  {
    return _activeStateWalker;
  }

  public void addVar(VarDefinition varDefinition)
  {
    throw new UnsupportedOperationException();
  }

  private VarHolder getVarHolder(Widget widget)
  {
    ActiveState activeState = _activeStateWalker.getState(widget);

    if (activeState == null)
      throw new IllegalArgumentException(L.l("unknown widget `{0}'", widget));

    return activeState.getVarHolder();
  }

  VarHolder getVarHolder()
  {
    return _varHolder;
  }

  public VarDefinition getVarDefinition(String varName)
  {
    return getInitState().getVarDefinition(varName);
  }

  public void setVar(Widget widget, String name, Object value)
    throws UnsupportedOperationException
  {
    getVarHolder(widget).setVar(name, value);
  }

  public void setVar(Widget widget, VarDefinition varDefinition, Object value)
    throws UnsupportedOperationException
  {
    getVarHolder(widget).setVar(varDefinition, value);
  }

  public <T> T getVar(Widget widget, String name)
  {
    return (T) getVarHolder(widget).getVar(name);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition)
  {
    return (T) getVar(widget, varDefinition.getName());
  }

  public <T> T getVar(Widget widget, String name, T deflt)
  {
    return (T) getVarHolder(widget).getVar(name, deflt);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition, T deflt)
  {
    return (T) getVarHolder(widget).getVar(varDefinition.getName(), deflt);
  }

  public void removeVar(Widget widget, String name)
  {
    getVarHolder(widget).removeVar(name);
  }

  public void removeVar(Widget widget, VarDefinition varDefinition)
  {
    getVarHolder(widget).removeVar(varDefinition);
  }

  public <T> T find(String name)
  {
    return (T) getInitState().find(name);
  }

  public void setApplicationAttribute(String name, Object value)
  {
    getInitState().setApplicationAttribute(name, value);
  }

  public <T> T getApplicationAttribute(String name)
  {
    return (T) getInitState().getApplicationAttribute(name);
  }

  public Enumeration<String> getApplicationAttributeNames()
  {
    return getInitState().getApplicationAttributeNames();
  }

  public void removeApplicationAttribute(String name)
  {
    getInitState().removeApplicationAttribute(name);
  }

  public WidgetURL createURL()
    throws WidgetException
  {
    return createURL(false);
  }

  public WidgetURL createSubmitURL()
    throws WidgetException
  {
    return getWidgetRootState().createURL(this, ActiveRootState.UrlType.SUBMIT);
  }

  public WidgetURL createParameterURL()
    throws WidgetException
  {
    return getWidgetRootState().createURL(this, ActiveRootState.UrlType.PARAMETER);
  }

  protected WidgetURL createURL(boolean isSubmit)
    throws WidgetException
  {
    return getWidgetRootState().createURL(this, ActiveRootState.UrlType.NORMAL);
  }

  public WidgetURL createTargetURL(Widget widget)
    throws WidgetException
  {
    return getWidgetRootState().createURL(this, ActiveRootState.UrlType.TARGET);
  }

  protected ActiveState getParentState()
  {
    return _parentState;
  }

  protected Widget getWidget()
  {
    return _widget;
  }

  public String getParameter()
  {
    String[] values = getParameterValues();

    if (values != null && values.length > 0)
      return values[0];
    else
      return null;
  }

  public String[] getParameterValues()
  {
    return getLocalParameterMap().get("");
  }

  public Map<String, String[]> getNamespaceParameterMap()
  {
    if (_namespaceParameterMap == null)
      _namespaceParameterMap = new PrefixMap();

    if (!_namespaceParameterMap.isInit()) {
      Map<String, String[]> parentMap =  getParentState().getNamespaceParameterMap();

      _namespaceParameterMap.setBackingMap(parentMap);

      if (isNamespace())
        _namespaceParameterMap.setPrefix(getId() + NAMESPACE_SEPARATOR);

      _namespaceParameterMap.init();
    }

    return _namespaceParameterMap;
  }

  public Map<String, String[]> getNamedParameterMap()
  {
    if (_namedParameterMap == null)
      _namedParameterMap = new PrefixMap();

    if (!_namedParameterMap.isInit()) {
      _namedParameterMap.setBackingMap(getLocalParameterMap());

      _namedParameterMap.setPrefix(NAMED_PARAMETER_SEPARATOR);

      _namedParameterMap.init();
    }

    return _namedParameterMap;
  }

  public String getParameter(String name)
  {
    String[] values = getParameterValues(name);

    if (values != null && values.length > 0)
      return values[0];
    else
      return null;
  }

  public String[] getParameterValues(String name)
  {
    Map<String, String[]> map = getNamedParameterMap();

    if (map != null)
      return map.get(name);
    else
      return null;
  }

  public Enumeration<String> getParameterNames()
  {
    Map<String, String[]> map = getNamedParameterMap();

    if (map == null)
      map = Collections.emptyMap();

    return Collections.enumeration(map.keySet());
  }

  public String getPathInfo()
  {
    return getParentState().getPathInfo();
  }

  public Cookie[] getCookies()
  {
    return getParentState().getCookies();
  }

  public String getRequestedSessionId()
  {
    return getParentState().getRequestedSessionId();
  }

  public boolean isRequestedSessionIdValid()
  {
    return getParentState().isRequestedSessionIdValid();
  }

  public boolean isRequestedSessionIdFromCookie()
  {
    return getParentState().isRequestedSessionIdFromCookie();
  }

  public boolean isRequestedSessionIdFromURL()
  {
    return getParentState().isRequestedSessionIdFromURL();
  }

  public String getAuthType()
  {
    return getParentState().getAuthType();
  }

  public String getRemoteUser()
  {
    return getParentState().getRemoteUser();
  }

  public boolean isUserInRole(String role)
  {
    return getParentState().isUserInRole(role);
  }

  public Principal getUserPrincipal()
  {
    return getParentState().getUserPrincipal();
  }

  public String getProtocol()
  {
    return getParentState().getProtocol();
  }

  public String getScheme()
  {
    return getParentState().getScheme();
  }

  public String getServerName()
  {
    return getParentState().getServerName();
  }

  public int getServerPort()
  {
    return getParentState().getServerPort();
  }

  public String getRemoteAddr()
  {
    return getParentState().getRemoteAddr();
  }

  public String getRemoteHost()
  {
    return getParentState().getRemoteHost();
  }

  public int getRemotePort()
  {
    return getParentState().getRemotePort();
  }

  public String getLocalAddr()
  {
    return getParentState().getLocalAddr();
  }

  public String getLocalName()
  {
    return getParentState().getLocalName();
  }

  public int getLocalPort()
  {
    return getParentState().getLocalPort();
  }

  public void setRequestCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    getParentState().setRequestCharacterEncoding(encoding);
  }

  public String getRequestCharacterEncoding()
  {
    return getParentState().getRequestCharacterEncoding();
  }

  public InputStream getInputStream()
    throws IOException
  {
    return getParentState().getInputStream();
  }

  public int getRequestContentLength()
  {
    return getParentState().getRequestContentLength();
  }

  public String getRequestContentType()
  {
    return getParentState().getRequestContentType();
  }

  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return getParentState().getReader();
  }

  public Locale getRequestLocale()
  {
    return getParentState().getRequestLocale();
  }

  public Enumeration<Locale> getRequestLocales()
  {
    return getParentState().getRequestLocales();
  }

  public boolean isSecure()
  {
    return getParentState().isSecure();
  }

  public <T> T getRequestAttribute(String name)
  {
    return (T) getParentState().getRequestAttribute(name);
  }

  public void setRequestAttribute(String name, Object value)
  {
    getParentState().setRequestAttribute(name, value);
  }

  public Enumeration<String> getRequestAttributeNames()
  {
    return getParentState().getRequestAttributeNames();
  }

  public void removeRequestAttribute(String name)
  {
    getParentState().removeRequestAttribute(name);
  }

  public <T> T getSessionAttribute(String name)
  {
    return (T) getParentState().getSessionAttribute(name);
  }

  public void setSessionAttribute(String name, Object value)
  {
    getParentState().setSessionAttribute(name, value);
  }

  public Enumeration<String> getSessionAttributeNames()
  {
    return getParentState().getSessionAttributeNames();
  }

  public void removeSessionAttribute(String name)
  {
    getParentState().removeSessionAttribute(name);
  }

  public void addCookie(Cookie cookie)
  {
    getParentState().addCookie(cookie);
  }

  public XmlWriter getWriter()
    throws IOException
  {
    if (_writer == null)
      _writer = getParentState().getWriter();

    return _writer;
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    if (_outputStream == null)
      _outputStream = getParentState().getOutputStream();

    return _outputStream;
  }

  public void setResponseContentType(String type)
  {
    getParentState().setResponseContentType(type);
  }

  public String getResponseContentType()
  {
    return getParentState().getResponseContentType();
  }

  public String getResponseCharacterEncoding()
  {
    return getParentState().getResponseCharacterEncoding();
  }

  public void setResponseCharacterEncoding(String charset)
  {
    getParentState().setResponseCharacterEncoding(charset);
  }

  public void setResponseLocale(Locale locale)
  {
    getParentState().setResponseLocale(locale);
  }

  public Locale getResponseLocale()
  {

    return getParentState().getResponseLocale();
  }

  public String createResourceURL(String path)
  {
    return getParentState().createResourceURL(path);
  }

  public void finishInvocation()
  {
  }

  public void finishRequest()
  {
  }

  public void finishResponse()
  {
    if (_writer != null)
      _writer.flush();


    if (_outputStream != null) {
      try {
        _outputStream.flush();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public String toString()
  {
    return "ActiveState[widget=" + getWidget().getTypeName() + " id=" + getWidget().getId() + "]";
  }

  public ActiveState getState()
  {
    return this;
  }

  public void setWidget(Widget widget)
  {
    _widget = widget;
  }

  protected boolean isNamespace()
  {
    return _widget.isNamespace();
  }

  public void setParentState(ActiveState parentState)
  {
    _parentState = parentState;
  }

  public PrefixMap getLocalParameterMap()
  {
    String id = getId();

    if (id == null)
      throw new UnsupportedOperationException(L.l("`{0}' is required for {1}", "id", toString()));

    if (_localParameterMap == null)
      _localParameterMap = new PrefixMap();

    if (!_localParameterMap.isInit()) {
      _localParameterMap.setBackingMap(getParentState().getNamespaceParameterMap());

      _localParameterMap.setPrefix(id);

      _localParameterMap.init();
    }

    return _localParameterMap;
  }

  // implement interface and delegate all calls to outer class

  abstract private class AbstractImpl
    implements VarContext
  {
    public void addVarDefinition(VarDefinition varDefinition)
    {
      getState().addVar(varDefinition);
    }

    public VarDefinition getVarDefinition(String varName)
    {
      return getState().getVarDefinition(varName);
    }

    public void setVar(Widget widget, String name, Object value)
      throws UnsupportedOperationException
    {
      getState().setVar(widget, name, value);
    }

    public void setVar(Widget widget, VarDefinition varDefinition, Object value)
      throws UnsupportedOperationException
    {
      getState().setVar(widget, varDefinition, value);
    }

    public <T> T getVar(Widget widget, String name)
    {
      return (T) getState().getVar(widget, name);
    }

    public <T> T getVar(Widget widget, VarDefinition varDefinition)
    {
      return (T) getState().getVar(widget, varDefinition);
    }

    public <T> T getVar(Widget widget, String name, T deflt)
    {
      return (T) getState().getVar(widget, name, deflt);
    }

    public <T> T getVar(Widget widget, VarDefinition varDefinition, T deflt)
    {
      return (T) getState().getVar(widget, varDefinition, deflt);
    }

    public void removeVar(Widget widget, String name)
    {
      getState().removeVar(widget, name);
    }

    public void removeVar(Widget widget, VarDefinition varDefinition)
    {
      getState().removeVar(widget, varDefinition);
    }

    public <T> T find(String name)
    {
      return (T) getState().find(name);
    }

    public void setApplicationAttribute(String name, Object value)
    {
      getState().setApplicationAttribute(name, value);
    }

    public <T> T getApplicationAttribute(String name)
    {
      return (T) getState().getApplicationAttribute(name);
    }

    public Enumeration<String> getApplicationAttributeNames()
    {
      return getState().getApplicationAttributeNames();
    }

    public void removeApplicationAttribute(String name)
    {
      getState().removeApplicationAttribute(name);
    }

  }

  // implement interface and delegate all calls to outer class

  private class WidgetInvocationImpl
    extends AbstractImpl
    implements WidgetInvocation
  {
    public String getParameter()
    {
      return getState().getParameter();
    }

    public String[] getParameterValues()
    {
      return getState().getParameterValues();
    }

    public String getParameter(String name)
    {
      return getState().getParameter(name);
    }

    public String[] getParameterValues(String name)
    {
      return getState().getParameterValues(name);
    }

    public Enumeration<String> getParameterNames()
    {
      return getState().getParameterNames();
    }

    public Map<String, String[]> getParameterMap()
    {
      return getState().getNamedParameterMap();
    }

    public String getPathInfo()
    {
      return getState().getPathInfo();
    }

    public Cookie[] getCookies()
    {
      return getState().getCookies();
    }

    public String getRequestedSessionId()
    {
      return getState().getRequestedSessionId();
    }

    public boolean isRequestedSessionIdValid()
    {
      return getState().isRequestedSessionIdValid();
    }

    public boolean isRequestedSessionIdFromCookie()
    {
      return getState().isRequestedSessionIdFromCookie();
    }

    public boolean isRequestedSessionIdFromURL()
    {
      return getState().isRequestedSessionIdFromURL();
    }

    public boolean isSecure()
    {
      return getState().isSecure();
    }

    public String getAuthType()
    {
      return getState().getAuthType();
    }

    public String getRemoteUser()
    {
      return getState().getRemoteUser();
    }

    public boolean isUserInRole(String role)
    {
      return getState().isUserInRole(role);
    }

    public Principal getUserPrincipal()
    {
      return getState().getUserPrincipal();
    }

    public String getProtocol()
    {
      return getState().getProtocol();
    }

    public String getScheme()
    {
      return getState().getScheme();
    }

    public String getServerName()
    {
      return getState().getServerName();
    }

    public int getServerPort()
    {
      return getState().getServerPort();
    }

    public String getRemoteAddr()
    {
      return getState().getRemoteAddr();
    }

    public String getRemoteHost()
    {
      return getState().getRemoteHost();
    }

    public int getRemotePort()
    {
      return getState().getRemotePort();
    }

    public String getLocalAddr()
    {
      return getState().getLocalAddr();
    }

    public String getLocalName()
    {
      return getState().getLocalName();
    }

    public int getLocalPort()
    {
      return getState().getLocalPort();
    }

    public Locale getLocale()
    {
      return getState().getRequestLocale();
    }

    public Enumeration<Locale> getLocales()
    {
      return getState().getRequestLocales();
    }

    public WidgetInvocationChain getInvocationChain()
    {
      return getState().getInvocationChain();
    }

    public void finish()
    {
      getState().finishInvocation();
    }
  }

  // implement interface and delegate all calls to outer class

  private class WidgetRequestImpl
    extends AbstractImpl
    implements WidgetRequest
  {
    public void setCharacterEncoding(String encoding)
      throws UnsupportedEncodingException
    {
      getState().setRequestCharacterEncoding(encoding);
    }

    public String getCharacterEncoding()
    {
      return getState().getRequestCharacterEncoding();
    }

    public int getContentLength()
    {
      return getState().getRequestContentLength();
    }

    public String getContentType()
    {
      return getState().getRequestContentType();
    }

    public InputStream getInputStream()
      throws IOException
    {
      return getState().getInputStream();
    }

    public BufferedReader getReader()
      throws IOException, IllegalStateException
    {
      return getState().getReader();
    }

    public <T> T getRequestAttribute(String name)
    {
      return (T) getState().getRequestAttribute(name);
    }

    public void setRequestAttribute(String name, Object value)
    {
      getState().setRequestAttribute(name, value);
    }

    public Enumeration<String> getRequestAttributeNames()
    {
      return getState().getRequestAttributeNames();
    }

    public void removeRequestAttribute(String name)
    {
      getState().removeRequestAttribute(name);
    }

    public <T> T getSessionAttribute(String name)
    {
      return (T) getState().getSessionAttribute(name);
    }

    public void setSessionAttribute(String name, Object value)
    {
      getState().setSessionAttribute(name, value);
    }

    public Enumeration<String> getSessionAttributeNames()
    {
      return getState().getSessionAttributeNames();
    }

    public void removeSessionAttribute(String name)
    {
      getState().removeSessionAttribute(name);
    }

    public WidgetRequestChain getRequestChain()
    {
      return getState().getRequestChain();
    }

    public void finish()
    {
      getState().finishRequest();
    }
  }

  // implement interface and delegate all calls to outer class

  private class WidgetResponseImpl
    extends AbstractImpl
    implements WidgetResponse
  {
    public WidgetURL getUrl()
      throws WidgetException
    {
      return getState().createURL();
    }

    public WidgetURL getSubmitUrl()
      throws WidgetException
    {
      return getState().createSubmitURL();
    }

    public WidgetURL getParameterUrl()
      throws WidgetException
    {
      return getState().createParameterURL();
    }

    public String getResourceUrl(String path)
    {
      return getState().createResourceURL(path);
    }

    public WidgetURL createTargetURL(Widget widget)
      throws WidgetException
    {
      return getState().createTargetURL(widget);
    }

    public void addCookie(Cookie cookie)
    {
      getState().addCookie(cookie);
    }

    public void setContentType(String type)
    {
      getState().setResponseContentType(type);
    }

    public String getContentType()
    {
      return getState().getResponseContentType();
    }

    public String getCharacterEncoding()
    {
      return getState().getResponseCharacterEncoding();
    }

    public void setCharacterEncoding(String charset)
    {
      getState().setResponseCharacterEncoding(charset);
    }

    public void setLocale(Locale locale)
    {
      getState().setResponseLocale(locale);
    }

    public Locale getLocale()
    {
      return getState().getResponseLocale();
    }

    public OutputStream getOutputStream()
      throws IOException
    {
      return getState().getOutputStream();
    }

    public XmlWriter getWriter()
      throws IOException
    {
      return getState().getWriter();
    }

    public <T> T getRequestAttribute(String name)
    {
      return (T) getState().getRequestAttribute(name);
    }

    public void setRequestAttribute(String name, Object value)
    {
      getState().setRequestAttribute(name, value);
    }

    public Enumeration<String> getRequestAttributeNames()
    {
      return getState().getRequestAttributeNames();
    }

    public void removeRequestAttribute(String name)
    {
      getState().removeRequestAttribute(name);
    }

    public <T> T getSessionAttribute(String name)
    {
      return (T) getState().getSessionAttribute(name);
    }

    public void setSessionAttribute(String name, Object value)
    {
      getState().setSessionAttribute(name, value);
    }

    public Enumeration<String> getSessionAttributeNames()
    {
      return getState().getSessionAttributeNames();
    }

    public void removeSessionAttribute(String name)
    {
      getState().removeSessionAttribute(name);
    }

    public WidgetResponseChain getResponseChain()
    {
      return getState().getResponseChain();
    }

    public void finish()
    {
      getState().finishResponse();
    }
  }
}
