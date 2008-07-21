package example.cmp.one2one;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.ejb.*;

import javax.naming.*;

import java.util.Collection;
import java.util.Iterator;

import java.io.PrintWriter;

/**
 * Illustrates a client that makes use of the 1-1 related CMP beans.
 */
public class ClientServlet extends HttpServlet {
  private TeacherHome _teacherHome = null;
  private CourseHome _courseHome = null;

  /**
   * Sets the TeacherHome.
   */
  public void setTeacherHome(TeacherHome teacherHome)
  {
    _teacherHome = teacherHome;
  }

  /**
   * Sets the CourseHome.
   */
  public void setCourseHome(CourseHome courseHome)
  {
    _courseHome = courseHome;
  }

  /**
   * Initialize the servlet.
   */
  public void init()
    throws ServletException
  {
    try {
      Context ic = new InitialContext();
      
      Context cmp = (Context) ic.lookup("java:comp/env/cmp");

      // get the Teacher CMP-Bean home interface
      if (_teacherHome == null)
        _teacherHome = (TeacherHome) cmp.lookup("one2one_teachers");

      // get the Course CMP-Bean home interface
      if (_courseHome == null)
        _courseHome = (CourseHome) cmp.lookup("one2one_courses");
    } catch (NamingException e) {
      throw new ServletException(e);
    }
  }
  /**
   *
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    res.setContentType("text/html");

    PrintWriter out = res.getWriter();

    try {
      Iterator iter = _courseHome.findAll().iterator();

      out.println("<H3>Getting Teacher-Course Assignments</H3>");

      while (iter.hasNext()) {
        Course course = (Course) iter.next();
        out.println(course.getTeacher().getName() + " teaches " +
                    course.getName() + "<br>");
      }
    } catch (FinderException fe) {
      throw new ServletException(fe);
    }
  }
}
