package example.cmp.find;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBHome;
import javax.ejb.FinderException;

import java.util.Collection;

/**
 * Home interface for the Course bean.
 * The home interface for the Course bean.  The home interface enables
 * you to create new entities and to obtain references to existing ones
 * using find methods.
 *
 * <p>The home interface enables you to create new entities and to obtain
 * references to existing ones.
 *
 * <p>The idea is that you use the Home Interface to obtain references
 * to whatever entities you're interested in. Each entity that you
 * <p>Applications use the Home Interface to obtain references
 * to whatever entities it needs.   Each entity that you
 * get from the Home Interface (using its create or finder methods)
 * is then accessible through its Local Interface.
 *
 */
public interface CourseHome extends EJBLocalHome {
  /**
   * Returns the <code>Course</code> taught by the indicated instructor.
   * This is an example of a finder method that returns a single
   * entity.  If no courses match or if multiple classes match, the find
   * method will throw an exception.
   *
   * <p>The return type is the local interface of the bean.
   * Find methods always return a single instance of the local
   * interface or a collection of the local interfaces.  Applications
   * which need to return other entity bean interfaces or values must
   * use ejbSelect methods in the bean implementation.
   *
   * <p>The find method's query is specified in the deployment descriptor
   * in the &lt;query> tag using EJB-QL.  "?1" refers to the first
   * method argument.  find_courses is the abstract-schema-name in the
   * deployment descriptor.  This may differ from the actual SQL table
   * if sql-table-name has been specified.
   *
   * <code><pre>
   * SELECT o FROM find_courses o WHERE o.instructor = ?1
   * </pre></code>
   *
   * <p>Resin-CMP will generate the code and SQL for the find method.
   *
   * @param instructorName name of the instructor who teaches
   * the <code>Course</code>
   * we want to find.
   *
   * @exception ObjectNotFoundException if there is no matching course.
   * @exception FinderException if there are more than one matching courses.
   */
  public Course findByInstructor(String instructorName)
    throws FinderException;

  /**
   * Returns a Collection of all <code>Course</code> entities in the database.
   * This is an example of a finder method that returns a Collection of
   * entities.
   *
   * <p>Resin-CMP will implement this method. All we have to provide is this
   * declaration, and a <code>&lt;query></code> section in the deployment
   * descriptor.
   *
   * <code><pre>
   * SELECT o FROM find_courses o
   * </pre></code>
   */
  public Collection findAll()
    throws FinderException;

  /**
   * Returns the <code>Course</code> that has <code>courseId</code>
   * as its primary key.
   *
   * <p>Every entity EJB needs to define this finder method that looks for an
   * entity based on the primary key.
   *
   * @param courseId the primary key of the course
   * @return the matching course
   *
   * @exception ObjectNotFoundException if there is no course matching the key.
   */
  Course findByPrimaryKey(String courseId)
    throws FinderException;
}
