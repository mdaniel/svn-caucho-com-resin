package example.cmp.transaction;

import java.util.*;
import java.security.*;
import com.caucho.util.*;
import com.caucho.server.security.AbstractAuthenticator;

import javax.ejb.*;
import javax.naming.*;

/**
 * A custom Authenticator that looks up users in the <code>Student</code>
 * CMP bean.
 */
public class CMPAuthenticator extends AbstractAuthenticator {
  /**
   * Home Interface for the Student bean.
   */
  StudentHome studentHome = null;

  /**
   * For more sophisticated sites, this would allow cookie-based
   * authentication so the user doesn't always have to login.
   *
   * <p>Since Hogwarts student will tend to access the web from
   * public labs, we won't use cookies.
   */
  public Principal authenticateCookie(String cookieValue)
  {
    return null;
  }

  /**
   * Authenticate for the user and password.
   */
  public Principal authenticate(String user, String password)
  {
    Student student = null;
    try {
      student = getStudentHome().findByName(user);
      
      if (student.getPassword().equals(password))
        return student;
    } catch (NamingException e) {
      e.printStackTrace();
    } catch (FinderException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Returns the student home, looking it up in the JNDI context
   * if necessary.
   */
  private StudentHome getStudentHome()
    throws NamingException
  {
    if (studentHome == null) {
      // The JNDI context containing EJBs
      Context cmp = (Context) new InitialContext().lookup("java:comp/env/cmp");

      // get the student bean stub
      studentHome = (StudentHome) cmp.lookup("transaction_student");
    }
    
    return studentHome;
  }
    

  /**
   * If we were storing cookies, this would store the <cookie, user> pair
   * in some sort of persistent storage.
   */
  public boolean updateCookie(Principal user, String cookieValue)
  {
    return true;
  }

 /**
   * Returns <code>true</code> if the user is in the specified role.
   * For this example, simlpy returns true if the user exists.
   */
  public boolean isUserInRole(Principal user, String role)
  {
    return user != null;
  }
}

