package example.cmp.map;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the Course bean.
 */
public interface CourseHome extends EJBLocalHome {
  /**
   * returns the <code>Course</code> entity that has <code>name</code>
   * as its primary key.
   */
  public Course findByPrimaryKey(String name)
    throws FinderException;
}
