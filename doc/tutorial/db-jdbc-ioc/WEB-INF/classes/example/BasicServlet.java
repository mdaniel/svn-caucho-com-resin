package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * The BasicServlet executes a simple JDBC query.
 *
 * The DataSource is saved on initialization to save the JNDI lookup
 * time.
 */
public class BasicServlet extends HttpServlet {
  /**
   * The saved DataSource for the database
   */
  private DataSource _ds = null;

  /**
   * Sets the data source.  The &lt;init data-source="jdbc/test"/> will
   * call this method at configuration time.
   */
  public void setDataSource(DataSource ds)
  {
    _ds = ds;
  }

  /**
   * The init() method checks if the DataSource has been properly configured.
   */
  public void init()
    throws ServletException
  {
    if (_ds == null) {
      // servlets can also create an assemble() method as in the jdbc-basic
      // tutorial to support the init-param configuration.
      throw new ServletException("data-source must be configured in an init tag.");
    }
  }

  /**
   * Respond to a request by doing a query and returning the results.
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    res.setContentType("text/html");
    
    PrintWriter out = res.getWriter();

    try {
      doQuery(out);
    } catch (SQLException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Typical pattern for database use.
   */
  public void doQuery(PrintWriter out)
    throws IOException, SQLException
  {
    Connection conn = _ds.getConnection();

    try {
      String sql = "SELECT name, cost FROM jdbc_basic_brooms ORDER BY cost DESC";
      
      Statement stmt = conn.createStatement();

      ResultSet rs = stmt.executeQuery(sql);

      out.println("<table border='3'>");
      
      while (rs.next()) {
	out.println("<tr><td>" + rs.getString(1));
	out.println("    <td>" + rs.getString(2));
      }

      out.println("</table>");
      
      rs.close();
      stmt.close();
    } finally {
      conn.close();
    }
  }
}
