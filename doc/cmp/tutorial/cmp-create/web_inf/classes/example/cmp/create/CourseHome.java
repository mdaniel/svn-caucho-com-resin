package example.cmp.create;

import javax.ejb.*;

import java.util.Collection;

/**
 * Home interface for the Course bean.
 *
 * The Home Interface enables you to create new entities and to obtain
 * references to existing ones.
 *
 * <p>The idea is that you use the Home Interface to obtain references
 * to whatever entities you're interested in. Each entity that you
 * get from the Home Interface (using its create or finder methods)
 * is then represented by its Remote Interface.
 *
 * <p>With this Remote Interface, you can obtain information about a particular
 * course, but you cannot change it. The Remote Interface is your only
 * point of access to an entity, and there are no setXXX methods in this
 * example.
 */
public interface CourseHome extends EJBLocalHome {
  /**
   * returns the <code>Course</code> that has <code>courseId</code>
   * as its primary key.
   * Every entity EJB needs to have a finder method that returns an entity
   * based on the primary key.
   *
   * @param courseId id and name of the course that is to be retreived
   */
  Course findByPrimaryKey(String courseId)
    throws FinderException;

  /**
   * returns a <code>Collection</code> of all courses.
   */
  Collection findAll()
    throws FinderException;

  /**
   * create a new course entity-
   * <p The home interface allows us to create new entities through
   * one of its <code>create</code> methods. The container will implement
   * the <code>create</code> methods for us, based on code that we write in
   * our implementation class (CourseHome.java). For each <code>create</code>
   * method in this home interface, there needs to be a corresponding
   * <code>ejbCreate</code> method in CourseHome.java that has the same
   * parameters and doesn't throw <code>CreateException</code>.
   */
  public Course create(String courseId, String instructor)
    throws CreateException;
}
