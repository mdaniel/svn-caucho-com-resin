package example.cmp.one2one;

/**
 * Implementation class for the Teacher bean.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <pre><code>
 *  CREATE TABLE one2one_course(
 *    name VARCHAR(250) NOT NULL,
 *    room VARCHAR(250) NOT NULL,
 *    teacher VARCHAR(250) NOT NULL,
 *
 *    PRIMARY KEY(name)
 *  );
 * </code></pre>
 */
abstract public class TeacherBean extends com.caucho.ejb.AbstractEntityBean {

  /**
   * returns the <code>Teacher</code>'s name
   */
  abstract public String getName();

  /**
   * returns the <code>Course</code> taught by the <code>Teacher</code>
   */
  abstract public Course getCourse();

//  abstract public void setCourse(Course course);

}
