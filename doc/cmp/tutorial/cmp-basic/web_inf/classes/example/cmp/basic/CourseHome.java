package example.cmp.basic;

import javax.ejb.*;

/**
 * Minimal home interface for courses taught at Hogwarts, containing methods
 * to find any Course.
 *
 * <p>The example's <code>CourseHome</code> provides the minimal
 * implementation of a home interface: a single findByPrimaryKey
 * method to find any course.  Home interfaces can also provide
 * create methods as described in example.cmp.create.CourseHome.
 *
 * <p>Applications Use the home interface to obtain references
 * to whatever entities you're interested in. Each entity that you
 * get from the home interface (using its create or finder methods)
 * is then represented by its local interface.
 *
 * @see example.cmp.create.CourseHome.
 */
public interface CourseHome extends EJBLocalHome {
  /**
   * Returns the <code>Course</code> with <code>courseId</code>
   * as its primary key.  Every home interface must define the
   * findByPrimaryKey method.  The argument must be the primary key type
   * and the return value must be the local interface.
   *
   * @param courseId ID of the course that is to be retreived
   *
   * @return the local interface of the specified course.
   *
   * @exception ObjectNotFoundException if no such course exists.
   */
  Course findByPrimaryKey(String courseId)
    throws FinderException;
}
