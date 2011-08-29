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

import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletSecurityException;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

abstract public class InvocationURL
{
  private InvocationFactory _invocationFactory;
  private String _namespace;
  private Invocation _invocation;

  private Boolean _isSecure;

  /**
   * @param invocationFactory the InvocationFactory
   *
   * @param namespace the namespace that is the default target for setting
   *                   parameters, portlet mode, and window state
   */ 
  public InvocationURL( InvocationFactory invocationFactory, 
                        String namespace )
  {
    _invocationFactory = invocationFactory;
    _namespace = namespace;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  protected Invocation getInvocation()
  {
    if (_invocation == null)
      _invocation = _invocationFactory.getInvocation(_namespace);

    return _invocation;
  }

  protected Invocation getInvocation(String namespace)
  {
    return _invocationFactory.getInvocation(namespace);
  }

  protected void setWindowState(Invocation invocation, WindowState windowState)
    throws WindowStateException
  {
    invocation.setWindowState(windowState);
  }

  public void setWindowState(WindowState windowState)
    throws WindowStateException
  {
    setWindowState(getInvocation(), windowState);
  }

  public void setWindowState(String namespace, WindowState windowState)
    throws WindowStateException
  {
    setWindowState(getInvocation(namespace), windowState);
  }

  protected void setPortletMode(Invocation invocation, PortletMode portletMode)
    throws PortletModeException
  {
    invocation.setPortletMode(portletMode);
  }

  public void setPortletMode(PortletMode portletMode)
    throws PortletModeException
  {
    setPortletMode(getInvocation(), portletMode);
  }

  public void setPortletMode(String namespace, PortletMode portletMode)
    throws PortletModeException
  {
    setPortletMode(getInvocation(namespace), portletMode);
  }

  protected Map<String, String[]> getRenderParameterMap()
  {
    return getInvocation().getParameterMap();
  }

  protected Map<String, String[]> getRenderParameterMap(String namespace)
  {
    return getInvocation(namespace).getParameterMap();
  }

  private void checkNullName(String name)
  {
    if (name == null)
      throw new IllegalArgumentException("parameter name cannot be null");
  }

  private void checkNullValue(Object value)
  {
    if (value == null)
      throw new IllegalArgumentException("parameter value cannot be null");
  }

  protected String getParameterValue(Map<String, String[]> map, String name)
  {
    checkNullName(name);

    String values[] = map.get(name);

    return values == null || values.length == 0 ? null : values[0];
  }

  protected String[] getParameterValues( Map<String, String[]> map, 
                                         String name)
  {
    checkNullName(name);
    return map.get(name);
  }

  protected Enumeration getParameterNames(Map<String, String[]> map)
  {
    return Collections.enumeration(map.keySet());
  }

  protected void setParameter( Map<String, String[]> map, 
                               String name, 
                               String value)
  {
    checkNullName(name);
    checkNullValue(value);

    map.put(name, new String[] { value });
  }

  protected void setParameters( Map<String, String[]> destMap, 
                                Map<String, String[]> srcMap)
  {
    checkNullValue(srcMap);
    destMap.clear();

    Iterator<Map.Entry<String, String[]>> iter 
      = srcMap.entrySet().iterator();

    while (iter.hasNext()) 
    {
      Map.Entry<String, String[]> entry = iter.next();

      setParameter(destMap, entry.getKey(), entry.getValue());
    }
  }

  protected void setParameter( Map<String, String[]> map, 
                               String name, 
                               String[] values)
  {
    checkNullName(name);
    checkNullValue(values);

    if (values.length == 0)
      map.remove(name);
    else
      map.put(name, values);
  }

  public void setParameter(String name, String value)
  {
    setParameter(getRenderParameterMap(), name, value);
  }

  public void setParameter(String name, String[] values)
  {
    setParameter(getRenderParameterMap(), name, values);
  }

  public void setParameters(Map<String, String[]> parameters)
  {
    setParameters(getRenderParameterMap(), parameters);
  }

  public void setParameter(String namespace, String name, String value)
  {
    setParameter(getRenderParameterMap(namespace), name, value);
  }

  public void setParameter(String namespace, String name, String[] values)
  {
    setParameter(getRenderParameterMap(namespace), name, values);
  }

  public void setParameters(String namespace, Map<String, String[]> parameters)
  {
    setParameters(getRenderParameterMap(namespace), parameters);
  }

  public void setSecure(boolean secure) 
    throws PortletSecurityException
  {
    _isSecure = secure ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * True if setSecure() was called for this url, even if it was
   * called with setSecure(false)
   */
  protected boolean isSecureSpecified()
  {
    return _isSecure != null;
  }

  protected boolean isSecure()
  {
    return _isSecure == null || _isSecure == Boolean.FALSE;
  }

  /**
   * Return a partially formed URL, it is then resolved to a full URL by the
   * Portal.
   */
  abstract public String getURL();
}
