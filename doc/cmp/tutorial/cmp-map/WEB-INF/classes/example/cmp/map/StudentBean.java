package example.cmp.map;

import java.util.Map;
/**
 * Implementation class for the Student bean.
 *
 * <p>Each instance of StudentBean maps to a table entry of "map_students".
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
 *    CREATE TABLE map_students (
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
   * Returns a map of the student's grades.
   */
  abstract public Map getGrades();
}
