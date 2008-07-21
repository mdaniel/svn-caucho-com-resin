package example.cmp.many2many;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.ejb.*;

import javax.naming.*;

import java.util.Collection;
import java.util.Iterator;

import java.io.PrintWriter;

/**
 * A client to illustrate EJB managed relations in a many-to-many context.
 */
public class FindServlet extends HttpServlet {
  StudentHome studentHome;
  CourseHome courseHome;

  public void init()
    throws ServletException
  {
    // obtain references to the home interfaces
    try {
      Context ejb = (Context)new InitialContext().lookup("java:comp/env/cmp");

      // get the Student CMP-Bean home interface
      studentHome = (StudentHome) ejb.lookup("many2many_StudentBean");

      // get the Course CMP-Bean home interface
      courseHome = (CourseHome) ejb.lookup("many2many_CourseBean");

    } catch (NamingException e) {
      throw new ServletException(e);
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    res.setContentType("text/html");

    PrintWriter out = res.getWriter();

    // get references to all entities we will work with
    String studentName = "Hermione Granger";
    String courseName = "Divination";

    try {
      // one student can be enrolled in any number of courses...
      out.println("<h3>" + studentName + "'s classes:</h3>");

      // Show all courses the student is currently enrolled in
      Iterator it = courseHome.findByStudent(studentName).iterator();
      if (! it.hasNext())
        out.println("No classes");

      while (it.hasNext())
        out.println("<li>" + ((Course) it.next()).getName());

    // ...and a course can be taught to any number of students
      out.println("<h3>" + courseName + " students:</h3>");
      it = studentHome.findByCourse(courseName).iterator();
      if (! it.hasNext())
        out.println("No students");

      while (it.hasNext())
        out.println("<li>" + ((Student)it.next()).getName() );
    } catch (FinderException e) {
      throw new ServletException(e);
    }
  }
}
