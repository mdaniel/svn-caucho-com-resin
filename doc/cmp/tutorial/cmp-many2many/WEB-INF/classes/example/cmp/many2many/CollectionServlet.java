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
public class CollectionServlet extends HttpServlet {
  StudentHome studentHome;
  CourseHome courseHome;

  /**
   *
   */
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

  /**
   *
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    res.setContentType("text/html");

    PrintWriter out = res.getWriter();

    // get references to all entities we will work with
    Student student = null;
    Course course = null;
    try {
      student = studentHome.findByPrimaryKey("Hermione Granger");
      course = courseHome.findByPrimaryKey("Divination");
    } catch (javax.ejb.FinderException e) {
      throw new ServletException(e);
    }

    // one student can be enrolled in any number of courses...
    out.println("<h3>" + student.getName() + "'s classes:</h3>");

    // Show all courses the student is currently enrolled in
    Iterator it = student.getCourseList().iterator();
    while (it.hasNext())
      out.println("<li> " + ((Course) it.next()).getName() );

    // enrolling the student in the course
    out.println("<p>Enrolling " + student.getName() + " in " +
                course.getName() + "\n");

    student.addCourse(course);

    // Show the change
    out.println("<h3>" + student.getName() + "'s new classes:</h3>");
    it = student.getCourseList().iterator();
    while (it.hasNext())
      out.println("<li> " + ((Course) it.next()).getName() );

    student.removeCourse(course);

    // Show the change
    out.println("<h3>" + course.getName() + " students:</h3>");

    // ...and a course can be taught to any number of students
    it = course.getStudentList().iterator();
    while (it.hasNext())
      out.println("<li> " + ((Student) it.next()).getName());

    // enroll the student in the course
    out.println("<p>Enrolling " + student.getName() + " in " +
                course.getName());


    course.addStudent(student);

    // Show the change
    out.println("<h3>" + course.getName() + " new students:</h3>");

    // ...and a course can be taught to any number of students
    it = course.getStudentList().iterator();
    while (it.hasNext())
      out.println("<li> " + ((Student) it.next()).getName());

    // undo our steps so that the example will be worthwhile next time its run
    course.removeStudent(student);
  }
}
