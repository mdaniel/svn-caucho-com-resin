/*
 * Copyright 1998-1998 Caucho Technology -- all rights reserved
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
 * $Id: Servlet.java,v 1.1.1.1 2004/09/11 05:06:14 cvs Exp $
 */

package javax.servlet;

import java.util.*;
import java.io.*;

/**
 * A servlet is any Java class with a null-arg constructor that
 * implements the Servlet API.
 *
 * <p>Simple servlets should extend HttpServlet to create servlets.
 *
 * <p>Servlets that need full control should extend GenericServlet.
 *
 * <h4>Location</h4>
 *
 * Servlets are usually loaded from WEB-INF/classes under the application's
 * root.  To add a servlet test.MyServlet, create the java file:
 * <center>/www/myweb/WEB-APP/classes/test/MyServlet.java</center>
 *
 * <p>Servlets can also live in the global classpath.
 *
 * <h4>Configuration</h4>
 *
 * Servlet configuration for Resin is in the resin.conf file.
 *
 * <pre><code>
 * &lt;servlet servlet-name='hello'
 *          servlet-class='test.HelloServlet'
 *          load-on-startup>
 *   &lt;init-param param1='value1'/>
 *   &lt;init-param param2='value2'/>
 * &lt;/servlet>
 * </code></pre>
 *
 * <h4>Dispatch</h4>
 *
 * The servlet engine selects servlets based on the
 * <code>servlet-mapping</code> configuration.  Servlets can use the
 * special 'invoker' servlet or they can be configured to execute directly.
 *
 * <p>To get a path info, your servlet needs to use a wildcard.  In the
 * following example, /Hello will match the 'hello' servlet, but
 * /Hello/there will match the 'defaultServlet' servlet with a pathinfo
 * of /Hello/there.
 *
 * <pre><code>
 * &lt;servlet-mapping url-pattern='/'
 *                  servlet-name='defaultServlet'/>
 *
 * &lt;servlet-mapping url-pattern='/Hello'
 *                  servlet-name='hello'/>
 *
 * &lt;servlet-mapping url-pattern='/servlet/*'
 *                  servlet-name='invoker'/>
 *
 * &lt;servlet-mapping url-pattern='*.jsp'
 *                  servlet-name='com.caucho.jsp.JspServlet'/>
 * </code></pre>
 *
 * <h4>Life cycle</h4>
 *
 * Servlets are normally initialized when they are first loaded.  You can
 * force loading on startup using the 'load-on-startup' attribute to the
 * servlet configuration.  This is a useful technique for the equivalent
 * of the global.jsa file.
 *
 * <p>A servlet can count on having only one instance per
 * application (JVM) unless it implements SingleThreadedModel.
 *
 * <p>Servlet requests are handed by the <code>service</code> routine.
 * Since the servlet engine is multithreaded, multiple threads may call
 * <code>service</code> simultaneously.
 *
 * <p>When the application closes, the servlet engine will call
 * <code>destroy</code>.  Note, applications always close and are restarted
 * whenever a servlet changes.  So <code>init</code> and <code>destroy</code>
 * may be called many times while the server is still up.
 */
public interface Servlet {
  /**
   * Returns an information string about the servlet.
   */
  public String getServletInfo();
  /**
   * Initialize the servlet.  ServletConfig contains servlet parameters
   * from the configuration file.  GenericServlet will store the config
   * for later use.
   *
   * @param config information from the configuration file.
   */
  public void init(ServletConfig config) throws ServletException;
  
  /**
   * Returns the servlet configuration, usually the same value as passed
   * to the init routine.
   */
  public ServletConfig getServletConfig();
  
  /**
   * Service a request.  Since the servlet engine is multithreaded, 
   * many threads may execute <code>service</code> simultaneously.  Normally,
   * <code>req</code> and <code>res</code> will actually be
   * <code>HttpServletRequest</code> and <code>HttpServletResponse</code>
   * classes.
   *
   * @param req request information.  Normally servlets will cast this
   * to <code>HttpServletRequest</code>
   * @param res response information.  Normally servlets will cast this
   * to <code>HttpServletRequest</code>
   */
  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException;
  
  /**
   * Called when the servlet shuts down.  Servlets can use this to close
   * database connections, etc.  Servlets generally only shutdown when
   * the application closes.
   */
  public void destroy();
}
