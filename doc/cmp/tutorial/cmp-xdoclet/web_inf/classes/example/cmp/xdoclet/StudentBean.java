package example.cmp.xdoclet;

import java.util.*;

/**
 * Implementation class for the Student bean.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <pre><code>
 *  CREATE TABLE xdoclet_students (
 *    name VARCHAR(250) NOT NULL,
 *
 *    PRIMARY KEY(name)
 *   );
 * </code></pre>
 *
 * @ejb:bean name="xdoclet_StudentBean" view-type="local" type="CMP"
 *  reentrant="False" schema="students" primkey-field="name"
 * @ejb:pk class="java.lang.String"
 * @ejb:home generate="local" local-class="example.cmp.xdoclet.StudentHome"
 * @ejb:interface generate="local" local-class="example.cmp.xdoclet.Student"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *  query="SELECT s FROM students s ORDER BY s.name"
 *
 * @ejb:finder signature="java.util.Collection findByCourse(java.lang.String course)"
 *  query="SELECT s FROM courses course, IN(course.studentList) s WHERE course.name = ?1"
 *
 * @resin-ejb:entity-bean sql-table="xdoclet_students"
 */
abstract public class StudentBean extends com.caucho.ejb.AbstractEntityBean
{
  /**
   * A Student is identified by name.
   *
   * @ejb:interface-method
   * @ejb:persistent-field
   * @ejb:pk-field
   */
  abstract public String getName();

  /**
   * Returns a Collection of Courses which this Student is taking.
   *
   * @ejb:interface-method
   * @ejb:relation name="xdoclet_enrollment" role-name="student"
   *
   * @resin-ejb:relation sql-column="student"
   */
  abstract public Collection getCourseList();
  
  /**
   * Enrolls this Student in a Course.
   *
   * @ejb:interface-method
   */
  public void addCourse(Course course)
  {
    getCourseList().add(course);
  }
  
  /**
   * Drops this Student from a Course.
   *
   * @ejb:interface-method
   */
  public void removeCourse(Course course)
  {
    getCourseList().remove(course);
  }
}
