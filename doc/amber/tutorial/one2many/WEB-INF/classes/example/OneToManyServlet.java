package example;

import java.util.*;
import java.io.PrintWriter;

import java.sql.ResultSet;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.amber.AmberFactory;
import com.caucho.amber.AmberConnection;

/**
 * The OneToMany servlets gets information about students and their houses.
 */
public class OneToManyServlet extends HttpServlet {
  /**
   * Cached reference to the Amber connection factory interface.
   * Because this object never changes, the client can look it up once
   * in the <code>init()</code> method and avoid JNDI calls for each request.
   */
  private AmberFactory _factory = null;

  /**
   * Sets the amber factory.  The servlet configuration will set
   * this in the &lt;init> method.
   */
  public void setAmberFactory(AmberFactory factory)
  {
    _factory = factory;
  }

  /**
   * Illustrates how to interact with the Course EJB
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    AmberConnection aConn = null;

    try {
      aConn = _factory.getConnection();

      House house = (House) aConn.load(House.class, 1);

      out.println("<h3>" + house.getName() + " House</h3>");

      Collection students = house.getStudents();
      Iterator iter = students.iterator();
      while (iter.hasNext()) {
	Student student = (Student) iter.next();

	out.println(student.getName() + "<br>");
      }
    }
    catch (Exception e) {
      throw new ServletException(e);
    }
    finally {
      aConn.close();
    }
  }
}
