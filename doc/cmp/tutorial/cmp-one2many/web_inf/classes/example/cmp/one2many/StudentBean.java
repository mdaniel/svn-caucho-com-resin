package example.cmp.one2many;

/**
 * Implementation class for the Student bean.
 *
 * <p>Each instance of StudentBean
 * maps to a table entry of "one2many_students", where student is defined.
 *
 * <p>StudentBean is abstract since it's taking advantage of container-managed
 * persistence.  Resin-CMP will create the implementation of the abstract
 * methods.
 *
 * <p>StudentBean also takes advantage of the AbstractEntityBean
 * implementation.  AbstractEntityBean is just a stub
 * EntityBean implementation with default methods to make life
 * a little more sane for simple beans.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <code><pre>
 *    CREATE TABLE one2many_students (
 *     name VARCHAR(250) NOT NULL,
 *     house VARCHAR(250),
 *
 *     PRIMARY KEY(name)
 *   );
 * </pre></code>
 */
abstract public class StudentBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns the name of the student (CMP field). The name is also the primary
   * key as defined in the deployment descriptor.
   */
  abstract public String getName();
  /**
   * Returns the <code>House</code> that the student belongs to (CMR field).
   * <p>This method needs to exist
   * because the field <code>house</code> is defined as a CMR field.
   */
  abstract public House getHouse();
  /**
   * Sets the <code>House</code> that the student belongs to (CMR field).
   * This method needs to exist
   * because the field <code>house</code> is defined as a CMR field.
   */
  abstract public void setHouse(House house);
}
