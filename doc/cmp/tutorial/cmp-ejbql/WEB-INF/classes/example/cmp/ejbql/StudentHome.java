package example.cmp.ejbql;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the Student bean.  The <code>findAll()</code> method
 * is one of the most generically useful queries.
 */
public interface StudentHome extends EJBLocalHome {
  /**
   * Returns the <code>Student</code> entity that has <code>name</code>
   * as its primary key.
   */
  public Student findByPrimaryKey(String name)
    throws FinderException;

  /**
   * Returns a Collection of all Students enrolled at Hogwarts.
   *
   * <code><pre>
   * SELECT o FROM ejbql_student o
   * </pre></code>
   */
  abstract public Collection findAll()
    throws FinderException;
}
