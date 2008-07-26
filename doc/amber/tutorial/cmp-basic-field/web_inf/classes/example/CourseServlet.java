package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.persistence.*;

/**
 * The CourseServlet queries the active courses and displays them.
 */
public class CourseServlet extends HttpServlet {
  @PersistenceContext(name="example")
  private EntityManager _manager;

  /**
   * Initializes the database for the example.
   */
  public void init()
    throws ServletException
  {
    Course course = null;
    
    try {
      course = _manager.find(Course.class, new Integer(1));
      if (course != null)
	return;
    } catch (Exception e) {
    }

    _manager.persist(new Course("Potions", "Severus Snape"));
    _manager.persist(new Course("Transfiguration", "Minerva McGonagall"));
  }

  /**
   * Illustrates how to interact with the Course EJB
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    out.println("<h3>Course Details</h3>");

    Query query = _manager.createQuery("SELECT o FROM Course o");
    
    for (Course course : (List<Course>) query.getResultList()) {
      out.println("course: " + course.course() + "<br>");
      out.println("teacher: " + course.teacher() + "<br>");
      out.println("<br>");
    }
  }
}
