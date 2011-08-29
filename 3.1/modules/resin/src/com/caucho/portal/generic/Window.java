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

import javax.portlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A Window represents the use of a portlet on a page.
 * The Window interface is the main point of contact between a portal
 * implementation and the generic portlet codebase.
 */ 
public interface Window
{
  public PortletConfig getPortletConfig();

  public Renderer getRenderer();

  /**
   * Time in seconds, 0 disables caching and -1 means never expire.
   */ 
  public int getExpirationCache();

  /**
   * Return true if the contents of the portlet are private.
   *
   * The portlet specification states that "cached content must not be shared
   * across different user clients displaying the same portlet".
   *
   * If a portal implementation supports caching,  then a return value of false
   * will allow the portal to share the cached content across different user
   * clients.
   */ 
  public boolean isPrivate();

  /**
   * Return the default preferences for the portlet, null if there are no
   * default preferences.
   */
  public PortletPreferences getDefaultPreferences();

  /**
   * Return the preferences validators for the portlet, null if there are no
   * preference validators.
   */
  public ArrayList<PreferencesValidator> getPreferencesValidators();

  /**
   * Return a map that map's role names used by the portlet to the role names
   * recognized by the portal, null if the role names used by the portlet
   * are to be used unchanged.
   */
  public Map<String, String> getRoleRefMap();

   /**
    * Return a list of {@link Constraint} that are applied before the
    * processAction() and render() of the portlet are called, null if there
    * are no Constraints.
    */ 
  public ArrayList<Constraint> getConstraints();

  /**
   * Return the content types supported for the mode, null if all content
   * types are permitted.
   */
  public Set<String> getSupportedContentTypes(PortletMode portletMode);

  /**
   * Return the Locales supported, null if all Locales are permitted.
   */
  public Set<Locale> getSupportedLocales();

  /**
   * Return a preferred buffer size, may be overridden by the portlet 
   * if it calls setBufferSize().  
   * A value of 0 disables buffering for the portlet. 
   * A value of -1 allows the portal to use a default buffer size.
   */
   public int getBufferSize();

  /**
   * Return true if the WindowState is allowed.
   * <code>portletRequest.getResponseContentType()</code> can be used
   * if the allowed portlet modes depends on the mime type of the
   * response.
   */
  public boolean isWindowStateAllowed(PortletRequest request,
                                      WindowState windowState);

  /**
   * Return true if the PortletMode is allowed.
   * <code>portletRequest.getResponseContentType()</code> can be used
   * if the allowed portlet modes depends on the mime type of the
   * response.
   */
  public boolean isPortletModeAllowed(PortletRequest request,
                                      PortletMode portletMode);

  /**
   * This is called when a request arrives requesting a PortletMode
   * that does not pass the isPortletModeAllowed() tests.
   * It gives the Window an opportunity to return a PortletMode
   * that can be used.  If the returned mode is also not allowed, a
   * PortletModeException occurs.
   *
   * Implementations can use {@link handleException} to handle
   * PortletModeException. 
   *
   * @return a new PortletMode to try, or null to cause an exception.
   */
  public PortletMode handlePortletModeFailure( PortletRequest request, 
                                               PortletMode notAllowed );

  /**
   * This is called when a request arrives requesting a WindowState
   * that does not pass the isWindowStateAllowed() tests.
   * It gives the Window an opportunity to return a WindowState
   * that can be used.  If the returned window state is also not allowed, a
   * WindowStateException occurs.
   *
   * Implementations can use {@link handleException} to handle
   * WindowStateException. 
   *
   * @return a new WindowState to try, or null to cause an exception,
   */
  public WindowState handleWindowStateFailure( PortletRequest request, 
                                               WindowState notAllowed );

  /**
   * Optionally handle a constraint failure by sending some output to the
   * client using the response or by hiding the window.
   *
   * When a Portlet fails a Constriant during processAction(), it is stored
   * until the render stage, and handleConstriantFailure() is called instead of
   * render().
   *
   * When a Portlet fails a Constraint during render(), 
   * handleConstriantFailure() is immediately called.
   *
   * If the implementation of this method does nothing, then the response of 
   * this window is reset() and the failure propagates to the parent window.
   * If there is no parent window, 
   * {@link PortletConnection#handleConstraintFailure()} is called.
   *
   * Implementations of this method can change that behaviour by using the
   * <code>event.setHandled(boolean hideWindow)</code> callback method. 
   *
   * See {@link #handleException} for more information.
   */
  public void handleConstraintFailure( RenderRequest request, 
                                       RenderResponse response, 
                                       ConstraintFailureEvent event)
    throws IOException, PortletException;


  /**
   * Optionally handle an exception by sending some output to the client
   * using the response or by hiding the window.
   *
   * When a Portlet throws an Exception during processAction(), it is stored
   * until the render stage, and handleException() is called instead of
   * render().
   *
   * When a Portlet throws an exception during render(), the handleException()
   * is immediately called.
   *
   * If the implementation of this method does nothing, then the repsonse of 
   * this window is reset() and the exception propagates to the parent window.
   * If there is no parent window, {@link PortletConnection#handleException()}
   * is called.
   *
   * Implementations of this method can change that behaviour by using the
   * <code>event.setHandled(boolean hideWindow)</code> callback method. 
   *
   * <h3>exceptionEvent.setHandled(false)</h3>
   *
   * If handleException calls exceptionEvent.setHandled(false) then
   * the content that the portlet has written is kept, and the request
   * continues.  Implementations use this when they have written some
   * contents to the portlet that should be visible to the user.
   *
   * <pre>
   * public void handleException(RenderRequest request, RenderResponse response, ExceptionEvent event)
   *   throws PortletException, IOException 
   * {
   *   if (event.getException() instanceof UnavailableException) {
   *     PrintWriter out = response.getWriter();
   *     response.setContentType("text/html"); 
   *     out.println("This service is currently unavailable."); 
   *     event.setHandled(false);
   *   } 
   * } 
   * </pre>
   *
   * If an implementation is handling an exception that can occur after the
   * Portlet has written some content already, it should call response.reset()
   * before it writes anything to the response.
   *
   * <pre>
   * public void handleException(RenderRequest request, RenderResponse response, ExceptionEvent event)
   *   throws PortletException, IOException 
   * {
   *   response.reset(); 
   *   response.setContentType("text/html"); 
   *   PrintWriter out = response.getWriter();
   *   printFancyExceptionMessage(event.getException());
   *   event.setHandled(false);
   * } 
   * </pre>
   *
   * <h3>exceptionEvent.setHandled(true)</h3>
   *
   * If handleException calls exceptionEvent.setHandled(true) then
   * the output is reset(), including any output from a Renderer, and 
   * the request continues.  The effect of this call is to make the window
   * invisible, no content for this window is shown but other windows on 
   * the page will be shown. Implementations use this when they have dealt
   * with the exception in some way and wish to hide the exception from the
   * user.
   *
   * <pre>
   * public void handleException(ExceptionEvent event)
   *   throws PortletException, IOException 
   * {
   *   log.log(Level.WARNING, ex.toString(), ex);
   *   event.setHandled(true); 
   * } 
   * </pre>
   */
  public void handleException( RenderRequest renderRequest,
                               RenderResponse renderResponse,
                               ExceptionEvent exceptionEvent)
  throws IOException, PortletException;

}

