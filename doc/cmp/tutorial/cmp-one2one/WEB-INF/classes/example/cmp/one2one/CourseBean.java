package example.cmp.one2one;

import java.util.*;

/**
 * Implementation class for the Course bean.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <pre><code>
 *  CREATE TABLE one2one_teacher (
 *    name VARCHAR(250) NOT NULL,
 *    course VARCHAR(250) NOT NULL,
 *
 *    PRIMARY KEY(name)
 * );
 * </code></pre>
 */
abstract public class CourseBean extends com.caucho.ejb.AbstractEntityBean {

  /**
   * Returns the name of the course (CMP field). This method will be
   * implemented by Resin-CMP.
   * It is also the primary key as defined in the deployment descriptor.
   */
  abstract public String getName();

  /**
   * Returns the Teacher who is teaching this Course (CMR field).
   * Resin-CMP will implement this method.
   */
  abstract public Teacher getTeacher();
}
