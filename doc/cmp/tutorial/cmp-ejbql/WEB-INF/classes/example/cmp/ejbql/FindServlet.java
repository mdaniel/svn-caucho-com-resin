package example.cmp.ejbql;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.naming.*;
import javax.ejb.*;

import java.util.*;

import java.io.PrintWriter;

/**
 * A client to illustrate entity bean finder methods
 */
public class FindServlet extends HttpServlet {
  private CourseHome _courseHome = null;
  private TeacherHome _teacherHome = null;
  private StudentHome _studentHome = null;
  private HouseHome _houseHome = null;

  /**
   * Sets the local CourseHome
   */
  public void setCourseHome(CourseHome home)
  {
    _courseHome = home;
  }

  /**
   * Sets the local TeacherHome
   */
  public void setTeacherHome(TeacherHome home)
  {
    _teacherHome = home;
  }

  /**
   * Sets the local StudentHome
   */
  public void setStudentHome(StudentHome home)
  {
    _studentHome = home;
  }

  /**
   * Sets the local HouseHome
   */
  public void setHouseHome(HouseHome home)
  {
    _houseHome = home;
  }

  public void init()
    throws ServletException
  {
    try {
      Context ic = new InitialContext();
      // The JNDI context containing EJBs
      Context cmp = (Context) ic.lookup("java:comp/env/cmp");

      // get the bean stubs
      if (_courseHome == null)
        _courseHome = (CourseHome) cmp.lookup("ejbql_course");
      if (_teacherHome == null)
        _teacherHome = (TeacherHome) cmp.lookup("ejbql_teacher");
      if (_studentHome == null)
        _studentHome = (StudentHome) cmp.lookup("ejbql_student");
      if (_houseHome == null)
        _houseHome = (HouseHome) cmp.lookup("ejbql_house");

    } catch (NamingException e) {
      throw new ServletException(e);
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();
    res.setContentType("text/html");

    try {
      out.println("<H3>Students enrolled at Hogwarts</H3>");
      Collection allStudents = _studentHome.findAll();
      Iterator studentIter = allStudents.iterator();
      
      while (studentIter.hasNext()) {
        out.println(((Student)studentIter.next()).getName() + "<BR>");
      }

      // Find out for all Students who their teachers are
      out.println("<h3>Students are taking classes from these teachers:</h3>");
      studentIter = allStudents.iterator();
      
      while (studentIter.hasNext()) {
        Student student = (Student)studentIter.next();
        String studentName = student.getName();
        Iterator teacherIter =
          _teacherHome.findByStudent(studentName).iterator();
        out.println("<b>"+studentName+"'s teachers</b><BR>");
        
        if (! teacherIter.hasNext())
          out.println(studentName + " is not taking any classes<BR>");
        
        while (teacherIter.hasNext())
          out.println(((Teacher)teacherIter.next()).getName()+"<BR>");

        out.println("<BR>");
      }
      
      // Find out what classes are taken by Students in Gryffindor
      String houseName = "Gryffindor";
      out.println("<h3>Students in " + houseName +
                  " are taking these classes:</h3>");
      Collection courses = _courseHome.findByHouse("Gryffindor");
      Iterator courseIter = courses.iterator();
      
      if (! courseIter.hasNext())
        out.println("Students in " + houseName + " are lazy. " +
                    "Nobody is taking any courses.<BR>");
      
      while (courseIter.hasNext()) {
        Course course = (Course)courseIter.next();
        out.println(course.getName() + " (taught by Professor " +
                    course.getTeacher().getName() + ")<BR>");
      }
      
      out.println("<BR>");
    } catch (FinderException e) {
      throw new ServletException(e);
    }
  }
}
