/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;


/**
 */
public class MapBasedInvocationFactory
  implements InvocationFactory, Cloneable
{
  static protected final Logger log = 
    Logger.getLogger(MapBasedInvocationFactory.class.getName());

  private String _reservedNamespace = "__";
  private String _actionTargetParameterName = "A";
  private String _windowStateParameterName = "W";
  private String _portletModeParameterName = "M";


  private LinkedHashMap<String, MapBasedInvocation> _invocationMap
    = new LinkedHashMap<String, MapBasedInvocation>();

  private String _actionNamespace;

  private Set<WindowState> _windowStatesUsed;
  private Set<PortletMode> _portletModesUsed;

  // this class is not required to be thread-safe
  private StringBuffer _buffer = new StringBuffer(256);

  /**
   * The reserved namespace is used to mark parameters that have special
   * meaning to the portal.  The specification suggests "javax.portal.", which
   * is rather long so the default is "__".  
   * 
   * This implementation also uses the reserved namespace as a prefix to
   * parameter names.
   */
  public void setReservedNamespace(String namespace)
  {
    _reservedNamespace = namespace;
  }

  /**
   * The reserved namespace is used to mark parameters that have special
   * meaning to the portal.
   */
  public String getReservedNamespace()
  {
    return _reservedNamespace;
  }

  /**
   * The name of the parameter to use to store the namespace of
   * the target of an action.
   */
  public void setActionTargetParameterName(String name)
  {
    _actionTargetParameterName = name;
  }

  /**
   * The name of the parameter to use to store the namespace of
   * the target of an action.
   */
  public String getActionTargetParameterName()
  {
    return _actionTargetParameterName;
  }

  /**
   * The name of the parameter to use to store the window state.
   */
  public void setWindowStateParameterName(String name)
  {
    _windowStateParameterName = name;
  }

  /**
   * The name of the parameter to use to store the window state.
   */
  public String getWindowStateParameterName()
  {
    return _windowStateParameterName;
  }

  /**
   * The name of the parameter to use to store the portlet mode.
   */
  public void setPortletModeParameterName(String name)
  {
    _portletModeParameterName = name;
  }

  /**
   * The name of the parameter to use to store the portlet mode.
   */
  public String getPortletModeParameterName()
  {
    return _portletModeParameterName;
  }

  public void start(Map<String, String[]> rawParameters)
  {
    if (rawParameters != null)
      decodeRawParameters(rawParameters);
  }

  public void finish()
  {
    _windowStatesUsed = null;
    _portletModesUsed = null;

    _actionNamespace = null;
    _invocationMap.clear();
  }

  public boolean isActionTarget(String namespace)
  {
    return namespace == _actionNamespace;
  }

  /**
   * The actionNamespace and actionMap are null in the returned clone
   * The invocation that matches the passed namespace will not have any
   * parameters. 
   */
  protected MapBasedInvocationFactory clone(String namespace)
  {
    try {
      MapBasedInvocationFactory clone 
        = (MapBasedInvocationFactory) super.clone();

      clone._windowStatesUsed = null;
      clone._portletModesUsed = null;
      clone._actionNamespace = null;

      clone._invocationMap = new LinkedHashMap<String, MapBasedInvocation>();

      Iterator<Map.Entry<String, MapBasedInvocation>> iter 
        = _invocationMap.entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry<String, MapBasedInvocation> entry = iter.next();
        String invocationNamespace = entry.getKey();
        MapBasedInvocation invocation = entry.getValue();

        boolean keepParameters = !namespace.equals(invocationNamespace);

        clone._invocationMap.put( invocationNamespace, 
                                  invocation.clone(keepParameters) );
      }

      return clone;
    }
    catch (CloneNotSupportedException ex) {
      throw new RuntimeException(ex);
    }
  }

  protected void decodeRawParameters(Map<String, String[]> raw)
  {
    _buffer.setLength(0);
    StringBuffer buf = _buffer;

    buf.append(_reservedNamespace);
    int len = buf.length();

    buf.append(_reservedNamespace);
    buf.append(_actionTargetParameterName);
    String actionTargetParameterName = buf.toString();

    buf.setLength(len);
    buf.append(_windowStateParameterName);
    String windowStateParameterName = buf.toString();

    buf.setLength(len);
    buf.append(_portletModeParameterName);
    String portletModeParameterName = buf.toString();

    MapBasedInvocation invocation = null;

    // first, determine the target of the action, if any.
    String[] action =  raw.get(actionTargetParameterName); 
    _actionNamespace = action == null || action.length == 0 ? null : action[0];

    if (_actionNamespace != null)
      getInvocation(_actionNamespace);

    // iterate the parameters.
    // parameters that begin with the _reservedNamespace are render
    // parameters.
    // parameters that do not begin with the _reservedNamespace are action
    // parameters, and belong to the Invocation for _actionNamespace.

    Iterator<Map.Entry<String, String[]>> iter = raw.entrySet().iterator();

    while (iter.hasNext()) {
      Map.Entry<String, String[]> entry = iter.next();
      String key = entry.getKey();
      String[] values = entry.getValue();


      String namespace = null;

      if (key.startsWith(_reservedNamespace)) {

        int keyLen = key.length();

        int st = _reservedNamespace.length();
        int nd = key.indexOf('.', st);
        if (nd > -1) {
          namespace = key.substring(st, nd);
          nd++;
        } 
        else {
          if ( log.isLoggable(Level.FINE) 
               && !key.equals(actionTargetParameterName) )
            log.fine("unusable raw parameter name `" + key + "'");
          continue;
        }
        
        if (nd < keyLen)
          key = key.substring(nd);
        else {
          if ( log.isLoggable(Level.FINE) ) 
            log.fine("unusable . raw parameter name `" + key + "'");
          continue;
        }
      }
      else {
        if (_actionNamespace == null) {
          log.finer("unusable raw action parameter name `" + key + "'");
          continue;
        } else
          namespace = _actionNamespace;
      }

      if (invocation == null || !invocation.getNamespace().equals(namespace))
        invocation = getInvocation(namespace);

      if (windowStateParameterName.equals(key)) {
        invocation.setWindowStateName(values);
      }
      else if (portletModeParameterName.equals(key)) {
        invocation.setPortletModeName(values);
      }
      else {
        invocation.getParameterMap().put(key, values);
      }
    }

  }


  public String getURL()
  {
    _buffer.setLength(0);
    StringBuffer buf = _buffer;

    StringBuffer url = _buffer;

    if (_actionNamespace != null)
      appendReserved(url, null, _actionTargetParameterName, _actionNamespace);

    Iterator<Map.Entry<String, MapBasedInvocation>> iter 
      = _invocationMap.entrySet().iterator();

    // "view" and "normal" are defaults.  They only to to be included
    // in the url if:
    // 1) some namespace uses view or normal and has no parameters
    // 2) some other namespace uses a different portlet mode/window state
    //
    // They only need to be included once, the purpose for including them is so
    // that on a subsequent request the windowStatesUsed and portletModesUsed
    // are correct.

    String viewPortletModeNamespace = null;
    String normalWindowStateNamespace = null;
    boolean needViewPortletMode = false;
    boolean needNormalWindowState = false;

    boolean sawActionNamespace = false;

    while (iter.hasNext()) {
      Map.Entry<String, MapBasedInvocation> entry = iter.next();
      String namespace = entry.getKey();
      MapBasedInvocation invocation = entry.getValue();

      // when the namespace is the target of an action, the parameter
      // names are not encoded
      String paramNamespace = namespace == _actionNamespace ? null : namespace;

      PortletMode portletMode = invocation.getPortletMode();
      WindowState windowState = invocation.getWindowState();
      Map<String, String[]> parameterMap = invocation._parameterMap;

      boolean hasParameters = parameterMap != null && !parameterMap.isEmpty();

      if (portletMode == PortletMode.VIEW) {
        if (viewPortletModeNamespace == null && !hasParameters)
          viewPortletModeNamespace = namespace;
      } 
      else {
        needViewPortletMode = true;

        String key = _portletModeParameterName;
        String value = portletMode.toString();

        appendReserved( url, namespace, key, value );
      }


      if (windowState == WindowState.NORMAL) {
        if (normalWindowStateNamespace == null && !hasParameters)
          normalWindowStateNamespace = namespace;
      }
      else {
        needNormalWindowState = true;

        String key = _windowStateParameterName;
        String value = windowState.toString();

        appendReserved( url, namespace, key, value );
      }

      if (parameterMap != null && !parameterMap.isEmpty()) 
      {
        Iterator<Map.Entry<String, String[]>> paramIter 
          = parameterMap.entrySet().iterator();

        while (paramIter.hasNext()) {
          Map.Entry<String, String[]> paramEntry = paramIter.next();

          String paramKey = paramEntry.getKey();
          String[] paramValues = paramEntry.getValue();

          if (paramValues == null || paramValues.length == 0)
            continue;

          appendParameter( url, paramNamespace, paramKey, paramValues );
        }
      }
      
    } // iterate invocationMap

    if (needViewPortletMode && viewPortletModeNamespace != null) {
      String key = _portletModeParameterName;

      if (viewPortletModeNamespace == _actionNamespace)
        viewPortletModeNamespace = null;

      appendReserved( url, viewPortletModeNamespace, key, "view" );
    }

    if (needNormalWindowState && normalWindowStateNamespace != null) {
      String key = _windowStateParameterName;

      if (normalWindowStateNamespace == _actionNamespace)
        normalWindowStateNamespace = null;

      appendReserved( url, normalWindowStateNamespace, key, "normal" );
    }

    return url.toString();
  }

  private void appendReserved( StringBuffer url, String namespace, 
                               String key, String value )
  {
    url.append(url.length() == 0 ? '?' : '&');

    url.append(_reservedNamespace);

    if (namespace != null) {
      url.append(namespace);
      url.append('.');
    }

    url.append(_reservedNamespace);
    url.append(key);
    url.append('=');
    HttpUtil.encode(value, url);
  }

  private void appendParameter( StringBuffer url, String namespace, 
                                String key, String values[] )
  {
    for (int i = 0; i < values.length; i++) {
      url.append(url.length() == 0 ? '?' : '&');

      if (namespace != null) {
        url.append(_reservedNamespace);
        url.append(namespace);
        url.append('.');
      }

      HttpUtil.encode(key, url);
      url.append('=');
      HttpUtil.encode(values[i], url);
    }
  }


  /**
   * @return a Set of all WindowState's used.
   */
  public Set<WindowState> getWindowStatesUsed()
  {
    if (_windowStatesUsed == null) {
      _windowStatesUsed = new HashSet<WindowState>();

      if (_invocationMap != null) {
        Iterator<Map.Entry<String, MapBasedInvocation>> iter
          = _invocationMap.entrySet().iterator();

        while (iter.hasNext()) {
          Map.Entry<String, MapBasedInvocation> entry = iter.next();
          _windowStatesUsed.add(entry.getValue().getWindowState());
        }
      }
    }

    if (_windowStatesUsed.isEmpty())
      _windowStatesUsed.add(WindowState.NORMAL);

    return _windowStatesUsed;
  }

  private void addWindowStateUsed(WindowState windowState)
  {
    if (_windowStatesUsed != null)
      _windowStatesUsed.add(windowState);
  }

  /**
   * @return a Set of all PortletMode's used.
   */
  public Set<PortletMode> getPortletModesUsed()
  {
    if (_portletModesUsed == null) {
      _portletModesUsed = new HashSet<PortletMode>();

      if (_invocationMap != null) {
        Iterator<Map.Entry<String, MapBasedInvocation>> iter
          = _invocationMap.entrySet().iterator();

        while (iter.hasNext()) {
          Map.Entry<String, MapBasedInvocation> entry = iter.next();
          _portletModesUsed.add(entry.getValue().getPortletMode());
        }
      }
    }

    if (_portletModesUsed.isEmpty())
      _portletModesUsed.add(PortletMode.VIEW);

    return _portletModesUsed;
  }

  private void addPortletModeUsed(PortletMode portletMode)
  {
    if (_portletModesUsed != null)
      _portletModesUsed.add(portletMode);
  }

  /**
   * Return a Invocation appropriate for the passed namespace.
   */
  public MapBasedInvocation getInvocation(String namespace)
  {
    if (namespace == null)
      throw new NullPointerException("namespace cannot be null");

    MapBasedInvocation invocation =  _invocationMap.get(namespace);

    if (invocation == null) {
      invocation = new MapBasedInvocation(this);

      invocation.start(namespace);
      _invocationMap.put(namespace, invocation);
    }

    return invocation;
  }

  protected InvocationURL createActionURL(String namespace)
  {
    MapBasedInvocationFactory clone = clone(namespace);
    clone._actionNamespace = namespace;
    return new MapBasedInvocationURL(clone, namespace);
  }

  protected InvocationURL createRenderURL(String namespace)
  {
    MapBasedInvocationFactory clone = clone(namespace);
    clone._actionNamespace = null;
    return new MapBasedInvocationURL(clone, namespace);
  }

  public String toString()
  {
    return "[MapBasedInvocationFactory actionNamespace=" + _actionNamespace
      + " invocationMap=" + _invocationMap
      + "]";
  }

  private static class MapBasedInvocation implements Invocation, Cloneable
  {
    private MapBasedInvocationFactory _factory;

    private String _namespace;
    private WindowState _windowState = WindowState.NORMAL;
    private PortletMode _portletMode = PortletMode.VIEW;
    private Map<String, String[]> _parameterMap; 

    public MapBasedInvocation(MapBasedInvocationFactory factory)
    {
      _factory = factory;
    }

    public void start(String namespace)
    {
      if (_namespace != null)
        throw new IllegalStateException("missing finish()?");

      _namespace = namespace;
    }

    public void finish()
    {
      _windowState = WindowState.NORMAL;
      _portletMode = PortletMode.VIEW;
      if (_parameterMap != null)
        _parameterMap.clear();
      _namespace = null;
    }

    public MapBasedInvocation clone(boolean keepParameters)
    {
      try {
        MapBasedInvocation clone = (MapBasedInvocation) super.clone();

        if (keepParameters && _parameterMap != null)
          clone._parameterMap 
            = new LinkedHashMap<String, String[]>(_parameterMap);
        else
          clone._parameterMap = null;

        return clone;
      }
      catch (CloneNotSupportedException ex) {
        throw new RuntimeException(ex);
      }
    }

    public String getNamespace()
    {
      return _namespace;
    }

    public boolean isActionTarget()
    {
      return _factory.isActionTarget(_namespace);
    }

    public Map<String, String[]> getParameterMap()
    {
      if (_parameterMap == null)
        _parameterMap = new LinkedHashMap<String, String[]>();

      return _parameterMap;
    }

    public Map<String, String[]> releaseParameterMap()
    {
      Map<String, String[]> map = getParameterMap();
      _parameterMap = null;
      return map;
    }

    public WindowState getWindowState()
    {
      return _windowState;
    }

    public void setWindowState(WindowState windowState)
    {
      _windowState = windowState == null ? WindowState.NORMAL : windowState;
      _factory.addWindowStateUsed(_windowState);
    }

    void setWindowStateName(String[] values)
    {
      String windowStateName 
        = values == null || values.length == 0 ? null : values[0];

      if (windowStateName == null)
        setWindowState(WindowState.NORMAL);
      else if (windowStateName.equals("normal"))
          setWindowState(WindowState.NORMAL);
      else if (windowStateName.equals("minimized"))
          setWindowState(WindowState.MINIMIZED);
      else if (windowStateName.equals("maximized"))
        setWindowState(WindowState.MAXIMIZED);
      else
        setWindowState(new WindowState(windowStateName));
    }

    public PortletMode getPortletMode()
    {
      return _portletMode;
    }

    public void setPortletMode(PortletMode portletMode)
    {
      _portletMode = portletMode == null ? PortletMode.VIEW : portletMode;
      _factory.addPortletModeUsed(_portletMode);
    }

    void setPortletModeName(String[] values)
    {
      String portletModeName 
        = values == null || values.length == 0 ? null : values[0];

      if (portletModeName == null)
        setPortletMode(PortletMode.VIEW);
      else if (portletModeName.equals("view"))
          setPortletMode(PortletMode.VIEW);
      else if (portletModeName.equals("edit"))
          setPortletMode(PortletMode.EDIT);
      else if (portletModeName.equals("help"))
          setPortletMode(PortletMode.HELP);
      else
        setPortletMode(new PortletMode(portletModeName));
    }

    public InvocationURL createActionURL()
    {
      return _factory.createActionURL(_namespace);
    }

    public InvocationURL createRenderURL()
    {
      return _factory.createRenderURL(_namespace);
    }

    public String toString()
    {
      return "[MapBasedInvocationFactory " 
        + " namespace=" + _namespace
        + " windowState=" + _windowState
        + " portletMode=" + _portletMode
        + " parameters=" + _parameterMap
        + "]";

    }
  }

  static class MapBasedInvocationURL 
    extends InvocationURL
  {
    MapBasedInvocationFactory _factory;

    MapBasedInvocationURL(MapBasedInvocationFactory factory, String namespace)
    {
      super(factory, namespace);
      _factory = factory;
    }

    public String getURL()
    {
      return _factory.getURL();
    }

  }
}

