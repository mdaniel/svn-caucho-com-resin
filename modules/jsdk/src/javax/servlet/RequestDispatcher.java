/*
 * Copyright 1998-2000 Caucho Technology -- all rights reserved
 *
 * Caucho Technology forbids redistribution of any part of this software
 * in any form, including derived works and generated binaries.
 *
 * This Software is provided "AS IS," without a warranty of any kind. 
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.

 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.      
 *
 * $Id: RequestDispatcher.java,v 1.1.1.1 2004/09/11 05:06:12 cvs Exp $
 */

package javax.servlet;

import java.util.*;
import java.io.*;

/**
 * The RequestDispatcher gives servlets the capabilities of SSI includes.
 * The forwarded or included page is handled as a normal page request.
 *
 * <pre><code>
 * RequestDispatcher disp;
 * disp = request.getRequestDispatcher("inc.jsp?a=b");
 * disp.include(request, response);
 * </code></pre>
 *
 * <p>Servlets typically use <code>ServletRequest.setAttribute()</code>
 * to communicate between included pages.
 *
 * <p>A popular architecture uses servlets to process the initial request
 * and JSP files to format the results.  That template architecture uses
 * request attributes to communicate data from the servlet to the JSP page.
 * <code>disp.forward()</code> transfers the request to the JSP file.
 */
public interface RequestDispatcher {
  /**
   * Forwards the request to another page.  Forward may not be called
   * if data has been sent to the client.  Specifically,
   * forward calls the <code>response.reset()</code> method to clear
   * the output buffer.
   *
   * <p>Query parameters are added to the original query parameters.
   *
   * <p>The new URI values are based on the RequestDispatcher URI.
   * So getRequestURI(), getServletPath(), and getPathInfo() will reflect
   * the request dispatcher URI.
   *
   * @param request the original request
   * @param response the original response
   */
  public void forward(ServletRequest request, ServletResponse response)
    throws ServletException, IOException;
  /**
   * Includes the result of another page.
   *
   * <p>Query parameters are added to the original query parameters.
   *
   * <p>The included request's URI methods reflect the <em>original</em>
   * URI data.  So getRequestURI() will return the URI sent by
   * the browser.
   *
   * <p>Included pages should use request.getAttribute() to get the
   * new URI values:
   * <table>
   * <tr><td>getRequestURI<td>javax.servlet.include.request_uri
   * <tr><td>getContextPath<td>javax.servlet.include.context_path
   * <tr><td>getServletPath<td>javax.servlet.include.servlet_path
   * <tr><td>getPathInfo<td>javax.servlet.include.path_info
   * <tr><td>getQueryString<td>javax.servlet.include.query_string
   * </table>
   *
   * @param request the original request
   * @param response the original response
   */
  public void include(ServletRequest request, ServletResponse response)
    throws ServletException, IOException;
}
