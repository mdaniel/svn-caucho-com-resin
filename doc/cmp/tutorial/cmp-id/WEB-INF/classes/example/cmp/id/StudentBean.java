package example.cmp.id;

/**
 * Implementation class for the Student bean.
 *
 * <p>Each instance of StudentBean maps to a table entry of "id_students".
 *
 * <p>Each Student may have an associated Quidditch entry if the
 * Student is on the house team.  Since the Quidditch entry is
 * an identifying relation, there is no corresponding entry in the SQL.
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
 *    CREATE TABLE id_students (
 *     name VARCHAR(250) NOT NULL,
 *
 *     PRIMARY KEY(name)
 *   );
 * </pre></code>
 */
abstract public class StudentBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns the name of the student. The name is also the primary
   * key as defined in the deployment descriptor.
   */
  abstract public String getName();
  /**
   * Returns the student's <code>Quidditch</code> statistics, if the
   * student is on the house team.
   */
  abstract public Quidditch getQuidditch();
}
