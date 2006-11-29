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

import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Window that is a container for one or more Windows.
 */
public class GenericLayoutWindow
  extends GenericWindow
{
  static protected final Logger log = 
    Logger.getLogger(GenericLayoutWindow.class.getName());

  private ArrayList<GenericWindow> _children
    = new ArrayList<GenericWindow>();

  public GenericLayoutWindow()
  {
  }
 
  /**
   * Add a window to the layout.
   */
  public void add(GenericWindow window)
  {
    _children.add(window);
  }

  /**
   * Add a {@link GenericPortletWindow} to the layout, a GenericPortletWindow
   * is a Window that contains one {@link javax.portlet.Portlet} .  This method
   * provides the same functionality as {@link #add()}; it is included for
   * dependency injection containers that support injection based on method
   * names (like Resin).
   */
  public void addWindow(GenericPortletWindow portletWindow)
  {
    _children.add(portletWindow);
  }

  /**
   * Add a {@link GenericLayoutWindow} to the layout.  In this way layouts can
   * be nested to an arbitrary depth and complexity.  This method provides the
   * same functionality as {@link #add()}; it is
   * included for dependency injection containers that support injection based
   * on method names (like Resin).
   */
  public void addLayout(GenericLayoutWindow layoutWindow)
  {
    _children.add(layoutWindow);
  }

  /**
   * init() each child window.
   */
  public void init(PortletContext portletContext)
    throws PortletException
  {
    super.init(portletContext);

    for (int i = 0; i < _children.size(); i++) {
      _children.get(i).init(portletContext);
    }
  }

  /**
   * Use the PortletConnection to get an Action appropriate for this window,
   * and then call processAction on each child window.
   */
  public void processAction(PortletConnection connection)
    throws PortletException, IOException
  {
    Action action = connection.getAction(this, getNamespace());

    if (action != null) {
      try {
        for (int i = 0; i < _children.size(); i++) {
          _children.get(i).processAction(connection);
        }
      }
      finally {
        action.finish();
      }
    }
  }

  /**
   * Use the PortletConnection to get a Render appropriate for this window,
   * and then call render on each child window.
   */
  public void render(PortletConnection connection)
    throws PortletException, IOException
  {
    Render render = connection.getRender(this, getNamespace());

    if (render != null) {
      try {
        for (int i = 0; i < _children.size(); i++) {
          _children.get(i).render(connection);
        }
      }
      finally {
        render.finish();
      }
    }
  }

  /**
   * Destroy each child window.
   */
  public void destroy()
  {
    ArrayList<GenericWindow> children 
      = new ArrayList<GenericWindow>(_children);

    _children.clear();

    for (int i = 0; i < children.size(); i++) {
      try {
        children.get(i).destroy();
      }
      catch (Exception ex) {
          log.log(Level.WARNING, ex.toString(), ex);
      }
    }
  }
}

