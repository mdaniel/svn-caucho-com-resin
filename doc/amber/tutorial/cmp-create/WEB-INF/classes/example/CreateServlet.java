package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;

import javax.ejb.Query;

import com.caucho.ejb.entity2.EntityManagerProxy;

/**
 * A client to illustrate the services of the CourseBean CMP bean.
 */
public class CreateServlet extends HttpServlet {

  private EntityManagerProxy _entityManager;

  /**
   * Sets the entity manager.
   */
  public void setEntityManager(EntityManagerProxy entityManager)
  {
    _entityManager = entityManager;
  }

  public void init()
  {
    Course course = null;
      
    try {
      course = (Course) _entityManager.find("Course", new Integer(1));
    } catch (Throwable e) {
    }

    if (course == null) {
      course = new Course("Potions", "Severus Snape");
      _entityManager.persist(course);
	
      course = new Course("Transfiguration", "Minerva McGonagall");
      _entityManager.persist(course);
	
      course = new Course("Defense Against the Dark Arts", "Remus Lupin");
      _entityManager.persist(course);
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    Query courseQuery = _entityManager.createQuery("SELECT o FROM Course o");

    out.println("<h3>Initial Classes</h3>");
    
    displayCourses(out, courseQuery);

    Course divination = null;
    Course creatures = null;

    try {
      divination = new Course("Divination", "Sybil Trelawney");

      // creates the divination course
      _entityManager.persist(divination);
    
      creatures = new Course("Care of Magical Creatures", "Rubeus Hagrid");

      // creates the creatures course
      _entityManager.persist(creatures);

      out.println("<h3>Adding some classes</h3>");
      displayCourses(out, courseQuery);
    } finally {
      // remove the courses
      String sql = "SELECT o FROM Course o WHERE o.course=?1";
    
      Query findQuery = _entityManager.createQuery(sql);

      findQuery.setParameter(1, "Divination");
      divination = (Course) findQuery.getSingleResult();

      if (divination != null)
	_entityManager.remove(divination);
    
      findQuery.setParameter(1, "Care of Magical Creatures");
      creatures = (Course) findQuery.getSingleResult();

      if (creatures != null)
	_entityManager.remove(creatures);
    }

    out.println("<h3>Removing the new classes</h3>");
    displayCourses(out, courseQuery);
  }

  private void displayCourses(PrintWriter out, Query query)
    throws IOException
  {
    List list = query.listResults();

    for (int i = 0; i < list.size(); i++) {
      Course course = (Course) list.get(i);

      out.println(course.getCourse() + " is taught by " +
		  course.getTeacher() + "<br>");
    }
  }
}
