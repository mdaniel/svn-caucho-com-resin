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
import java.util.Set;

public interface Portal 
{
  public PortalContext getPortalContext();

  /**
   * Return true if the WindowState is allowed.
   * <code>portletRequest.getResponseContentType()</code> can be used
   * if the allowed portlet modes depends on the mime type of the
   * response.
   */
  public boolean isWindowStateAllowed(PortletRequest portletRequest,
                                      WindowState windowState);

  /**
   * Return true if the PortletMode is allowed.
   * <code>portletRequest.getResponseContentType()</code> can be used
   * if the allowed portlet modes depends on the mime type of the
   * response.
   */
  public boolean isPortletModeAllowed(PortletRequest portletRequest,
                                      PortletMode portletMode);

  /**
   * The reserved namespace is used to mark parameters that have special
   * meaning to the portal.  The specification suggests "javax.portal.", which
   * is rather long so the default is ususally "__".  
   */
  public String getReservedNamespace();

  /**
   * Return a PreferencesStore for a connection.  
   * The store is obtained once for each connection and used throughout the
   * course of the connection to obtain a 
   * {@link javax.portlet.PortletPreferences} for each namespace.
   */
  public PreferencesStore getPreferencesStore();

  /**
   * Return a Set of all user attributes names that this portlet uses,
   * null if all user attributes available from the portal should be made
   * available.
   */
  public Set<String> getUserAttributeNames();

  /**
   * Return a UserAttributeStore.  
   * The store is obtained once for each connection if needed.
   */
  public UserAttributeStore getUserAttributeStore();

  /**
   * XXX: caching not currently implemented 
   *
   * @return null to disable caching
   */
  public Cache getCache();

  /**
   * @return null to disable buffering
   */
  public BufferFactory getBufferFactory();
}

