package example;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ejb.EntityManager;
import javax.ejb.Inject;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRED;

/**
 * The SwapServlet swaps cources.
 */
public class SwapServlet extends HttpServlet {
  @Inject
  private EntityManager _manager;

  /**
   * Initializes the database for the demo.
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

    Course potions = new Course();
    potions.setCourse("Potions");
    potions.setTeacher("Severus Snape");

    _manager.persist(potions);

    Course xfig = new Course();
    xfig.setCourse("Transfiguration");
    xfig.setTeacher("Minerva McGonagall");

    _manager.persist(xfig);
  }

  /**
   * Illustrates how to swap with a transaction.
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    Course []courses = new Course[2];

    courses[0] = (Course) _manager.find("Course", new Integer(1));
    courses[1] = (Course) _manager.find("Course", new Integer(2));

    printCourses(out, "Initial Teachers", courses);

    // Swaps the courses using the transactional swap method
    swap(courses[0], courses[1]);

    printCourses(out, "Swapped Teachers", courses);

    // Swaps the courses using the transactional swap method
    swap(courses[0], courses[1]);
  }

  /**
   * Swaps the teachers within a transation.
   */
  @TransactionAttribute(REQUIRED)
  private void swap(Course a, Course b)
  {
    String teacher = a.getTeacher();
    a.setTeacher(b.getTeacher());
    b.setTeacher(teacher);
  }

  private void printCourses(PrintWriter out, String title, Course []courses)
    throws IOException
  {
    out.println("<h3>" + title + "</h3>");

    for (Course course : courses) {
      out.println("course: " + course.getCourse() + "<br>");
      out.println("teacher: " + course.getTeacher() + "<br>");
      out.println("<br>");
    }
  }
}
