package example.cmp.transaction;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;
import java.security.Principal;

/**
 * Local interface for the Student bean.
 */
public interface Student extends EJBLocalObject, java.security.Principal {

  /**
   * Returns the student's ID (CMP field). This is also the primary key.
   */
  int getId();

  /**
   * Returns the student's name (CMP field).
   */
  String getName();

  /**
   * Returns the password associated with this student.
   */
  public String getPassword();

  /**
   * Returns the gender of the student (CMP field).
   */
  public String getGender();
  /**
   * Sets the gender of the student (CMP field).
   */
  public void setGender(String gender);
  /**
   * Returns a <code>Collection</code> of all Courses that the student is
   * currently enrolled in (CMR field).
   */
  public Collection getCourseList();

}
