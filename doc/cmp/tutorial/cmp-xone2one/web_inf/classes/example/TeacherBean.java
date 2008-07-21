package example;

/**
 * Implementation class for the Teacher bean.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <pre><code>
 *  CREATE TABLE xone2one_teacher(
 *    name VARCHAR(250) NOT NULL,
 *    course VARCHAR(250) NOT NULL,
 *
 *    PRIMARY KEY(name)
 *  );
 * </code></pre>
 *
 * @ejb.bean name="teacher" view-type="local" type="CMP"
 *           reentrant="False" schema="courses" primkey-field="name"
 * @ejb.pk class="java.lang.String"
 * @ejb.home generate="local" local-class="example.TeacherHome"
 * @ejb.interface generate="local" local-class="example.Teacher"
 * @ejb.persistence table-name="xone2one_teachers"
 */
abstract public class TeacherBean extends com.caucho.ejb.AbstractEntityBean {

  /**
   * returns the <code>Teacher</code>'s name
   *
   * @ejb.interface-method
   * @ejb.persistence column-name="name"
   * @ejb.pk-field
   */
  abstract public String getName();

  /**
   * returns the <code>Course</code> taught by the <code>Teacher</code>
   *
   * @ejb.interface-method
   * @ejb.relation name="course-teacher"
   */
  abstract public Course getCourse();
}
