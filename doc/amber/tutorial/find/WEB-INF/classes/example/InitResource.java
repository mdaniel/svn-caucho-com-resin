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
 * Normally, this would be handled by Amber code.
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
   * Initializes the database.
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
	  ResultSet rs = stmt.executeQuery("SELECT id FROM amber_find_courses");

	  if (rs.next())
	    return;
	} catch (SQLException e) {
	}
	
	stmt.executeUpdate("CREATE TABLE amber_find_courses (" +
			   "  id INTEGER PRIMARY KEY auto_increment," +
			   "  name VARCHAR(250)," +
			   "  teacher VARCHAR(250))");

	stmt.executeUpdate("INSERT INTO amber_find_courses VALUES(1, 'Potions', 'Severus Snape')");
	stmt.executeUpdate("INSERT INTO amber_find_courses VALUES(2, 'Transfiguration', 'Minerva McGonagall')");
	stmt.executeUpdate("INSERT INTO amber_find_courses VALUES(3, 'Defense Against the Dark Arts', 'Remus Lupin')");

	log.info("initialized amber/tutorial/find");

	stmt.close();
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}
