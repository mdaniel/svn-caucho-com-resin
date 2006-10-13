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
 * The DataSource saved on initialization to save the JNDI lookup
 * time.
 */
public class BasicServlet extends HttpServlet {
  private String _dataSourceName;
  
  /**
   * The saved DataSource for the database
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
   * Initializes the reference to the DataSource and caches it in
   * the servlet.
   */
  public void init()
    throws ServletException
  {
    if (_ds == null)
      assemble();
  }


  /**
   * Assembles the servlet by looking up the DataSource from JNDI.
   */
  private void assemble()
    throws ServletException
  {
    try {
      String dataSourceName = getInitParameter("data-source");

      if (dataSourceName == null)
	throw new ServletException("data-source must be specified");

      Context ic = new InitialContext();
	
      _ds = (DataSource) ic.lookup("java:comp/env/" + dataSourceName);

      if (_ds == null)
	throw new ServletException(dataSourceName + " is an unknown data-source.");
    } catch (NamingException e) {
      throw new ServletException(e);
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
