package example;

import java.sql.ResultSet;

import javax.servlet.*;
import javax.servlet.http.*;

import java.util.*;
import java.io.PrintWriter;

import com.caucho.amber.AmberFactory;
import com.caucho.amber.AmberConnection;

/**
 * The Create servlet shows creating and removing of entities.
 */
public class CreateServlet extends HttpServlet {
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
      
      ResultSet rs = aConn.query("FROM example.Course");

      while (rs.next()) {
        Course course = (Course) rs.getObject(1);

        out.println("#" + course.getId() + " " + course.getName() +
		    " is taught by " + course.getTeacher() + "<br>");
      }

      //
      // Create new two Courses.
      //
      out.println("<h3>Adding some classes</h3>");

      // Creating a course is just creating a new object.
      // At first, the course is not connected to Amber.
      Course divination = new Course();
      divination.setName("Divination");
      divination.setTeacher("Sybil Trelawney");

      // Now add the course to the database and connect the Course
      // to Amber
      aConn.create(divination);
      
      Course creatures = new Course();
      creatures.setName("Care of Magical Creatures");
      creatures.setTeacher("Rubeus Hagrid");

      aConn.create(creatures);
      
      rs = aConn.query("FROM example.Course");

      while (rs.next()) {
        Course course = (Course) rs.getObject(1);

        out.println("#" + course.getId() + " " + course.getName() +
		    " is taught by " + course.getTeacher() + "<br>");
      }

      out.println("<h3>Removing the new classes</h3>");

      aConn.delete(divination);
      aConn.delete(creatures);
      
      rs = aConn.query("FROM example.Course");

      while (rs.next()) {
        Course course = (Course) rs.getObject(1);

        out.println("#" + course.getId() + " " + course.getName() +
		    " is taught by " + course.getTeacher() + "<br>");
      }
    } catch (Exception e) {
      throw new ServletException(e);
    } finally {
      aConn.close();
    }
  }
}
