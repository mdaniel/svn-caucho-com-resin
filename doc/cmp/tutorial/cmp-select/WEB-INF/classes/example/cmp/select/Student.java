package example.cmp.select;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the Student bean.
 *
 * <code><pre>
 * CREATE TABLE select_student (
 *   name VARCHAR(250) NOT NULL,
 *   gender VARCHAR(6) NOT NULL,
 *   house VARCHAR(250) NOT NULL,
 * 
 *   PRIMARY KEY(name)
 * );
 * </pre></code>
 *
 * In this example, the gender field is not exposed in the
 * local interface (for no reason other to demonstrate that
 * it's possible). The getGender and setGender methods are still
 * in the StudentBean implementation, but they're not accessible
 * to clients.
 *
 * <p>The getName method must always be available because
 * it's the primary key, and the getHouse and setHouse methods must always
 * be in the local interface because relation methods must be available to
 * the persistence manager..
 */
public interface Student extends EJBLocalObject {
  /**
   * Returns the student's name (the primary key).
   */
  public String getName();

  /**
   * Returns the <code>House</code> that this Student belongs to (CMR field).
   */
  public House getHouse();

  /**
   * sets the <code>House</code> that this Student is to belong to (CMR field).
   *
   * @param house new House that this Student will belong to.
   */
  public void setHouse(House house);
}
