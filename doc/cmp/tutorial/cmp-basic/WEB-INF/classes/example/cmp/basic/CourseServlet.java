package example.cmp.basic;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.naming.*;
import javax.transaction.*;
import javax.ejb.*;

import java.util.*;
import java.io.PrintWriter;

/**
 * The basic CourseClient shows the basic flow of any Resin-CMP client.
 */
public class CourseServlet extends HttpServlet {
  /**
   * Cached reference to the CourseHome interface.  Because this object
   * never changes, the client can look it up once in the <code>init()</code>
   * method and avoid JNDI calls for each request.
   */
  private CourseHome _home = null;
  /**
   * Cached reference to the UserTransaction.  Because this object
   * never changes, the client can look it up once in the <code>init()</code>
   * method and avoid JNDI calls for each request.
   *
   * <p>Normally, Resin-CMP clients will use business methods to
   * encapsulate transactions.  This example uses an explicit
   * UserTransaction to explain more clearly what's going on.
   */
  private UserTransaction _userTrans = null;

  /**
   * Sets the course home.
   */
  public void setCourseHome(CourseHome home)
  {
    _home = home;
  }

  /**
   * Initializes the reference to the CourseBean home interface.
   */
  public void init()
    throws ServletException
  {
    try {
      Context ic = new InitialContext();
      
      // The JNDI context containing local EJBs
      Context cmp = (Context) ic.lookup("java:comp/env/cmp");

      if (cmp == null)
	throw new ServletException("ejb-server has not initialized properly.");

      // Get the house stub.
      // If you're familar with old-style EJB calls, the local EJB home
      // doesn't need the PortableRemoteObject.narrow call because it's
      // not a remote object.
      if (_home == null)
        _home = (CourseHome) cmp.lookup("basic_CourseBean");

      // The JNDI context containing the user transaction
      Object trans = ic.lookup("java:comp/UserTransaction");
      _userTrans = (UserTransaction) trans;
    } catch (NamingException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Illustrates how to interact with the Course EJB
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    //
    // Each entity is mapped to one database row that is identified by
    // its primary key.
    // For all CMP beans, Resin-CMP implements a mandatory findByPrimaryKey
    // method that we must define in the home interface (CourseHome.java).
    // We know the primary keys of two Courses that have been preset in the
    // database. We can now easily obtain references to two Course
    // entities.
    //
    Course []course = new Course[2];

    try {
      course[0] = _home.findByPrimaryKey("Potions");
      course[1] = _home.findByPrimaryKey("Transfiguration");

      out.println("<h3>Course Details</h3>");

      for (int i = 0; i < course.length; i++) {
        out.println("course: " + course[i].getId() + "<br>");
        out.println("instructor: " + course[i].getInstructor() + "<br>");
        out.println("<br>");
      }

      out.println();
      out.println("<p>Swap the instructors for the two courses." );
      out.println("The change will be automatically and instantly reflected" );
      out.println("in the database" );
      out.println();

      // Swap the instructors inside a transaction.
      // The transaction acts like an intelligent synchronized lock.

      course[0].swap(course[1]);
    
      out.println("<h3>New Course Details:</h3>");
      for (int i = 0; i < course.length; i++) {
        out.println("course: " + course[i].getId() + "<br>");
        out.println("instructor: " + course[i].getInstructor() + "<br>");
        out.println("<br>");
      }

      // The following is equivalent to the swap method above.
      // Usually transactions are be encapsulated in an EJB business
      // method like the swap method so Resin-CMP would take care of
      // them automatically, but you can create your own transaction
      // context, too.
      try {
        _userTrans.begin();
        String temp = course[0].getInstructor();
        course[0].setInstructor(course[1].getInstructor());
        course[1].setInstructor(temp);
      } finally {
        _userTrans.commit();
      }
    }
    catch (Exception e) {
      throw new ServletException(e);
    }
  }
}
