package example.cmp.many2many;

import java.util.*;

/**
 * Implementation class for the Student bean.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <pre><code>
 *  CREATE TABLE many2many_students (
 *    name VARCHAR(250) NOT NULL,
 *
 *    PRIMARY KEY(name)
 *   );
 * </code></pre>
 */
abstract public class StudentBean extends com.caucho.ejb.AbstractEntityBean {

  /**
   * Returns the name of the student (CMP field).
   * The name is also the primary key.
   */
  abstract public String getName();

  /**
   * returns a <code>Collection</code> of all <code>Course</code>s the
   * <code>Student</code> is currently enrolled in.
   */
  abstract public Collection getCourseList();

  /**
   * a little helper to enroll students in a <code>Course</code>
   */
  public void addCourse(Course course)
  {
    this.getCourseList().add( course );
  }

  /**
   * a little helper to drop a <code>Course</code>
   */
  public void removeCourse(Course course)
  {
    this.getCourseList().remove( course );
  }
}
