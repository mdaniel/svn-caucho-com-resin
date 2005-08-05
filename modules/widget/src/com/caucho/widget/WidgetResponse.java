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

import com.caucho.vfs.XmlWriter;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Locale;


public interface WidgetResponse
  extends VarContext
{
  /**
   * Create a url that reinvokes the widgets.  All widgets in the tree
   * have their {@link Widget#url(WidgetURL)} method called,
   * giving them an opportunity to save any parameters needed to restore their
   * state on a subsequent request.
   *
   * @return a String
   */
  public WidgetURL getUrl()
    throws WidgetException;

  /**
   * Create a form submit url for a form that contains child widgets that
   * create controls for the form. Parameters
   * in the form become the parameter retrieved by child.getParameter() on
   * subsequent requests.
   *
   * All widgets in
   * the tree have their {@link Widget#url(WidgetURL)} method called,
   * giving them an opportunity to save any parameters needed to restore their
   * state on a subsequent request.
   */
  public WidgetURL getSubmitUrl()
    throws WidgetException;

  /**
   * Create a form submit url for a form; parameters in the form become named
   * parameters retrieved by widget.getParameter(name) on subsequent requests.
   *
   * All widgets in
   * the tree have their {@link Widget#url(WidgetURL)} method called,
   * giving them an opportunity to save any parameters needed to restore their
   * state on a subsequent request.
   */
  public WidgetURL getParameterUrl()
    throws WidgetException;

  /**
   * Create a URL to a resource that is not a widget, for example
   * an image, css file, external html or pdf document, etc.
   *
   * TODO: determine search strategy, should be able to make
   * urls to files obtained with getClass().getResource
   */
  public String getResourceUrl(String path);

  /**
   * Find a widget within the same namespace.
   */
  public <T> T find(String name);

  /**
   * @see javax.servlet.http.HttpServletResponse#addCookie(Cookie cookie)
   */
  public void addCookie(Cookie cookie);

  /**
   * @see javax.servlet.http.HttpServletResponse#setContentType(String type)
   */
  public void setContentType(String type);

  /**
   * @see javax.servlet.http.HttpServletResponse#getContentType()
   */
  public String getContentType();


  /**
   * @see javax.servlet.http.HttpServletResponse#getCharacterEncoding()
   */
  public String getCharacterEncoding();


  /**
   * @see javax.servlet.http.HttpServletResponse#setCharacterEncoding(String charset)
   */
  public void setCharacterEncoding(String charset);

  /**
   * @see javax.servlet.http.HttpServletResponse#setLocale(Locale locale)
   */
  public void setLocale(Locale locale);

  /**
   * @see javax.servlet.http.HttpServletResponse#getLocale()
   */
  public Locale getLocale();

  /**
   * @see javax.servlet.http.HttpServletResponse#getOutputStream()
   */
  public OutputStream getOutputStream()
    throws IOException;


  /**
   * @see javax.servlet.http.HttpServletResponse#getWriter()
   */
  public XmlWriter getWriter()
    throws IOException;

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
   * @see javax.servlet.http.HttpServletRequest#setAttribute(String name, Object value)
   */
  public void setRequestAttribute(String name, Object value);

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
   * @see javax.servlet.http.HttpSession#setAttribute(String name, Object value)
   */
  public void setSessionAttribute(String name, Object value);

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

  public WidgetResponseChain getResponseChain();

  public void finish();

  /* XXX: check these, these are HttpServletResponse methods that have not been included

   public void setStatus(int sc)

   public void sendError(int sc, String msg)
   throws IOException

   public void sendError(int sc)
   throws IOException

   public void sendRedirect(String location)
   throws IOException

   public void setHeader(String name, String value)

   public void addHeader(String name, String value)

   public boolean containsHeader(String name)

   public void setDateHeader(String name, long date)

   public void addDateHeader(String name, long date)

   public void setIntHeader(String name, int value)

   public void addIntHeader(String name, int value)

   public String encodeURL(String url)

   public String encodeRedirectURL(String name)

   public void setStatus(int sc, String msg)

   public void setBufferSize(int size)

   public int getBufferSize()

   public void flushBuffer()
   throws IOException

   public boolean isCommitted()

   public void reset()

   public void resetBuffer()

   public String encodeUrl(String url)

   public String encodeRedirectUrl(String url)
   **/
}
