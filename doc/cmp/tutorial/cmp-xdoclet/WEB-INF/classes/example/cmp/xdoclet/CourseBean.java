package example.cmp.xdoclet;

import java.util.*;

/**
 * Implementation class for the Course bean.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <code><pre>
 *  CREATE TABLE xdoclet_courses (
 *    id VARCHAR(250) NOT NULL,
 *    instructor VARCHAR(250),
 *
 *    PRIMARY KEY(name)
 *   );
 * </pre></code>
 *
 * @ejb:bean name="xdoclet_CourseBean" view-type="local" type="CMP"
 *  reentrant="False" schema="courses" primkey-field="name"
 * @ejb:pk class="java.lang.String"
 * @ejb:home generate="local" local-class="example.cmp.xdoclet.CourseHome"
 * @ejb:interface generate="local" local-class="example.cmp.xdoclet.Course"
 *
 * @ejb:finder signature="java.util.Collection findByStudent(java.lang.String student)"
 *  query="SELECT c FROM students student, IN(student.courseList) c WHERE student.name = ?1"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *  query="SELECT c FROM courses c ORDER BY c.name"
 *
 * @ejb:select signature="java.util.Collection ejbSelectAllInstructors()"
 *  query="SELECT c.instructor FROM courses c 
 *    WHERE c.instructor IS NOT NULL 
 *    ORDER BY c.instructor"
 *
 * @resin-ejb:entity-bean sql-table="xdoclet_courses"
 */
abstract public class CourseBean extends com.caucho.ejb.AbstractEntityBean
{
  /**
   * A Course is identified by its name.
   *
   * @ejb:interface-method
   * @ejb:persistent-field
   * @ejb:pk-field
   *
   * @resin-ejb:cmp-field sql-column="id"
   */
  abstract public String getName();

  /**
   * Returns the name of this Course's instructor.
   *
   * @ejb:interface-method
   * @ejb:persistent-field
   */
  abstract public String getInstructor();

  /**
   * Returns a Collection of Students enrolled in this Course.
   *
   * @ejb:interface-method
   * @ejb:relation name="xdoclet_enrollment" role-name="course"
   *
   * @resin-ejb:relation sql-table="xdoclet_enrollment" sql-column="course"
   */
  abstract public Collection getStudentList();

  /**
   * Enrolls a Student is this Course.
   *
   * @ejb:interface-method
   */
  public void addStudent( Student student )
  {
    getStudentList().add( student );
  }

  /**
   * Drops a Student from this Course.
   *
   * @ejb:interface-method
   */
  public void removeStudent( Student student )
  {
    getStudentList().remove( student );
  }
  
  /**
   * Returns the names of all instructors in alphabetical order.
   */
  abstract public Collection ejbSelectAllInstructors()
  throws javax.ejb.FinderException;
  
  /**
   * Returns the names of all instructors in alphabetical order.
   *
   * @ejb:home-method view-type="local"
   */
  public Collection ejbHomeListAllInstructors()
  throws javax.ejb.FinderException
  {
    return ejbSelectAllInstructors();
  }
}
