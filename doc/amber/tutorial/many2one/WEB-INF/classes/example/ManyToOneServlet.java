package example;

import java.util.*;
import java.io.PrintWriter;

import java.sql.ResultSet;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.amber.AmberFactory;
import com.caucho.amber.AmberConnection;

/**
 * The ManyToOne servlets gets information about students and their houses.
 */
public class ManyToOneServlet extends HttpServlet {
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

      Student student = (Student) aConn.load(Student.class, 1);

      out.println("<h3>Single Student</h3>");

      out.println(student.getName() + ", " + student.getHouse().getName());

      out.println("<h3>All Students</h3>");

      String sql = ("SELECT o.name, o.house.name" +
		    " FROM example.Student AS o" +
		    " ORDER BY o.house.name, o.name");

      out.println("<table border='0'>");
      ResultSet rs = aConn.query(sql);
      while (rs.next()) {
	out.print("<tr><td>" + rs.getString(1) + ",<td>" + rs.getString(2));
      }
      out.println("</table>");
    }
    catch (Exception e) {
      throw new ServletException(e);
    }
    finally {
      aConn.close();
    }
  }
}
