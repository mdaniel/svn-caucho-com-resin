package example.cmp.map;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.ejb.*;

import javax.naming.*;

import java.util.Collection;
import java.util.Map;
import java.util.Iterator;

import java.io.PrintWriter;

/**
 * A client to illustrate EJB managed identifying relationships and
 * compound primary keys.
 */
public class ClientServlet extends HttpServlet {
  StudentHome studentHome = null;
  CourseHome courseHome = null;
  GradeHome gradeHome = null;

  /**
   * Load the EJB Home objects one.
   */
  public void init()
    throws ServletException
  {
    try {
      Context ejb = (Context)new InitialContext().lookup("java:comp/env/cmp");

      // get the Student CMP-Bean home interface
      studentHome = (StudentHome) ejb.lookup("map_StudentBean");
      
      // get the Course CMP-Bean home interface
      courseHome = (CourseHome) ejb.lookup("map_CourseBean");
      
      // get the Grade CMP-Bean home interface
      gradeHome = (GradeHome) ejb.lookup("map_GradeBean");
    } catch (NamingException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Print the statistics for all the students.
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    res.setContentType("text/html");

    PrintWriter out = res.getWriter();

    // for all students, print their grades.
    Collection students = null;
    try {
      students = studentHome.findAll();
    } catch(javax.ejb.FinderException e) {
      throw new ServletException(e);
    }

    out.println("<title>Student Grades</title>");
    out.println("<h1>Student Grades</h1>");

    Iterator iter = students.iterator();
    while (iter.hasNext()) {
      Student student = (Student) iter.next();

      out.println("<h3>" + student.getName() + "</h3>");

      Map grades = student.getGrades();
      
      out.println("<table>");
      out.println("<tr><th>Course<th>Grade");

      Iterator courseIter = grades.keySet().iterator();
      while (courseIter.hasNext()) {
        Course course = (Course) courseIter.next();

        Grade grade = (Grade) grades.get(course);

        out.print("<tr><td>" + course.getName() + "<td>" + grade.getGrade());
      }
      out.println("</table>");
    }
  }
}
