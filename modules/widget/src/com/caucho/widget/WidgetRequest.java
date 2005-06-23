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

package com.caucho.widget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;


public interface WidgetRequest
  extends VarContext
{
  /**
   * Find a widget within the same namespace.
   */
  public <T> T find(String name);

  /**
   * @see javax.servlet.http.HttpServletRequest#setCharacterEncoding(String encoding)
   */
  public void setCharacterEncoding(String encoding)
    throws UnsupportedEncodingException;

  /**
   * @see javax.servlet.http.HttpServletRequest#getCharacterEncoding()
   */
  public String getCharacterEncoding();

  /**
   * @see javax.servlet.http.HttpServletRequest#getContentLength()
   */
  public int getContentLength();

  /**
   * @see javax.servlet.http.HttpServletRequest#getContentType()
   */
  public String getContentType();

  /**
   * @see javax.servlet.http.HttpServletRequest#getInputStream()
   */
  public InputStream getInputStream()
    throws IOException;

  /**
   * @see javax.servlet.http.HttpServletRequest#getReader()
   */
  public BufferedReader getReader()
    throws IOException, IllegalStateException;

  /**
   * @see WidgetInit#setApplicationAttribute(String,Object)
   */
  public void setApplicationAttribute(String name, Object value);

  /**
   * @see WidgetInit#getApplicationAttribute(String)
   */
  public <T> T getApplicationAttribute(String name);

  /**
   * @see WidgetInit#getApplicationAttributeNames()
   */
  public Enumeration<String> getApplicationAttributeNames();

  /**
   * @see WidgetInit#removeApplicationAttribute(String name)
   */
  public void removeApplicationAttribute(String name);

  /**
   * This method is provided for interaction with traditional servlet's and jsp's;
   * {@link #getVar(Widget, String)} is preferred.
   *
   * @see javax.servlet.http.HttpServletRequest#getAttribute(String name)
   */
  public <T> T getRequestAttribute(String name);

  /**
   * This method is provided for interaction with traditional servlet's and jsp's;
   * {@link #setVar(Widget, String, Object)} is preferred.
   *
   * @see javax.servlet.http.HttpServletRequest#setAttribute(String name, Object o)
   */
  public void setRequestAttribute(String name, Object o);

  /**
   * @see javax.servlet.http.HttpServletRequest#getAttributeNames()
   */
  public Enumeration<String> getRequestAttributeNames();

  /**
   * This method is provided for interaction with traditional servlet's and jsp's.
   *
   * @see javax.servlet.http.HttpServletRequest#removeAttribute(String name)
   */
  public void removeRequestAttribute(String name);

  /**
   * This method is provided for interaction with traditional servlet's and jsp's.
   * {@link #getVar(Widget, String)} with var defined as persistent
   * {@link VarDefinition#setPersistent(boolean)} is preferred.
   *
   * @see javax.servlet.http.HttpSession#getAttribute(String name)
   */
  public <T> T getSessionAttribute(String name);

  /**
   * This method is provided for interaction with traditional servlet's and jsp's;
   * {@link #setVar(Widget, String, Object)} with var defined as persistent
   * {@link VarDefinition#setPersistent(boolean)} is preferred.
   *
   * @see javax.servlet.http.HttpSession#setAttribute(String name, Object o)
   */
  public void setSessionAttribute(String name, Object o);

  /**
   * This method is provided for interaction with traditional servlet's and jsp's;
   *
   * @see javax.servlet.http.HttpSession#getAttributeNames()
   */
  public Enumeration<String> getSessionAttributeNames();

  /**
   * This method is provided for interaction with traditional servlet's and jsp's.
   *
   * @see javax.servlet.http.HttpSession#removeAttribute(String name)
   */
  public void removeSessionAttribute(String name);

  public WidgetRequestChain getRequestChain();

  public void finish();
}

