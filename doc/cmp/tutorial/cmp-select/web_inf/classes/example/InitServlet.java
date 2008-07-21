package example;

import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.*;
import javax.sql.*;

import java.util.*;
import java.io.PrintWriter;

/**
 * The InitServlet initializes the database.
 *
 * Normally, this would be handled by CMP code, but in this case
 * the example doesn't include the ejbCreate methods to make it simpler.
 */
public class InitServlet extends HttpServlet {
  /**
   * The DataSource for the table.
   */
  private DataSource _ds = null;

  /**
   * Sets the data source.
   */
  public void setDataSource(DataSource ds)
  {
    _ds = ds;
  }

  /**
   * Initializes the reference to the CourseBean home interface.
   */
  public void init()
    throws ServletException
  {
    try {
      if (_ds == null)
	throw new ServletException("data-source must be specified");

      Connection conn = _ds.getConnection();

      try {
	Statement stmt = conn.createStatement();

	ResultSet rs = stmt.executeQuery("SELECT name FROM select_student");

	if (rs.next()) {
	  rs.close();
	  stmt.close();
	  return;  // already initialized
	}

	stmt.executeUpdate("INSERT INTO select_student (name, house, gender) VALUES('Harry Potter', 'Gryffindor', 'Boy')");
	stmt.executeUpdate("INSERT INTO select_student (name, house, gender) VALUES('Ron Weasley', 'Gryffindor', 'Boy')");
	stmt.executeUpdate("INSERT INTO select_student (name, house, gender) VALUES('Hermione Granger', 'Gryffindor', 'Girl')");
	stmt.executeUpdate("INSERT INTO select_student (name, house, gender) VALUES('Draco Malfoy', 'Slytherin', 'Boy')");
	stmt.executeUpdate("INSERT INTO select_student (name, house, gender) VALUES('Penelope Clearwater', 'Ravenclaw', 'Girl')");
	stmt.executeUpdate("INSERT INTO select_student (name, house, gender) VALUES('Millicent Bulstrode', 'Slytherin', 'Girl')");

	stmt.executeUpdate("INSERT INTO select_house VALUES('Gryffindor')");
	stmt.executeUpdate("INSERT INTO select_house VALUES('Slytherin')");
	stmt.executeUpdate("INSERT INTO select_house VALUES('Ravenclaw')");
	stmt.executeUpdate("INSERT INTO select_house VALUES('Hufflepuff')");

	stmt.close();
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Illustrates how to interact with the Course EJB
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    throw new UnsupportedOperationException();
  }
}
