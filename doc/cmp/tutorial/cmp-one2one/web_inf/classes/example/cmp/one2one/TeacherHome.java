package example.cmp.one2one;

import java.rmi.*;
import javax.ejb.*;
import java.util.*;

/**
 * Home interface for the Teacher bean.
 */
public interface TeacherHome extends EJBLocalHome {

  /**
   * returns the <code>Teacher</code> entity that has <code>name</code>
   * as its primary key.
   */
  Teacher findByPrimaryKey(String name)
    throws FinderException;

}
