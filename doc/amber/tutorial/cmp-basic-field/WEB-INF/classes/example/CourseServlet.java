package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ejb.EntityManager;
import javax.ejb.Query;

/**
 * The CourseServlet queries the active courses and displays them.
 */
public class CourseServlet extends HttpServlet {
  private EntityManager _manager;

  /**
   * Configures the servlet with the entity manager.
   */
  public void setEntityManager(EntityManager manager)
  {
    _manager = manager;
  }

  /**
   * Initializes the database for the example.
   */
  public void init()
    throws ServletException
  {
    Course course = null;
    
    try {
      course = (Course) _manager.find("Course", new Integer(1));
      if (course != null)
	return;
    } catch (Exception e) {
    }

    _manager.create(new Course("Potions", "Severus Snape"));
    _manager.create(new Course("Transfiguration", "Minerva McGonagall"));
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
    
    for (Course course : (List<Course>) query.listResults()) {
      out.println("course: " + course.course() + "<br>");
      out.println("teacher: " + course.teacher() + "<br>");
      out.println("<br>");
    }
  }
}
