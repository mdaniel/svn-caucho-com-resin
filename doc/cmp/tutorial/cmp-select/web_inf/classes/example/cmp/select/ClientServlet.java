package example.cmp.select;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.naming.*;
import javax.ejb.*;

import java.util.*;

import java.io.PrintWriter;

/**
 * A client to illustrate entity bean select methods.  Because select
 * methods aren't directly usable by clients, the beans need to
 * create business methods to return the selected values.
 */
public class ClientServlet extends HttpServlet {

  private StudentHome _studentHome = null;
  private HouseHome _houseHome = null;

  /**
   * Sets the EJB student home.
   */
  public void setStudentHome(StudentHome home)
  {
    _studentHome = home;
  }

  /**
   * Sets the EJB house home.
   */
  public void setHouseHome(HouseHome home)
  {
    _houseHome = home;
  }

  public void init()
    throws ServletException
  {
    try {
      // The initial context
      Context ic = new InitialContext();
      // The JNDI context containing local EJB homes
      Context cmp = (Context) ic.lookup("java:comp/env/cmp");

      // get the house stub
      if (_houseHome == null)
        _houseHome = (HouseHome) cmp.lookup("select_house");
      if (_studentHome == null)
        _studentHome = (StudentHome) cmp.lookup("select_student");

    } catch (NamingException ne) {
      throw new ServletException(ne);
    }

  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    res.setContentType("text/html");

    PrintWriter out = res.getWriter();

    Iterator iterHouses = null;
    try {
      Collection houses = _houseHome.findAll();
      iterHouses = houses.iterator();
    }
    catch (FinderException fe) {
      throw new ServletException(fe);
    }

    // iterate through all Houses
    while (iterHouses.hasNext()) {
      House house = (House) iterHouses.next();
      out.println("<H3>Boys living in " + house.getName() + ":</H3>");

      // Iterate through the sorted student names
      ListIterator boys = house.getAllBoyNamesSorted().listIterator();
      
      if (! boys.hasNext())
        out.println("No boys are living in " + house.getName());
      else {
        while (boys.hasNext())
          out.println(boys.next() + "<BR>");
      }

      out.println("<BR>");
    }
  }
}
