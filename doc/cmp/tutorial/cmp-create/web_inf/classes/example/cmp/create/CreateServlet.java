package example.cmp.create;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.naming.*;
import javax.ejb.*;

import java.util.*;
import java.io.PrintWriter;

/**
 * A client to illustrate the services of the CourseBean CMP bean.
 */
public class CreateServlet extends HttpServlet {

  private CourseHome _home = null;

  /**
   * Bean setter to configure the course home.
   */
  public void setCourseHome(CourseHome home)
  {
    _home = home;
  }

  /**
   * gets a reference to the CourseBean home interface
   */
  public void init()
    throws ServletException
  {
    if (_home == null) {
      try {
        Context ic = new InitialContext();

        // get the house stub
        _home = (CourseHome) ic.lookup("java:comp/env/cmp/create_CourseBean");
      } catch (NamingException e) {
        throw new ServletException(e);
      }
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    out.println("<h3>Original classes</h3>");

    try {
      Collection c = _home.findAll();
      Iterator iter = c.iterator();

      while (iter.hasNext()) {
        Course course = (Course) iter.next();

        out.println(course.getCourseId() + " is taught by " +
                    course.getInstructor() + "<br>");
      }
    } catch (FinderException e) {
      throw new ServletException(e);
    }

    //
    // Use the home interface to create 3 new Course entities.
    //
    // Because we employ CMP, each of the Courses will be automatically
    // stored to a database table as we defined in cmp-create.ejb
    //
    Course divination = null;
    Course creatures = null;

    try {
      out.println("<h3>Adding some classes</h3>");

      //
      // We can use any one of the create(...) methods defined in the local
      // interface, Course.java. Each create(...) method needs matching
      // ejbCreate(...) and ejbPostCreate(...) methods defined in the
      // implementation class, CourseBean.java, with the same
      // parameters.
      //
      // create(...) returns a new persistant entity, which means it also
      // creates a new database column for the entity.

      divination = _home.create("Divination", "Sybil Trelawney");
      creatures = _home.create("Care of Magical Creatures", "Rubeus Hagrid");

      Collection c = _home.findAll();
      Iterator iter = c.iterator();

      while (iter.hasNext()) {
        Course course = (Course) iter.next();

        out.println(course.getCourseId() + " is taught by " +
                    course.getInstructor() + "<br>");
      }
    }
    catch (Exception e) {
      throw new ServletException(e);
    }

    out.println("<h3>Removing the new classes</h3>");

    try {
      if (divination != null)
        divination.remove();
      if (creatures != null)
        creatures.remove();

      Collection c = _home.findAll();
      Iterator iter = c.iterator();

      while (iter.hasNext()) {
        Course course = (Course) iter.next();

        out.println(course.getCourseId() + " is taught by " +
                    course.getInstructor() + "<br>");
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
}
