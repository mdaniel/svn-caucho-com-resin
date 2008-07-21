package example.cmp.one2many;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.ejb.*;

import javax.naming.*;

import java.util.Collection;
import java.util.Iterator;

import java.io.PrintWriter;

/**
 *  A client to illustrate EJB managed relations in a one-to-many context.
 */
public class ClientServlet extends HttpServlet {
  StudentHome studentHome = null;
  HouseHome houseHome = null;


  /**
   *
   */
  public void init()
    throws ServletException
  {
    try {
      Context ejb = (Context)new InitialContext().lookup("java:comp/env/cmp");

      // get the Student CMP-Bean home interface
      studentHome = (StudentHome)ejb.lookup("one2many_StudentBean");

      // get the House CMP-Bean home interface
      houseHome = (HouseHome)ejb.lookup("one2many_HouseBean");

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

    // for all students, print in which House they live
    Collection students = null;
    try {
      students = studentHome.findAll();
    } catch(javax.ejb.FinderException e) {
      throw new ServletException(e);
    }

    out.println("<h3>Student House assignments</h3>");

    Iterator iter = students.iterator();
    while (iter.hasNext()) {
      Student student = (Student) iter.next();
      String name = student.getName();
      String houseName = student.getHouse().getName();
      out.println(name + " lives in " + houseName + "<br>");
    }

    // for each House, print the list of students that live there:
    out.println("<h3>Residents of all Houses</h3>");
    Collection houses = null;
    try {
      houses = houseHome.findAll();
    } catch(javax.ejb.FinderException e) {
      throw new ServletException(e);
    }

    // iterate through the Houses
    iter = houses.iterator();
    while (iter.hasNext()) {
      House house = (House) iter.next();
      out.println("<h4>" + house.getName() + "</h4>");

      students = (Collection) house.getStudentList();

      // iterate through the students for that House
      Iterator studentIter = students.iterator();
      if (! studentIter.hasNext())
        out.println("No students.<br>");

      while (studentIter.hasNext()) {
        Student student = (Student) studentIter.next();
        out.println("<li>" + student.getName());
      }
    }

    /*
    // so far, the only point where we have seen EJB's relation feature in
    // action was the getStudentList() method.
    //
    // The code below adds a new student to house Gryffindor. If you're using
    // the examples schema supplied with Resin-EJB, Harry Potter is already
    // living in Slytherin Hall. Resin-EJB automatically updates the
    // "Harry Potter" entity to reflect the fact that he moved to
    // Gryffindor.
    try {

      House gryffindor = houseHome.findByPrimaryKey("Gryffindor");
      House slytherin = houseHome.findByPrimaryKey("Slytherin");
      Student harry = studentHome.findByPrimaryKey("Harry Potter");

      // for the example, make sure Harry lives somewhere other than
      // Gryffindor. <code>setHouse</code> is a helper method
      // defined in the local interface, <code>Student</code>.
      harry.setHouse(slytherin);

      println(out,harry.getName() + " currently lives in " +
        harry.getHouse().getName());

      println(out,"Moving " + harry.getName() + " from " +
        harry.getHouse().getName() + " to " + gryffindor.getName() );

      // automatically updates <code>harry</code> entity
      gryffindor.addStudent(harry);

      println(out,harry.getName() + " now lives in " +
        harry.getHouse().getName());

    } catch( FinderException fe ) {
    }
    */
  }
}
