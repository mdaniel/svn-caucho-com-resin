package example.cmp.find;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.naming.*;
import javax.ejb.*;

import java.util.*;

import java.io.PrintWriter;

/**
 * A client to illustrate Course bean's finder methods.  The example uses
 * three finder methods from the CourseHome interface:
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
   * The course local home interface.  It is set in the servlet's
   * init() method.
   */
  private CourseHome _home = null;

  /**
   * Sets the course home as a bean setter.
   */
  public void setCourseHome(CourseHome home)
  {
    _home = home;
  }

  /**
   * Finds the course's home interface using JNDI.  Since the home interface
   * never changes, looking it up in the <code>init()</code> interface
   * avoids the relatively slow JNDI call for each request.
   *
   * <p>The course bean is located at java:comp/env/cmp/finder_CourseBean.
   *
   * @exception ServletException if the JNDI lookup fails.
   */
  public void init()
    throws ServletException
  {
    if (_home == null) {
      try {
        Context ic = new InitialContext();
        
        // get the house stub
        _home = (CourseHome) ic.lookup("java:comp/env/cmp/find_CourseBean");
      } catch (NamingException e) {
        throw new ServletException(e);
      }
    }
  }

  /**
   *
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    out.println("<h3>Find by Primary Key</h3>");

    // We want to find the course "Potions"
    String courseName = "Potions";

    try {
      // findByPrimaryKey() is a finder method that must be present in
      // every CMP bean's home interface. It returns the entity that
      // corresponds to the primary key passed. We have configured
      // courseId to be the primary key in cmp-find.ejb

      Course course = _home.findByPrimaryKey(courseName);

      out.println(course.getCourseId() + " is taught by " +
                  course.getInstructor());
    } catch (ObjectNotFoundException e) {
      out.println("There is no course '" + courseName + "'");
    } catch (FinderException e) {
      throw new ServletException(e);
    }

    out.println("<h3>Find All Courses</h3>");

    try {
      // We have defined a finder method in cmp-find.ejb that returns
      // all courses. Resin-CMP has implemented this method for us.

      Collection c = _home.findAll();
      Iterator iter = c.iterator();

      // Because findByInstructor returns a Collection, we will not catch
      // an ObjectNotFoundException if there are no courses.
      // Instead we will receive an empty Collection.

      if (! iter.hasNext())
        out.println("No classes are being taught!");

      // display all courses
      while (iter.hasNext()) {
        Course course = (Course) iter.next();

        out.println(course.getCourseId() + " is taught by " +
                    course.getInstructor() + "<br>");
      }
    } catch (ObjectNotFoundException e) {
      throw new ServletException(e);
    } catch (FinderException e) {
      throw new ServletException(e);
    }

    out.println("<h3>Find the course taught by an instructor</h3>");

    String teacher = "Remus Lupin";
    try {
      // We have defined a finder method in cmp-find.ejb that returns
      // the course taught by a teacher.  Resin-CMP has implemented
      // this method for us.

      Course course = _home.findByInstructor(teacher);

      // Because findByInstructor returns a single Course, it will
      // throw an ObjectNotFoundException if the teacher has no course.

      // display the course
      out.println(course.getCourseId() + " is taught by " +
                  course.getInstructor() + "<br>");
    } catch (ObjectNotFoundException e) {
      out.println(teacher + " is not teaching any courses.");
    } catch (FinderException e) {
      throw new ServletException(e);
    }
  }
}
