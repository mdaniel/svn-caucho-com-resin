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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;

/**
 * <h3>Template</h3> 
 *
 * To implement a renderer that adds decoration to content
 * rendered with the PrintWriter, the following are overridden:
 *
 * <pre>
 * protected void beginPage( PrintWriter out, RenderRequest request, String namespace )
 *  throws IOException
 * {
 *   ... 
 * }
 *
 * protected void beginWindow( PrintWriter out, RenderRequest request, String namespace )
 *  throws IOException
 * {
 *   ... 
 * }
 *
 * protected void endWindow( PrintWriter out, RenderRequest request, String namespace )
 *  throws IOException
 * {
 *   ... 
 * }
 *
 * protected void endPage( PrintWriter out, RenderRequest request, String namespace )
 *  throws IOException
 * {
 *   ... 
 * }
 *
 * <h3>Helper methods</h3> 
 * <ul>
 * <li><code>{@link #printEscaped(java.io.PrintWriter,java.lang.CharSequence) printEscaped(out, string)};</code>
 * <li><code>String contentType = {@link #getContentType(javax.portlet.RenderRequest) getContentType(request)}; </code>
 * <li><code>Locale locale = {@link #getLocale(javax.portlet.RenderRequest) getLocale(request)}; </code>
 * <li><code>String title = {@link #getTitle(javax.portlet.RenderRequest) getTitle(request)}; </code>
 * <li><code>String shortTitle = {@link #getShortTitle(javax.portlet.RenderRequest) getShortTitle(request)}; </code>
 * <li><code>PortletURL url = {@link #getControlURL(javax.portlet.RenderRequest) getControlURL(request)}; </code>
 * <li><code>Set<PortletMode> portletModes = {@link #getPortletModes(javax.portlet.RenderRequest) getPortletModes(request)}; </code>
 * <li><code>Set<WindowState> windowStates = {@link #getWindowStates(javax.portlet.RenderRequest) getWindowStates(request)}; </code>
 * <li><code>ResourceBundle resourceBundle = {@link #getResourceBundle(javax.portlet.RenderRequest) getResourceBundle(request)}; </code>
 * <li><code>PortletConfig portletConfig = {@link #getPortletConfig(javax.portlet.RenderRequest) getPortletConfig(request)}; </code>
 * <li><code>RenderResponse renderResponse = {@link #getRenderResponse(javax.portlet.RenderRequest) getRenderResponse(request)}; </code>
 */

abstract public class AbstractRenderer implements Renderer 
{
  public static final String PAGE_HEADER_RENDERED 
    = "com.caucho.portal.generic.AbstractRenderer.PAGE_HEADER_RENDERED";

  protected static final Logger log = 
    Logger.getLogger(AbstractRenderer.class.getName());

  private boolean _isAlwaysWrite = true;
  private boolean _isAlwaysStream;
  private String _defaultContentType = "text/html";
  private int _bufferSize = -1;

  private Boolean _isDecorateWindow;

  /**
   * If true beginWindow() and endWindow() are always called, if false never
   * called, the default is to call beginWindow() and endWindow() unless
   * beginPage() and endPage() are called.
   */ 
  public void setDecorateWindow(boolean isDecorateWindow)
  {
    _isDecorateWindow = isDecorateWindow ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * If true the portal will always call getWriter(), even if the portlet does
   * not call getWriter(), unless getOutputStream() has been called.
   *
   * The default is true. 
   */ 
  public void setAlwaysWrite(boolean isAlwaysWrite)
  {
    _isAlwaysWrite = isAlwaysWrite;
  }


  /**
   * If true the portal will always call getOutputStream(), even if the portlet
   * does not call getOutputStream(), unless getWriter() has been called.
   *
   * The default is false. 
   */ 
  public void setAlwaysStream(boolean isAlwaysStream)
  {
    _isAlwaysStream = isAlwaysStream;
  }

  /**
   * The default content type used if {@link #isAlwaysWrite()}
   * or {@link #isAlwaysStream()} is true and a 
   * Writer or OutputStream is obtained before the content type of the
   * response has been set, default is "text/html". 
   */
  public void setDefaultContentType(String defaultContentType)
  {
    _defaultContentType = defaultContentType;
  }

  /**
   * The default is -1, which allows the portal to choose a buffer size.
   * 0 disables buffering of the renderer output. 
   */
  public void setBufferSize(int bufferSize)
  {
    _bufferSize = bufferSize;
  }

  /**
   * Return true if the WindowState is allowed.
   *
   * <code>portletRequest.getResponseContentType()</code> can be used
   * if the allowed window states is dependent on the mime type of the
   * response.
   *
   * The default is to return true. 
   */
  public boolean isWindowStateAllowed( PortletRequest request,
                                       WindowState windowState )
  {
    return true;
  }

  /**
   * Return true if the WindowState is allowed.
   *
   * <code>portletRequest.getResponseContentType()</code> can be used
   * if the allowed portlet modes is dependent on the mime type of the
   * response.
   *
   * The default is to return true. 
   */
  public boolean isPortletModeAllowed( PortletRequest request,
                                       PortletMode portletMode )
  {
    return true;
  }

  public boolean isAlwaysWrite()
  {
    return _isAlwaysWrite;
  }

  public boolean isAlwaysStream()
  {
    return _isAlwaysStream;
  }

  public String getDefaultContentType()
  {
    return _defaultContentType;
  }

  public int getBufferSize()
  {
    return _bufferSize;
  }

  /**
   * Return a Writer that wraps the passed PrintWriter, or null
   * if there is no specialized writer for this request.
   *
   * This implementation calls beginPage() if needed and beginWindow()
   * before returning the writer. 
   */
  public PrintWriter getWriter( PrintWriter out, 
                                RenderRequest request, 
                                String namespace )
    throws IOException
  {
    boolean doPage = (request.getAttribute(PAGE_HEADER_RENDERED) == null);

    boolean doWindow = _isDecorateWindow == null 
                        ? !doPage
                        : _isDecorateWindow.booleanValue();

    if (doPage) {
      beginPage(out, request, namespace);
      request.setAttribute(PAGE_HEADER_RENDERED, this);
    }

    if (doWindow)
      beginWindow(out, request, namespace);

    return out;
  }

  /**
   * Derived classes override to insert header content for a page,
   * called only when beginPage() has not been called on some instance of
   * AbstractRenderer for this request.
   */
  protected void beginPage( PrintWriter out, 
                            RenderRequest request, 
                            String namespace )
    throws IOException
  {
  }

  /**
   * Derived classes override to insert header content before a portlet
   * has been rendered.
   */
  protected void beginWindow( PrintWriter out, 
                              RenderRequest request, 
                              String namespace )
    throws IOException
  {
  }

  /**
   * Derived classes override to append footer content after a portlet
   * has been rendered.
   */
  protected void endWindow( PrintWriter out, 
                            RenderRequest request, 
                            String namespace )
    throws IOException
  {
  }

  /**
   * Derived classes override to insert footer content for a page,
   * called from  only when beginPage() has been called for the Window.
   */
  protected void endPage( PrintWriter out, 
                          RenderRequest request, 
                          String namespace )
    throws IOException
  {
  }

  /**
   * Finish with a Writer produced by this factory.
   * This may be called even if the writer threw an Exception.
   *
   * This implementation calls endWindow() and endPage() if needed. 
   */
  public void finish( PrintWriter out, 
                      RenderRequest request,
                      String namespace,
                      boolean isDiscarded )
    throws IOException
  {
    boolean doPage = (this == request.getAttribute(PAGE_HEADER_RENDERED));

    boolean doWindow = _isDecorateWindow == null 
                        ? !doPage
                        : _isDecorateWindow.booleanValue();

    if (!isDiscarded) {
      if (doWindow)
        endWindow(out, request, namespace);

      if (doPage)
        endPage(out, request, namespace);
    }
    else {
      if (this == request.getAttribute(PAGE_HEADER_RENDERED))
        request.removeAttribute(PAGE_HEADER_RENDERED);
    }
  }

  /**
   * Return an OutputStream that wraps the passed OutputStream, or null
   * if there is no specialized writer for this request.
   */
  public OutputStream getOutputStream( OutputStream out, 
                                       RenderRequest request, 
                                       String namespace )
    throws IOException
  {
    boolean doPage = (request.getAttribute(PAGE_HEADER_RENDERED) == null);

    boolean doWindow = _isDecorateWindow == null 
                        ? !doPage
                        : _isDecorateWindow.booleanValue();

    if (doPage) {
      beginPage(out, request, namespace);
      request.setAttribute(PAGE_HEADER_RENDERED, this);
    }

    if (doWindow)
      beginWindow(out, request, namespace);

    return out;
  }
  
  /**
   * Derived classes override to insert header content for a page,
   * called only when beginPage() has not been called on some instance of
   * AbstractRenderer for this request.
   *
   * <code>request.getResponseContentType()</code> can be used
   * to determine the content type.
   *
   * request.getAttribute("javax.portlet.title") may contain a title
   * for the Window, if the portlet has set one. 
   */
  protected void beginPage( OutputStream out, 
                            RenderRequest request, 
                            String namespace )
    throws IOException
  {
  }

  /**
   * Derived classes override to insert header content before a portlet
   * has been rendered.
   *
   * <code>request.getResponseContentType()</code> is used
   * to determine the content type.
   *
   * request.getAttribute("javax.portlet.title") may contain a title
   * for the Window, if the portlet has set one. 
   */
  protected void beginWindow( OutputStream out, 
                              RenderRequest request, 
                              String namespace )
    throws IOException
  {
  }

  /**
   * Derived classes override to append footer content after a portlet
   * has been rendered.
   *
   * <code>request.getResponseContentType()</code> can be used
   * to determine the content type.
   *
   */
  protected void endWindow( OutputStream out, 
                            RenderRequest request, 
                            String namespace )
    throws IOException
  {
  }

  /**
   * Derived classes override to insert footer content for a page,
   * called from  only when beginPage() has been called for the Window.
   *
   * <code>request.getResponseContentType()</code> can be used
   * to determine the content type.
   */
  protected void endPage( OutputStream out, 
                          RenderRequest request, 
                          String namespace )
    throws IOException
  {
  }

  /**
   * Finish with an OutputStream produced by this factory.
   * This may be called even if the outputStream threw an Exception.
   *
   * This implementation calls endWindow() and endPage() if needed.
   */
  public void finish( OutputStream out,
                      RenderRequest request,
                      String namespace,
                      boolean isDiscarded )
    throws IOException
  {
    boolean doPage = (this == request.getAttribute(PAGE_HEADER_RENDERED));

    boolean doWindow = _isDecorateWindow == null 
                        ? !doPage
                        : _isDecorateWindow.booleanValue();

    if (!isDiscarded) {
      if (doWindow)
        endWindow(out, request, namespace);

      if (doPage)
        endPage(out, request, namespace);
    }
    else {
      if (this == request.getAttribute(PAGE_HEADER_RENDERED))
        request.removeAttribute(PAGE_HEADER_RENDERED);
    }
  }

  /**
   * Return the content type to use for the response.
   */
  protected String getContentType(RenderRequest request)
  {
    return request.getResponseContentType();
  }

  /**
   * Return the locale to use for the response.
   */
  protected Locale getLocale(RenderRequest request)
  {
    return request.getLocale();
  }

  /**
   * Return the title for the window, or null if it has not been set.
   * The title is the first of:
   * <ul>
   * <li>A title set by the portlet using response.setTitle()
   *     or request.setAttribute("javax.portlet.title");
   * <li>The value of a lookup in the windows resource bundle for
   *     "javax.portlet.title" using the locale dertmined by
   *     {@link #getLocale(javax.portlet.RenderRequest) getLocale(request)}.
   * </ul>
   */
  protected String getTitle(RenderRequest request)
  {
   String title = (String) request.getAttribute("javax.portlet.title");

   if (title == null) {
     PortletConfig portletConfig
       = (PortletConfig) request.getAttribute("javax.portlet.portletConfig");

     Locale locale = getLocale(request);
     ResourceBundle bundle = portletConfig.getResourceBundle(locale);

     if (bundle != null)
       title = bundle.getString("javax.portlet.title");
   }

   return title;
  }

  /**
   * Return the short title for the window, or null if a title is not
   * available.
   *
   * <ul>
   * <li>A title set by the portlet using 
   *     request.setAttribute("javax.portlet.short-title");
   * <li>A title set by the portlet using response.setTitle()
   *     or request.setAttribute("javax.portlet.title");
   * <li>The value of a lookup in the windows resource bundle for
   *     "javax.portlet.short-title" using the locale determined by
   *     {@link #getLocale(javax.portlet.RenderRequest) getLocale(request)}.
   * <li>The value of a lookup in the windows resource bundle for
   *     "javax.portlet.title" using the locale determined by
   *     {@link #getLocale(javax.portlet.RenderRequest) getLocale(request)}.
   * </ul>
   */
  protected String getShortTitle( RenderRequest request )
  {
    String title = (String) request.getAttribute("javax.portlet.short-title");

    if (title == null)
      title = (String) request.getAttribute("javax.portlet.title");

    if (title == null) {
      PortletConfig portletConfig
        = (PortletConfig) request.getAttribute("javax.portlet.portletConfig");

      Locale locale = getLocale(request);
      ResourceBundle bundle = portletConfig.getResourceBundle(locale);

      title = bundle.getString("javax.portlet.short-title");

      if (title == null)
        title = bundle.getString("javax.portlet.title");
    }

    return title;
  }

  /**
   * Create a PortletURL for a control, such as a link to change the window
   * state or portlet mode.  Control URLs are render urls that maintain
   * existing render parameters.
   */
  protected PortletURL createControlURL(RenderRequest request)
  {
    RenderResponse response 
      = (RenderResponse) request.getAttribute("javax.portlet.renderResponse");

    Map<String, String[]> renderParamMap = request.getParameterMap();

    PortletURL url = response.createRenderURL();

    url.setParameters(renderParamMap);

    return url;
  }

  /**
   * Return an Set of the possible portlet modes that the window can
   * have.  The list of all possibilities is first obtained from 
   * {@link javax.portlet.PortalContext#getSupportedPortletModesa() PortalContext.getSupportedPortletModes()}. 
   * Each one is checked with 
   * {@link javax.portlet.PortletRequest#isPortletModeAllowed(javax.portlet.PortletMode) request.isPortletModeAllowed()},
   * if it is allowed then it is included in the returned Set.
   */
  protected Set<PortletMode> getPortletModes( RenderRequest request )
  {
    Set<PortletMode> portletModes = new LinkedHashSet<PortletMode>();

    Enumeration<PortletMode> e = (Enumeration<PortletMode>) 
      request.getPortalContext().getSupportedPortletModes();

    while (e.hasMoreElements())  {
      PortletMode portletMode = e.nextElement();

      if (request.isPortletModeAllowed(portletMode))
        portletModes.add(portletMode);
    }

    return portletModes;
  }

  /**
   * Return an Set of the possible window states that the window can
   * have.  The list of all possibilities is first obtained from 
   * {@link javax.portlet.PortalContext#getSupportedWindowStates() PortalContext.getSupportedWindowStates()}. 
   * Each one is checked with 
   * {@link javax.portlet.PortletRequest#isWindowStateAllowed(javax.portlet.WindowState) request.isWindowStateAllowed()},
   * if it is allowed then it is included in the returned Set.
   */
  protected Set<WindowState> getWindowStates( RenderRequest request )
  {
    Set<WindowState> windowStates = new LinkedHashSet<WindowState>();

    Enumeration<WindowState> e = (Enumeration<WindowState>) 
      request.getPortalContext().getSupportedWindowStates();

    while (e.hasMoreElements())  {
      WindowState windowState = e.nextElement();

      if (request.isWindowStateAllowed(windowState))
        windowStates.add(windowState);
    }

    return windowStates;
  }

  /**
   * Get a resource bundle for the portlet being rendered.
   */
  protected ResourceBundle getResourceBundle( RenderRequest request )
  {
    Locale locale = getLocale( request );
    PortletConfig portletConfig = getPortletConfig( request );
    return portletConfig.getResourceBundle( locale );
  }

 /**
   * Get the PortletConfig for the portlet being rendered.
   */
  protected PortletConfig getPortletConfig(RenderRequest request)
  {
    return (PortletConfig) request.getAttribute("javax.portlet.portletConfig");
  }

  /**
   * Get the RenderResponse for the portlet being rendered.
   */
  protected RenderResponse getRenderResponse(RenderRequest request)
  {
    return (RenderResponse) request.getAttribute("javax.portlet.renderResponse");
  }

  /**
   * Print a string with appropriate escaping for XML, for example '&lt;'
   * becomes '&amp;lt;'.
   */
  protected PrintWriter printEscaped( PrintWriter out, CharSequence string )
  {
    if ( string == null ) {
      out.print( (String) null );
      return out;
    }

    for ( int i = 0; i < string.length(); i++  ) {
      char ch = string.charAt( i );

      switch ( ch ) {
        case '<': out.print( "&lt;" ); break;
        case '>': out.print( "&gt;" ); break;
        case '&': out.print( "&amp;" ); break;
        case '\"': out.print( "&quot;" ); break;
        case '\'': out.print( "&rsquo;" ); break;
        default: out.print( ch );
      }
    }

    return out;
  }

}

