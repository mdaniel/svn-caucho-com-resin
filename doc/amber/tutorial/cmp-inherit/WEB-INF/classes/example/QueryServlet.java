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
import javax.ejb.Inject;

/**
 * The QueryServlet just displays the values.
 */
public class QueryServlet extends HttpServlet {
  private EntityManager _manager;

  @Inject
  public void setEntityManager(EntityManager manager)
  {
    _manager = manager;
  }

  /**
   * Illustrates how to interact with the Course EJB
   */
  public void init()
    throws ServletException
  {
    Student student = null;
    
    try {
      student = (Student) _manager.find("Student", new Long(1));
      if (student != null)
	return;
    } catch (Exception e) {
    }

    _manager.create(new Student("Harry Potter"));
    _manager.create(new Prefect("Ron Weasley"));
    _manager.create(new Prefect("Hermione Granger"));
  }

  /**
   * Illustrates how to interact with the Course EJB
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");
    
    out.println("<h3>Students</h3>");

    Query query = _manager.createQuery("SELECT o FROM Student o");
    
    for (Object student : query.listResults()) {
      out.println(student + "<br>");
    }
  }
}
