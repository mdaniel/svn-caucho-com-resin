package example;

import javax.servlet.*;
import javax.servlet.http.*;

import java.util.*;
import java.io.PrintWriter;

import com.caucho.amber.AmberFactory;
import com.caucho.amber.AmberConnection;

/**
 * The basic CourseClient shows the basic flow of any Resin-Persist client.
 */
public class CourseServlet extends HttpServlet {
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
    //
    // Each entity is mapped to one database row that is identified by
    // its primary key.
    //
    Course []course = new Course[2];

    try {
      aConn = _factory.getConnection();
      Object v = aConn.load(Course.class, 1);

      course[0] = (Course) v;
      course[1] = (Course) aConn.load(Course.class, 2);

      out.println("<h3>Course Details</h3>");

      for (int i = 0; i < course.length; i++) {
        out.println("course: " + course[i].getName() + "<br>");
        out.println("teacher: " + course[i].getTeacher() + "<br>");
        out.println("<br>");
      }

      out.println();
      out.println("<p>Swap the instructors for the two courses." );
      out.println();

      // updates which change multiple fields must be in a transaction
      aConn.beginTransaction();

      try {
        String temp = course[0].getTeacher();
        course[0].setTeacher(course[1].getTeacher());
        course[1].setTeacher(temp);
      } finally {
        aConn.commit();
      }
    
      out.println("<h3>New Course Details:</h3>");
      for (int i = 0; i < course.length; i++) {
        out.println("course: " + course[i].getName() + "<br>");
        out.println("teacher: " + course[i].getTeacher() + "<br>");
        out.println("<br>");
      }

      // Switch the instructors back.
      aConn.beginTransaction();

      try {
	String temp = course[0].getTeacher();
	course[0].setTeacher(course[1].getTeacher());
	course[1].setTeacher(temp);
      } finally {
	aConn.commit();
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
