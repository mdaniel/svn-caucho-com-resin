package example.cmp.ejbql;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the Student bean.
 */
public interface Student extends EJBLocalObject {

  /**
   * Returns the student's name (CMP field).
   */
  String getName();

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

  /**
   * returns the <code>House</code> that this Student belongs to (CMR field).
   */
  public House getHouse();

  /**
   * sets the <code>House</code> that this Student is to belong to (CMR field).
   *
   * @param house new House that this Student will belong to.
   */
  public void setHouse(House house);

}
