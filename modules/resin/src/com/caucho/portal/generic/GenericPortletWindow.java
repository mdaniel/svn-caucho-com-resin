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

import java.io.IOException;

import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;

/**
 * A Window that contains one {@link javax.portlet.Portlet}.
 */
public class GenericPortletWindow
  extends GenericWindow
{
  static protected final Logger log = 
    Logger.getLogger(GenericPortletWindow.class.getName());

  private Portlet _portlet;
  private boolean _isPortletNameSet;

  public GenericPortletWindow()
  {
  }
 
  /**
   * The portlet.  This method can be called in derived classes,
   * or through the use of dependency injection on containers that support it.
   */
  public void setPortlet(Portlet portlet)
  {
    if (_portlet != null)
      throw new IllegalArgumentException("`portlet' is already set");

    _portlet = portlet;
  }


  /**
   * An alternative to {@link #setPortlet(Portlet)}, specify the class
   * name of an object to instantiate
   */
  public void setPortletClass(String className)
  {
    setPortlet( (Portlet) newInstance(Portlet.class, className) );
  }

  /** 
   * The default is the value of portlet-class.
   */ 
  public void setPortletName(String portletName)
  {
    super.setPortletName(portletName);
    _isPortletNameSet = true;
  }

  public void init(PortletContext portletContext)
    throws PortletException
  {
    super.init(portletContext);

    if (_portlet == null)
      throw new PortletException("`portlet' is required");

    if (!_isPortletNameSet)
       setPortletName(_portlet.getClass().getName());

    _portlet.init(this);
  }


  protected Portlet getPortlet()
  {
    if (_portlet == null)
      throw new IllegalStateException("missing init()?");

    return _portlet;
  }

  /**
   * Use the PortletConnection to get an Action appropriate for this window,
   * and then call processAction on the portlet that is contained within this
   * Window if the Portlet is the target of the action.  
   */
  public void processAction(PortletConnection connection)
    throws PortletException, IOException
  {
    Action action = connection.getAction(this, getNamespace());

    if (action != null) {
      try {
        if (action.isTarget())
          action.processAction(getPortlet());
      }
      finally {
        action.finish();
      }
    }
  }

  /**
   * Use the PortletConnection to get a Render appropriate for this window,
   * and then call render on the portlet that is contained within this
   * Window.
   */
  public void render(PortletConnection connection)
    throws PortletException, IOException
  {
    Render render = connection.getRender(this, getNamespace());

    if (render != null) {
      try {
        render.render(getPortlet());
      }
      finally {
        render.finish();
      }
    }
  }

  public void destroy()
  {
    Portlet portlet = _portlet;

    _portlet = null;

    try {
      if (portlet != null)
        portlet.destroy();
    } 
    catch (Exception ex) {
      log.log(Level.WARNING, ex.toString(), ex);
    }
  }
}

