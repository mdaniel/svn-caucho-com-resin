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

import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;
import javax.portlet.WindowState;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A Portal implementation with default values.
 */ 
public class GenericPortal 
  implements Portal, PortalContext
{
  static final public Logger log = 
    Logger.getLogger(GenericPortal.class.getName());

  private String _portalInfo = "GenericPortal/1.0";
  private String _reservedNamespace = "__";
  private Map<String, String> _propertyMap;
  private Set<WindowState> _supportedWindowStates;
  private Set<PortletMode> _supportedPortletModes;

  private Set<String> _userAttributeNames;

  private PreferencesStore _preferencesStore;
  private UserAttributeStore _userAttributeStore;

  private Cache _cache;
  private BufferFactory _bufferFactory;

  private boolean _isInit;

  public PortalContext getPortalContext()
  {
    return this;
  }

  /**
   * Set the value to be returned by {@link #getPortalInfo}, the default
   * is "GenericPortal/1.0".
   */
  public void setPortalInfo(String portalInfo)
  {
    _portalInfo = portalInfo;
  }

  /**
   * {@inheritDoc}
   */
  public String getPortalInfo()
  {
    return _portalInfo;
  }
  /**
   * Set the reserved namespace, the default is "__".
   */
  public void setReservedNamespace(String reservedNamespace)
  {
    _reservedNamespace = reservedNamespace;
  }

  /**
   * {@inheritDoc}
   */
  public String getReservedNamespace()
  {
    return _reservedNamespace;
  }

  /**
   * Set a portal property that is available with {@link #getProperty}.
   */
  public void setProperty(String name, String value)
  {
    if (_propertyMap == null)
      _propertyMap = new LinkedHashMap<String, String>();

    _propertyMap.put(name, value);
  }

  /**
   * {@inheritDoc}
   */
  public String getProperty(String name)
  {
    if (_propertyMap == null)
      return null;

    return _propertyMap.get(name);
  }

  /**
   * {@inheritDoc}
   */
  public Enumeration getPropertyNames()
  {
    if (_propertyMap == null)
      return Collections.enumeration(Collections.EMPTY_LIST);
    else
      return Collections.enumeration(_propertyMap.keySet());
  }

  /**
   * Set the supported portlet modes.  If this method is not called then
   * then {@link #isPortletModeAllowed} will always return true (all portlet
   * modes are allowed) but {@link #getSupportedPortletModes} will return an
   * empty enumeration.
   */
  public void setSupportedPortletModes(Set<PortletMode> portletModes)
  {
    _supportedPortletModes = portletModes;
  }

  /**
   * {@inheritDoc}
   */
  public Enumeration getSupportedPortletModes()
  {
    if (_supportedPortletModes == null)
      return Collections.enumeration(Collections.EMPTY_LIST);
    else
      return Collections.<PortletMode>enumeration(_supportedPortletModes);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isPortletModeAllowed(PortletRequest portletRequest,
                                      PortletMode portletMode)
  {
    if (_supportedPortletModes == null)
      return true;
    else
      return _supportedPortletModes.contains(portletMode);
  }

  /**
   * Set the supported window states.  If this method is never called
   * and no window states are specified, then {@link #isWindowStateAllowed}
   * will always return true (all window states are allowed) but 
   * {@link #getSupportedWindowStates} will return an empty enumeration.
   */
  public void setSupportedWindowStates(Set<WindowState> windowStates)
  {
    _supportedWindowStates = windowStates;
  }

  /**
   * {@inheritDoc}
   */
  public Enumeration getSupportedWindowStates()
  {

    if (_supportedWindowStates == null)
      return Collections.enumeration(Collections.EMPTY_LIST);
    else
      return Collections.enumeration(_supportedWindowStates);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isWindowStateAllowed(PortletRequest portletRequest,
                                      WindowState windowState)
  {
    if (_supportedWindowStates == null)
      return true;
    else
      return _supportedWindowStates.contains(windowState);
  }

  /**
   * Set the {@link #PreferencesStore}, obtained once for each
   * connection and then used to obtain a preferences store for each namespace
   * as needed.  The default is an instance of 
   * {@link #SessionPreferencesStore}.
   */
  public void setPreferencesStore(PreferencesStore preferencesStore)
  {
    _preferencesStore = preferencesStore;
  }

  /**
   * {@inheritDoc}
   */
  public PreferencesStore getPreferencesStore()
  {
    if (_preferencesStore == null)
      _preferencesStore = new SessionPreferencesStore();

    return _preferencesStore;
  }

  /**
   * Set the user attribute names that the portlet can use, the default
   * is to allow all user attributes to be visible to the portlet.
   */
  public void setUserAttributeNames(Set<String> userAttributeNames)
  {
    if (_userAttributeNames != null)
      throw new IllegalArgumentException("user-attribute-names already set");

    _userAttributeNames = userAttributeNames;
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getUserAttributeNames()
  {
    return _userAttributeNames;
  }

  /**
   * Set the {@link #UserAttributeStore}, obtained once for each
   * connection and then used to obtain user attributes
   * as needed.  The default is null, which means that no user attributes
   * are available.
   */
  public void setUserAttributeStore(UserAttributeStore userAttributeStore)
  {
    _userAttributeStore = userAttributeStore;
  }

  /**
   * {@inheritDoc}
   */
  public UserAttributeStore getUserAttributeStore()
  {
    return _userAttributeStore;
  }

  /**
   * Caching is currently unimplemented, attempting to set this value will cause
   * an exception to be thrown.
   */
  public void setCache(Cache cache)
  {
    _cache = cache;
    throw new UnsupportedOperationException("cache not currently implemented");
  }

  /**
   * {@inheritDoc}
   */
  public Cache getCache()
  {
    return _cache;
  }

  /**
   * The default is an instance of BufferFactoryImpl.
   */ 
  public void setBufferFactory(BufferFactory bufferFactory)
  {
    _bufferFactory = bufferFactory;
  }

  /**
   * @inheritDoc
   */
  public BufferFactory getBufferFactory()
  {
    if (_bufferFactory == null)
      _bufferFactory = new BufferFactoryImpl();

    return _bufferFactory;
  }
}

