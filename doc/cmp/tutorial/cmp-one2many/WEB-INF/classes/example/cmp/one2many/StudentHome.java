package example.cmp.one2many;

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
   * returns a <code>Collection</code> of all students managed by the container.
   */
  public Collection findAll()
    throws FinderException;

}
