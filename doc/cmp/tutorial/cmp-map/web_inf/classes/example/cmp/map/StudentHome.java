package example.cmp.map;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the Student bean.
 */
public interface StudentHome extends EJBLocalHome {
  /**
   * returns the <code>Student</code> entity that has <code>name</code>
   * as its primary key.
   */
  public Student findByPrimaryKey(String name)
    throws FinderException;
  /**
   * Returns all the students.
   */
  public Collection findAll()
    throws FinderException;
}
