package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;

import javax.ejb.Query;

import com.caucho.amber.ejb3.EntityManagerProxy;

/**
 * A client to illustrate the query.
 */
public class QueryServlet extends HttpServlet {

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
    House house = null;
      
    try {
      house = (House) _entityManager.find("House", new Long(1));
    } catch (Throwable e) {
    }

    if (house == null) {
      House gryffindor = new House("Gryffindor");
      _entityManager.persist(gryffindor);
	
      House slytherin = new House("Slytherin");
      _entityManager.persist(slytherin);
	
      House ravenclaw = new House("Ravenclaw");
      _entityManager.persist(ravenclaw);
	
      House hufflepuff = new House("Hufflepuff");
      _entityManager.persist(hufflepuff);

      Student student;

      student = new Student("Harry Potter", "M", gryffindor);
      _entityManager.persist(student);

      student = new Student("Ron Weasley", "M", gryffindor);
      _entityManager.persist(student);

      student = new Student("Hermione Granger", "F", gryffindor);
      _entityManager.persist(student);

      student = new Student("Draco Malfoy", "M", slytherin);
      _entityManager.persist(student);

      student = new Student("Millicent Bulstrode", "F", slytherin);
      _entityManager.persist(student);

      student = new Student("Penelope Clearwater", "F", ravenclaw);
      _entityManager.persist(student);
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    Query allHouse = _entityManager.createQuery("SELECT o FROM House o");
    
    String sql = ("SELECT s" +
		  " FROM House h, IN(h.studentList) s" +
		  " WHERE h.id=?1 AND s.gender='M'");
    Query boysInHouse = _entityManager.createQuery(sql);
    
    List houses = allHouse.listResults();

    for (int i = 0; i < houses.size(); i++) {
      House house = (House) houses.get(i);
      
      out.println("<H3>Boys living in " + house.getName() + ":</H3>");

      boysInHouse.setParameter(1, new Long(house.getId()));
      List boys = boysInHouse.listResults();

      if (boys.size() == 0)
	out.println("No boys are living in " + house.getName());

      for (int j = 0; j < boys.size(); j++) {
	Student boy = (Student) boys.get(j);

	out.println(boy.getName() + "<br>");
      }
    }
  }
}
