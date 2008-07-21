package example.cmp.transaction;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.naming.*;
import javax.ejb.*;
import java.util.*;
import java.io.PrintWriter;

import com.caucho.server.security.ServletAuthenticator;

/**
 * Manages the registration process by invoking business logic and dispatching
 * the results to Resin XTP (Serif) views.
 */
public class RegistrationServlet extends HttpServlet {

  // XTP view page
  public static final String NEXT_PAGE = "/examples/cmp-courses.xtp";

  // session attribute name for the RegistrationSessionBean
  public static final String REGISTRATION_SESSION_NAME = "reg_session";

  // home interface of the Registration Stateful Session Bean
  private RegistrationSessionHome _registrationSessionHome = null;

  // home interface of the Course CMP entity bean
  private CourseHome _courseHome = null;

  // home interface of the Student CMP entity bean
  private StudentHome _studentHome = null;

  /**
   * Sets the course home.
   */
  public void setCourseHome(CourseHome courseHome)
  {
    _courseHome = courseHome;
  }

  /**
   * Sets the student home.
   */
  public void setStudentHome(StudentHome studentHome)
  {
    _studentHome = studentHome;
  }

  /**
   * Sets the registration home.
   */
  public void setSessionHome(RegistrationSessionHome registrationHome)
  {
    _registrationSessionHome = registrationHome;
  }

  /**
   * resolves all required Home Interfaces
   */
  public void init()
    throws ServletException
  {
    try {
      Context ic = new InitialContext();
      
      // The JNDI context containing EJBs
      Context cmp = (Context) ic.lookup("java:comp/env/cmp");

      if (_registrationSessionHome == null) {
	// get the registration session bean stub
	_registrationSessionHome = (RegistrationSessionHome)
	  cmp.lookup("transaction_registration_session");
      }

      if (_courseHome == null) {
	// get the course bean stub
	_courseHome = (CourseHome)cmp.lookup("transaction_course");
      }
    } catch (NamingException e) {
      throw new ServletException(e);
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    HttpSession session = req.getSession();
    PrintWriter out = res.getWriter();

    String cmd = null;
    int courseId = 0;
    Course course = null;

    // parse parameters
    cmd = req.getParameter("cmd")==null ? "" :
                   (String)req.getParameter("cmd");
    courseId = req.getParameter("course_id")==null ? 0 :
                     Integer.parseInt((String)req.getParameter("course_id"));

    if (courseId != 0) {
      try {
        course = _courseHome.findByPrimaryKey(courseId);
      } catch (FinderException fe) {
        throw new ServletException("There is no course with ID " + courseId, fe);
      }
    }

    // Obtain our Registration bean that we stored in the Servlet's
    // HttpSession during a previous request.
    RegistrationSession regSession =
      (RegistrationSession)session.getAttribute(REGISTRATION_SESSION_NAME);

    // If this is a new Servlet Session, obtain a Resgistration Session Bean
    // and store it in the servlet's HttpSession
    if (regSession == null) {
      try {
        regSession = _registrationSessionHome.create();
        session.setAttribute(REGISTRATION_SESSION_NAME, regSession);
      } catch (CreateException ce) {
        throw new ServletException(ce);
      }
    }
    
    // execute cmd
    if (cmd.equals("select")) {
      try {
        regSession.addCourse(course);
      } catch (FinderException fe) {
        throw new ServletException(fe);
      }
    }
    else if (cmd.equals("deselect")) {
      try {
        regSession.removeCourse(course);
      } catch (FinderException fe) {
        throw new ServletException(fe);
      }
    }
    else if (cmd.equals("finalize")) {
      try {
        regSession.finalizeRegistration();
        req.setAttribute("transaction_status",
          new Integer(RegistrationSessionBean.TRANSACTION_COMMITTED));
      } catch (RegistrationDeniedException rde) {
        req.setAttribute("transaction_status",
          new Integer(RegistrationSessionBean.TRANSACTION_ROLLEDBACK));
        req.setAttribute("rollback_exception", rde);
      }
    }

    // dispatch to the XTP view
    this.getServletContext().getRequestDispatcher(NEXT_PAGE).forward(req,res);
  }
}
