package example;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.caucho.config.ConfigException;

/**
 * The InitServlet initializes the database.
 *
 * Normally, this would be handled by CMP code, but in this case
 * the example doesn't include the ejbCreate methods to make it simpler.
 */
public class InitResource {
  private static Logger log = Logger.getLogger(InitResource.class.getName());
  
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
    throws Exception
  {
    try {
      if (_ds == null)
	throw new ConfigException("data-source must be specified");

      Connection conn = _ds.getConnection();

      try {
	Statement stmt = conn.createStatement();

	try {
	  ResultSet rs = stmt.executeQuery("SELECT id FROM amber_many2one_students");

	  if (rs.next())
	    return;
	} catch (SQLException e) {
	}

	try {
  	  stmt.executeUpdate("DROP TABLE amber_many2one_houses");
	} catch (SQLException e) {
	}

	try {
  	  stmt.executeUpdate("DROP TABLE amber_many2one_students");
	} catch (SQLException e) {
	}
	
	stmt.executeUpdate("CREATE TABLE amber_many2one_houses (" +
			   "  id INTEGER PRIMARY KEY," +
			   "  name VARCHAR(250))");

	stmt.executeUpdate("INSERT INTO amber_many2one_houses VALUES (1, 'Gryffindor')");
	stmt.executeUpdate("INSERT INTO amber_many2one_houses VALUES (2, 'Slytherin')");
	stmt.executeUpdate("INSERT INTO amber_many2one_houses VALUES (3, 'Hufflepuff')");
	stmt.executeUpdate("INSERT INTO amber_many2one_houses VALUES (4, 'Ravenclaw')");
	
	stmt.executeUpdate("CREATE TABLE amber_many2one_students (" +
			   "  id INTEGER PRIMARY KEY," +
			   "  name VARCHAR(250)," +
			   "  house INTEGER)");

	stmt.executeUpdate("INSERT INTO amber_many2one_students VALUES (1, 'Harry Potter', 1)");
	stmt.executeUpdate("INSERT INTO amber_many2one_students VALUES (2, 'Ron Weasley', 1)");
	stmt.executeUpdate("INSERT INTO amber_many2one_students VALUES (3, 'Hermione Granger', 1)");
	
	stmt.executeUpdate("INSERT INTO amber_many2one_students VALUES (4, 'Draco Malfoy', 2)");
	stmt.executeUpdate("INSERT INTO amber_many2one_students VALUES (5, 'Millicent Bustrode', 2)");
	
	stmt.executeUpdate("INSERT INTO amber_many2one_students VALUES (6, 'Justin Finch-Fletchly', 3)");
	stmt.executeUpdate("INSERT INTO amber_many2one_students VALUES (7, 'Cho Chang', 3)");
	
	stmt.executeUpdate("INSERT INTO amber_many2one_students VALUES (8, 'Terry Boot', 4)");
	stmt.executeUpdate("INSERT INTO amber_many2one_students VALUES (9, 'Luna Lovegood', 4)");

	log.info("initialized amber/tutorial/many2one");

	stmt.close();
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}
