package example.cmp.many2many;

import java.util.*;

/**
 * Implementation class for the Course bean.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <code><pre>
 *  CREATE TABLE many2many_courses (
 *    name VARCHAR(250) NOT NULL,
 *    instructor VARCHAR(250),
 *
 *    PRIMARY KEY(name)
 *   );
 * </pre></code>
 */
abstract public class CourseBean extends com.caucho.ejb.AbstractEntityBean {

  /**
   * Returns the name of the <code>Course</code> (CMP field). This method will
   * be implemented by Resin-CMP.
   * It is also the primary key as defined in the deployment descriptor.
   */
  abstract public String getName();

  /**
   * Returns the name of the instructor teaching the <code>Course</course>
   * (CMP field).
   * Resin-CMP will implement this method.
   */
  abstract public String getInstructor();

  /**
   * Returns a <code>Collection</code> of all Students that are taking this
   * course (CMR field).
   * Resin-CMP will implement this method.
   */
  abstract public Collection getStudentList();

  /**
   * Adds a <code>Student</code> to the <code>Course</course>. This will update
   * the table many2many_student_course_mapping as defined in the
   * deployment descriptor.
   */
  public void addStudent( Student student )
  {
    this.getStudentList().add( student );
  }

  /**
   * Removes a <code>Student</code> from the <code>Course</course>. This will
   * update the table many2many_student_course_mapping as defined in the
   * deployment descriptor.
   */
  public void removeStudent( Student student )
  {
    this.getStudentList().remove( student );
  }
}
