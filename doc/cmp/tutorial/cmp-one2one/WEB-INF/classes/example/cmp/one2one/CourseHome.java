package example.cmp.one2one;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the Course bean.
 */
public interface CourseHome extends EJBLocalHome {

  /**
   * returns the <code>Course</code> that has <code>name</code>
   * as its primary key.
   *
   * @param name Name of the course we want to find.
   */
  Course findByPrimaryKey(String name)
    throws FinderException;

  /**
   * returns all Courses managed by the container (Finder method).
   *
   * <code><pre>
   * SELECT o FROM one2one_course o
   * </pre></code>
   */
  Collection findAll()
    throws FinderException;
}
