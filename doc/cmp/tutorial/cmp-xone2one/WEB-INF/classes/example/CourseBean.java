package example;

import java.util.*;

/**
 * Implementation class for the Course bean.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <pre><code>
 *  CREATE TABLE xone2one_course (
 *    name VARCHAR(250) NOT NULL,
 *    teacher VARCHAR(250) NOT NULL,
 *
 *    PRIMARY KEY(name)
 * );
 * </code></pre>
 *
 * @ejb.bean name="course" view-type="local" type="CMP"
 *           schema="courses" primkey-field="name"
 * @ejb.pk class="java.lang.String"
 * @ejb.home generate="local" local-class="example.CourseHome"
 * @ejb.interface generate="local" local-class="example.Course"
 *
 * @ejb.finder signature="java.util.Collection findAll()"
 *             query="SELECT o FROM courses o"
 *
 * @ejb.persistence table-name="xone2one_courses"
 */
abstract public class CourseBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns the name of the course.
   * It is also the primary key as defined in the deployment descriptor.
   *
   * @ejb.interface-method
   * @ejb.persistence column-name="name"
   * @ejb.pk-field
   *
   * @return the course id
   */
  abstract public String getName();

  /**
   * Returns the Teacher who is teaching this Course.
   *
   * @ejb.interface-method
   * @ejb.relation name="course-teacher"
   */
  abstract public Teacher getTeacher();
}
