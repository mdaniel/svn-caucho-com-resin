package example.cmp.select;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the Student bean.
 */
public interface StudentHome extends EJBLocalHome {

  /**
   * Returns the <code>Student</code> entity that has <code>name</code>
   * as its primary key.
   */
  public Student findByPrimaryKey(String name)
    throws FinderException;

  /**
   * Returns a <code>Collection</code> of all Students enrolled.
   */
  public Collection findAll()
    throws FinderException;
}
