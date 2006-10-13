package test;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * A simple servlet that uses the resource.
 */
public class TestServlet extends HttpServlet {
  private static final Logger log =
    Logger.getLogger(TestServlet.class.getName());

  /**
   * The saved resource from JNDI.
   */
  private TestResource _resource;

  /**
   * The bean-setter allows for bean-style init of the servlet.
   */
  public void setResource(TestResource resource)
  {
    _resource = resource;
  }
  
  /**
   * The init method looks up the resource in JNDI and saves it in a
   * local field.  JNDI is somewhat slow, so it's more efficient
   * to save the resource.
   */
  public void init()
    throws ServletException
  {
    if (_resource == null) {
      try {
	Context ic = new InitialContext();

	_resource = (TestResource) ic.lookup("java:comp/env/test/basic");
      } catch (NamingException e) {
	throw new ServletException(e);
      }
    }
      
    log.info("TestServlet init: resource=" + _resource);
  }
  
  /**
   * The doGet method just prints out the resource.
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    PrintWriter out = res.getWriter();

    out.println("Resource: " + _resource);
  }
}
