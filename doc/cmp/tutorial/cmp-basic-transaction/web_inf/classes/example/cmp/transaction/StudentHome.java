package example.cmp.transaction;

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
   *
   * @param id ID of the <code>student</code> to be returned.
   */
  public abstract Student findByPrimaryKey(int id)
    throws FinderException;

  /**
   * Returns the Student with the given name.
   *
   * @param name Name of the <code>Student</code> to be returned.
   */
  public abstract Student findByName(String name)
      throws FinderException;

}
