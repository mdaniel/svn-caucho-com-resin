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
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import java.util.Map;

/**
 */
public interface PortalResponse
{
  /**
   * Set the portlet modes for a namespace.  
   * If the portlet matching the namespace has already had processAction()
   * or render() called on it,  an exception is thrown because it
   * is too late to set the portlet mode.
   *
   * @throw IllegalStateException if it is too late in the processing of the
   * request to set the portlet mode for the namespace
   *
   * @throw IllegalArgumentException if the namespace is not known
   */
  public void setPortletMode(String namespace, PortletMode portletMode)
    throws PortletModeException;

  /**
   * Set the window state for a namespace.  
   * If the portlet matching the namespace has already had processAction()
   * or render() called on it,  an exception is thrown because it
   * is too late to set the window state.
   *
   * @throw IllegalStateException if it is too late in the processing of the
   * request to set the window state for the namespace
   *
   * @throw IllegalArgumentException if the namespace is not known
   */
  public void setWindowState(String namespace, WindowState windowState)
    throws WindowStateException;

  /**
   * Set parameters for a namespace to the map.
   * If the portlet matching the namespace has already had processAction()
   * or render() called on it,  an exception is thrown because it
   * is too late to set parameters.
   *
   * @throw IllegalStateException if it is too late in the processing of the
   * request to set parameters for the namespace
   *
   * @throw IllegalArgumentException if the namespace is not known
   */
  public void setParameters(String namespace, Map<String, String[]> srcMap);

  /**
   * Set a Rparameter for a namespace.
   * If the portlet matching the namespace has already had processAction()
   * or render() called on it,  an exception is thrown because it
   * is too late to set parameters.
   *
   * @throw IllegalStateException if it is too late in the processing of the
   * request to set parameters for the namespace
   *
   * @throw IllegalArgumentException if the namespace is not known
   */
  public void setParameter(String namespace, String name, String value);

  /**
   * Set a parameter for a namespace.
   * If the portlet matching the namespace has already had processAction()
   * or render() called on it,  an exception is thrown because it
   * is too late to set parameters.
   *
   * @throw IllegalStateException if it is too late in the processing of the
   * request to set parameters for the namespace
   *
   * @throw IllegalArgumentException if the namespace is not known
   */
  public void setParameter(String namespace, String name, String[] values);

  /**
   * Create a render url to another namespace.
   *
   * @param keepParameters if true, then the render parameters that exist for
   * the namespace for this request are maintained for the next request.
   * If false, the url when first formed will have no parameters.
   */
  public PortalURL createRenderURL(String namespace, boolean keepParameters);

  /**
   * Create an action url that targets another namespace.
   *
   * @param isSticky if true, then the render parameters that exist for the
   * namespace for this request are maintained for the next request.
   * If false, the url when first formed will have no parameters.
   */
  public PortalURL createActionURL(String namespace, boolean keepParameters);

}
