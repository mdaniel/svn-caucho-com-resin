package example.cmp.basic;

import javax.ejb.*;

/**
 * Local interface for a course taught at Hogwarts, providing
 * methods to view and change it.
 *
 * <code><pre>
 * CREATE TABLE basic_courses (
 *   course_id VARCHAR(250) NOT NULL,
 *   teacher VARCHAR(250),
 * 
 *   PRIMARY KEY(course_id)
 * );
 * </pre></code>
 *
 * <p>All Course instances are obtained from the CourseHome interface, and
 * all access to the Course table occurs through the Course interface.
 * Clients can never use the CourseBean implementation directly or directly
 * access the database for the course table.  Because Resin-CMP has
 * complete control over the database's course entries, it can more
 * effectively cache results, often avoiding slow database access.
 *
 * <p>CourseId is the bean's primary key.  Every entity bean must have
 * a primary key.
 *
 * There is a <code>setInstructor</code> mutator, but no
 * <code>setCourseId</code> mutator because clients can't set the primary key.
 * Resin-CMP needs to implement the <code>
 * setCourseId</code> method that has been defined in the implementation
 * class,<code>CourseBean.java</code>. But the Implementation Class is
 * not associated with any particular entities -- it is only used by the
 * container to implement the Local Interface. If you want to use a method from
 * the implementation class when calling from a client, it has to be made
 * available explicitly in the Local Interface.
 *
 */
public interface Course extends EJBLocalObject {
  /**
   * Returns the ID of the course (CMP field). This is also the primary
   * key as defined in ejb-jar.xml.  There's no corresponding setCourseId
   * method in the local interface because clients must not change the
   * primary key.
   */
  public String getId();

  /**
   * Returns the instructor's name (CMP field).  More sophisticated
   * applications will create a separate entity bean for the instructor
   * instead of using a string.  The more sophisticated application will
   * use a relationship field to manage the instructor for a course.
   *
   * @see example.cmp.one2one
   */
  public String getInstructor();

  /**
   * Sets the instructor's name (CMP field).
   *
   * @param instructor the name of the new instructor.
   */
  public void setInstructor(String instructor);
  
  /**
   * Swaps the instructor for a course.  This business method will run in
   * a transaction, like all business methods in an entity bean.
   * The transaction protects the database from inconsistency when
   * several clients try to modify the database simultaneously.
   *
   * @param course the course which will swap instructors.
   */
  public void swap(Course course);
}
