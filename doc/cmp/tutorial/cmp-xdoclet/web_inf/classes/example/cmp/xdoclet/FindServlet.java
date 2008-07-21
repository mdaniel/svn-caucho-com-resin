package example.cmp.xdoclet;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.ejb.*;

import javax.naming.*;

import java.util.Collection;
import java.util.Iterator;

import java.io.PrintWriter;

/**
 * A client to illustrate EJB managed relations in a many-to-many context.
 *
 * @web:servlet name="findServlet"
 * @web:servlet-mapping url-pattern="/find"
 */
public class FindServlet extends HttpServlet
{
  StudentHome studentHome;
  CourseHome courseHome;

  public void init()
    throws ServletException
  {
    // obtain references to the home interfaces
    try {
      Context ejb = (Context)new InitialContext().lookup("java:comp/env/cmp");

      // get the Student CMP-Bean home interface
      studentHome = (StudentHome) ejb.lookup("xdoclet_StudentBean");

      // get the Course CMP-Bean home interface
      courseHome = (CourseHome) ejb.lookup("xdoclet_CourseBean");

    } catch (NamingException e) {
      throw new ServletException(e);
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    res.setContentType("text/html");

    PrintWriter out = res.getWriter();
    
    try {
      Course divination = courseHome.findByPrimaryKey("Divination");
      Student hermione = studentHome.findByPrimaryKey("Hermione Granger");
      Iterator i;
      
      out.println("<h3>Students By Class</h3>");
      // iterate over all courses
      i = courseHome.findAll().iterator();
      out.println("<ul>");
      while (i.hasNext()) {
        Course course = (Course) i.next();
        out.println("<li><b>" + course.getName() + ":</b>");
        
        out.println("<ul>");
        // Students in the Course
        Iterator students = course.getStudentList().iterator();
        if (! students.hasNext())
          out.println("<li>No students</li>");
        while (students.hasNext())
          out.println("<li>" + ((Student) students.next()).getName() + "</li>");
        out.println("</ul></li>");
      }
      out.println("</ul>");
      
      // add a student to Divination
      out.println("Enrolling Hermione in Divination<br>");
      divination.addStudent(hermione);
      out.println("<h4>Divination's new student list:</h4>");
      out.println("<ul>");
      i = divination.getStudentList().iterator();
      while (i.hasNext())
          out.println("<li>" + ((Student) i.next()).getName() + "</li>");
      out.println("</ul>");
      divination.removeStudent(hermione);
      
      // list all instructors
      i = courseHome.listAllInstructors().iterator();
      out.println("<h3>Teachers</h3>");
      if (! i.hasNext())
        out.println("No teachers<br>");
      while (i.hasNext())
        out.println(((String) i.next()) + "<br>");
      
      
      out.println("<h3>Classes by Student</h3>");
      // iterate over all students
      i = studentHome.findAll().iterator();
      out.println("<ul>");
      while (i.hasNext()) {
        Student student = (Student) i.next();
        out.println("<li><b>" + student.getName() + ":</b>");
        
        out.println("<ul>");
        // Courses taken by the Student
        Iterator courses = student.getCourseList().iterator();
        if (! courses.hasNext())
          out.println("<li>No classes?!</li>");
        while (courses.hasNext())
          out.println("<li>" + ((Course) courses.next()).getName() + "</li>");
        out.println("</ul></li>");
      }
      out.println("</ul>");
      
      // add a student to Divination
      out.println("Enrolling Hermione in Divination<br>");
      hermione.addCourse(divination);
      out.println("<h4>Hermione's new class list:</h4>");
      out.println("<ul>");
      i = hermione.getCourseList().iterator();
      while (i.hasNext())
          out.println("<li>" + ((Course) i.next()).getName() + "</li>");
      out.println("</ul>");
      hermione.removeCourse(divination);
    } catch (FinderException e) {
      throw new ServletException(e);
    }
  }
}
