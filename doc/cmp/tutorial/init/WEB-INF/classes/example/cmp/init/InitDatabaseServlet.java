package example.cmp.init;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.naming.*;

import javax.sql.*;
import java.sql.*;

import java.util.*;
import java.io.*;

// to flush caches
// import com.caucho.ejb.admin.EJBAdmin;


/**
 * Initializes the database for use with <code>examples</code> packages.
 * <pre>
 * You need to make sure that the init-parameter <code>jdbc-ref</code> points
 * to a correctly configured database pool.
 * <pre>
 * The init-parameter <code>schema</code> points to the SQL schema file used
 * by <code>InitDatabaseServlet</code>. If you experience
 * <code>SQLException</code>s, you may have to adapt the syntax in the file to
 * your database.
 */
public class InitDatabaseServlet extends HttpServlet {
  private int errorCount = 0;

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException
  {
    PrintWriter out = res.getWriter();

    String schemaFile = req.getParameter("schema_file");
    if (schemaFile == null)
      schemaFile = "/WEB-INF/sql/default.sql";

    String jdbcRef = req.getParameter("jdbc_ref");
    if (jdbcRef == null)
      jdbcRef = "java:comp/env/jdbc/test";

    out.println("schema_file: " + schemaFile);
    out.println("jdbc_ref: " + jdbcRef);
    out.println();

    Collection collection = getTextlinesFromResource(schemaFile);
    Iterator it = collection.iterator();

    DataSource source = null;
    Connection con = null;
    
    try {
      Context env = new InitialContext();
      source = (DataSource) env.lookup(jdbcRef);
    } catch(NamingException ne) {
      out.println("Can't find resource " + jdbcRef + ". Check the configuration to");
      out.println("make sure there's a resource-ref defining the database.");
      out.println();
      out.println(ne);

      getServletContext().log("", ne);
      return;
    }

    try {
      con = source.getConnection();
    } catch (SQLException e) {
      out.println("Can't connect to the database at " + jdbcRef + ". Check the configuration to");
      out.println("make sure the database is configured properly.");
      out.println();
      out.println(e);

      getServletContext().log("", e);
      return;
    }

    try {
      Statement s = con.createStatement();

      while (it.hasNext()) {
        String sql = (String) it.next();
        try {
          s.executeQuery(sql);
        } catch(SQLException sqle) {
          out.println("'" + sql + "' was not accepted by the database.");
          out.println(sqle);
          out.println();
        } finally {
        }
      }

      s.close();
    } catch(SQLException sqle) {
      sqle.printStackTrace(out);
    } finally {
      if (con != null) {
        try {
          out.println("closing connection");
          con.close();
        } catch(Exception e) {
          out.println("could not commit to the database" );
          
          getServletContext().log("", e);
        }
      }
    }

    /*
    EJBAdmin admin = (EJBAdmin) getServletContext().getAttribute("caucho.ejb.admin");
    if (admin != null)
      admin.invalidateCache();
    */

    out.println("Initialization complete");
  }

  /**
   * Produces a <code>Collection</Collection> of <code>String</code>s of
   * lines from a textfile. The textfile must be a resource of the webapp.
   */
  private Collection getTextlinesFromResource(String filename)
    throws IOException
  {
    ArrayList list = new ArrayList(50);

    InputStream in = getServletContext().getResourceAsStream(filename);

    if (in == null)
      throw new FileNotFoundException(filename);
    
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    
    String line = "";
    
    String inputLine;
    while ((inputLine = br.readLine()) != null) {
      inputLine = inputLine.trim();

      if (! inputLine.equals("")) {
        if (inputLine.endsWith(";")) {
          inputLine = inputLine.substring(0, inputLine.length() - 1);

          line += inputLine;
          list.add(line);
          line = "";
        }
        else if (! inputLine.startsWith("#"))
          line += inputLine;
      }
    }
    
    return list;
  }
}
