package example.cmp.map;

/**
 * Implementation class for the Course bean.
 *
 * <p>Each instance of CourseBean maps to a table entry of "map_courses".
 *
 * <p>CourseBean is abstract since it's taking advantage of container-managed
 * persistence.  Resin-CMP will create the implementation of the abstract
 * methods.
 *
 * <p>CourseBean also takes advantage of the AbstractEntityBean
 * implementation.  AbstractEntityBean is just a stub
 * EntityBean implementation with default methods to make life
 * a little more sane for simple beans.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <code><pre>
 *    CREATE TABLE map_courses (
 *     name VARCHAR(250) NOT NULL,
 *
 *     PRIMARY KEY(name)
 *   );
 * </pre></code>
 */
abstract public class CourseBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns the name of the course. The name is also the primary
   * key as defined in the deployment descriptor.
   */
  abstract public String getName();
}
