package example.cmp.id;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.ejb.*;

import javax.naming.*;

import java.util.Collection;
import java.util.Iterator;

import java.io.PrintWriter;

/**
 *  A client to illustrate EJB managed identifying relationships.
 */
public class ClientServlet extends HttpServlet {
  StudentHome _studentHome = null;
  QuidditchHome _quidditchHome = null;

  /**
   * Sets the student ejb local home.
   */
  public void setStudentHome(StudentHome home)
  {
    _studentHome = home;
  }

  /**
   * Sets the quidditch ejb local home.
   */
  public void setQuidditchHome(QuidditchHome home)
  {
    _quidditchHome = home;
  }

  /**
   * Load the EJB Home objects one.
   */
  public void init()
    throws ServletException
  {
    try {
      Context ic = new InitialContext();
      Context cmp = (Context) ic.lookup("java:comp/env/cmp");

      // get the Student CMP-Bean home interface
      if (_studentHome == null)
        _studentHome = (StudentHome) cmp.lookup("id_StudentBean");

      // get the House CMP-Bean home interface
      if (_quidditchHome == null)
        _quidditchHome = (QuidditchHome) cmp.lookup("id_QuidditchBean");
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

    // for all students, print in which House they live
    Collection students = null;
    try {
      students = _studentHome.findAll();
    } catch(javax.ejb.FinderException e) {
      throw new ServletException(e);
    }

    out.println("<h3>Student Quidditch statistics</h3>");

    out.println("<table>");
    out.println("<tr><th>Student<th>Position<th>Points");

    Iterator iter = students.iterator();
    while (iter.hasNext()) {
      Student student = (Student) iter.next();

      out.print("<tr><td>" + student.getName());
      
      Quidditch quidditch = student.getQuidditch();

      if (quidditch != null) {
        out.print("<td>" + quidditch.getPosition());
        out.println("<td>" + quidditch.getPoints());
      }
      else {
        out.println("<td>none<td>n/a");
      }
    }

    out.println("</table>");
  }
}
