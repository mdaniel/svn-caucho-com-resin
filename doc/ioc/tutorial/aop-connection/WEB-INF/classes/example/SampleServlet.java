package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;

public class SampleServlet extends GenericServlet {
  public void init()
    throws ServletException
  {
    try {
      initDatabase(null);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  @RequireConnection("jdbc/resin")
  private void initDatabase(Connection conn)
    throws SQLException
  {
    Statement stmt = conn.createStatement();

    try {
      ResultSet rs = stmt.executeQuery("SELECT name FROM aop_houses");
      if (rs.next()) {
	rs.close();
	stmt.close();
	return;
      }
    } catch (SQLException e) {
    }

    String sql = ("CREATE TABLE aop_houses (" +
		  "  id INTEGER PRIMARY KEY auto_increment," +
		  "  name VARCHAR(255)" + 
		  ")");

    stmt.executeUpdate(sql);

    stmt.executeUpdate("INSERT INTO aop_houses (name) VALUES ('Gryffindor')");
    stmt.executeUpdate("INSERT INTO aop_houses (name) VALUES ('Ravenclaw')");
    stmt.executeUpdate("INSERT INTO aop_houses (name) VALUES ('Hufflepuff')");
    stmt.executeUpdate("INSERT INTO aop_houses (name) VALUES ('Slytherin')");

    stmt.close();
  }
  
  public void service(ServletRequest request,
		      ServletResponse response)
    throws IOException, ServletException
  {
    response.setContentType("text/html");
    
    PrintWriter out = response.getWriter();

    try {
      testConnection(out, null);
    } catch (SQLException e) {
      throw new ServletException(e);
    }
  }

  @RequireConnection("jdbc/resin")
  private void testConnection(PrintWriter out, Connection conn)
    throws IOException, SQLException
  {
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT id, name FROM aop_houses");

    while (rs.next()) {
      out.println(rs.getInt(1) + ": " + rs.getString(2) + "<br>");
    }

    rs.close();
    stmt.close();
  }
}
