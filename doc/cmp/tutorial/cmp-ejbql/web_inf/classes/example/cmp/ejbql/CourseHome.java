package example.cmp.ejbql;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBHome;
import javax.ejb.FinderException;

import java.util.Collection;

/**
 * Home interface for the Course bean.
 * The Home Interface enables you to create new entities and to obtain
 * references to existing ones.
 *
 * <p>The CourseHome example provides two finder methods: the standard
 * findByPrimaryKey and a findByHouse method.  Find methods for a local
 * home always return the local interface or a collection of
 * the local interface.
 *
 * <p/>All find methods except findByPrimaryKey need an EJB-QL query in
 * the deployement descriptor.
 */
public interface CourseHome extends EJBLocalHome {
  /**
   * This is an example of a finder method that returns a single entity if
   * successful, and throws an <code>ObjectNotFoundException</code> if it
   * was unsuccessful.
   * Every entity EJB needs to define this finder method that looks for an
   * entity based on the primary key.
   */
  Course findByPrimaryKey(String primaryKey)
    throws FinderException;

  /**
   * Finds all the courses for the students living in a house.  The example
   * methods shows how a query can use the IN(...) expression to select
   * a collection of entities.
   *
   * <code><pre>
   * SELECT DISTINCT OBJECT(course)
   * FROM ejbql_student student, IN(student.courseList) course
   * WHERE student.house.name=?1
   * </pre></code>
   *
   * @param house the name of the student's house.
   *
   * @return the matching collection of courses.
   */
  Collection findByHouse(String house)
    throws FinderException;
}
