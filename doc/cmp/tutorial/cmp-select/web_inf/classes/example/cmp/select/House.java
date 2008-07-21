package example.cmp.select;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the House bean.  In this example, the house is just
 * an empty object with no interesting data.
 *
 * <code><pre>
 * CREATE TABLE select_house (
 *   name VARCHAR(250) NOT NULL,
 *
 *   PRIMARY KEY(name)
 * );
 * </pre></code>
 */
public interface House extends EJBLocalObject {

  /**
   * returns the name of the house (CMP field).
   */
  public String getName();

  /**
   * returns a <code>Collection</code> of all Students living in this House
   * (CMR field).
   */
  public Collection getStudentList();

  /**
   * returns a sorted <code>List</code> of all Students in this House who are
   * boys.
   */
  public List getAllBoyNamesSorted();
}
