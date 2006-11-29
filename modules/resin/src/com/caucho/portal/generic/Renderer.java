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
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.WindowState;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Classes can implement {@link AbstractRenderer} to protect from API changes. 
 */
public interface Renderer {
  /**
   * Return true if the WindowState is allowed.
   * <code>portletRequest.getResponseContentType()</code> can be used
   * if the allowed portlet modes depends on the mime type of the
   * response.
   */
  public boolean isWindowStateAllowed( PortletRequest request,
                                       WindowState windowState );

  /**
   * Return true if the PortletMode is allowed.
   * <code>portletRequest.getResponseContentType()</code> can be used
   * if the allowed portlet modes depends on the mime type of the
   * response.
   */
  public boolean isPortletModeAllowed( PortletRequest request,
                                       PortletMode portletMode );

  /**
   * If true the portal will always call getWriter(), even if the portlet does
   * not call getWriter(), unless getOutputStream() has been called.
   */ 
  public boolean isAlwaysWrite();

  /**
   * If true the portal will always call getOutputStream(), even if the portlet
   * does not call getOutputStream(), unless getWriter() has been called.
   */ 
  public boolean isAlwaysStream();

  /**
   * If {@link #isAlwaysWrite()} or {@link #isAlwaysStream()} is true, then a
   * Writer or OutputStream might be obtained before the content type of the
   * response has been set.  IF that is the case, then this method is called to
   * detrmine a default content type.
   */
  public String getDefaultContentType();

  public int getBufferSize();

  /**
   * Return a Writer that wraps the passed PrintWriter, or null
   * if there is no specialized writer for this request.
   *
   * <code>renderRequest.getResponseContentType()</code> can be used
   * if the Renderer needs to know the content type.
   *
   * renderRequest.getAttribute("javax.portlet.title") may contain a title
   * for the Window, if the portlet has set one. 
   */
  public PrintWriter getWriter( PrintWriter out, 
                                RenderRequest request, 
                                String namespace )
    throws IOException;
  
  /**
   * Finish with a Writer produced by this factory.
   * This may be called even if the writer threw an Exception.
   *
   * @param isDiscarded true if the portal discarded the output.
   * Output is discarded when the portal wishes to discard any output
   * that has been made for the window, with the intention that the
   * window should not be rendered at all.
   */
  public void finish( PrintWriter writer,
                      RenderRequest request, 
                      String namespace,
                      boolean isDiscarded )
    throws IOException;

  /**
   * Return an OutputStream that wraps the passed OutputStream, or null
   * if there is no specialized writer for this request.
   *
   * <code>renderRequest.getResponseContentType()</code> can be used
   * if the Renderer needs to know the content type.
   *
   * renderRequest.getAttribute("javax.portlet.title") may contain a title
   * for the Window, if the portlet has set one. 
   */
  public OutputStream getOutputStream( OutputStream out, 
                                       RenderRequest renderRequest, 
                                       String namespace )
    throws IOException;
  
  /**
   * Finish with an OutputStream produced by this factory.
   * This may be called even if the outputStream threw an Exception.
   *
   * @param discarded true if the portal discarded the output, may occur
   * Output is discarded when the portal wishes to discard any output
   * that has been made for the window, with the intention that the
   * window should not be rendered at all.
   */
  public void finish( OutputStream outputStream,
                      RenderRequest renderRequest, 
                      String namespace,
                      boolean isDiscarded )
    throws IOException;
}

