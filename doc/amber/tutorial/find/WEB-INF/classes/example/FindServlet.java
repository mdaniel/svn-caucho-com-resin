package example;

import java.util.List;

import java.io.PrintWriter;

import java.sql.ResultSet;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.amber.AmberFactory;
import com.caucho.amber.AmberConnection;
import com.caucho.amber.AmberQuery;

/**
 * A client to illustrate Amber finder methods.
 * <ul>
 * <li>findByPrimaryKey - the standard find method available in every bean.
 * <li>findAll - a collection finder returning all the courses.
 * <li>findByInstructor - a single-bean finder returning the course taught
 *   by an instructor.
 * </ul>
 *
 *
 */
public class FindServlet extends HttpServlet {
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
   * Handle the request.
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    out.println("<h3>Find by Primary Key</h3>");

    AmberConnection aConn = null;

    // We want to find the course "Potions"
    String courseName = "Potions";

    try {
      aConn = _factory.getConnection();

      // The main finder is the <code>load</code> method of
      // AmberConnection, loading a single entity by its primary key.
      //
      // In some applications, the primary key is available from
      // a query string, like an article id.

      Course course = (Course) aConn.load(Course.class, 1);

      if (course != null) {
	out.println(course.getName() + " is taught by " +
		    course.getTeacher());
      }
      else
	out.println("There is no course '" + courseName + "'");

      out.println("<h3>Find All Courses</h3>");

      // Many queries use the <code>find</code> convenience method
      // of AmberConnection.  The entitites are preloaded, i.e.
      // only a single database query is needed for the load.
      //
      ResultSet rs = aConn.query("SELECT course FROM example.Course course");

      while (rs.next()) {
        course = (Course) rs.getObject(1);

        out.println(course.getName() + " is taught by " +
                    course.getTeacher() + "<br>");
      }

      out.println("<h3>Find the course taught by an instructor</h3>");

      String teacher = "Remus Lupin";

      // Here we prepare a query, similar to the preparedStatement call of
      // JDBC.

      String hSQL = ("SELECT course " +
		     "FROM example.Course AS course " +
		     "WHERE course.teacher=?");
      
      AmberQuery query = aConn.prepareQuery(hSQL);

      query.setString(1, teacher);

      // The query returns a JDBC result set for retrieving results.

      rs = query.executeQuery();

      if (rs.next()) {
	course = (Course) rs.getObject(1);
	
	out.println(course.getName() + " is taught by " +
		    course.getTeacher() + "<br>");
      }
      else
	out.println(teacher + " is not teaching any courses.");

      rs.close();
    } catch (Exception e) {
      throw new ServletException(e);
    } finally {
      aConn.close();
    }
  }
}
