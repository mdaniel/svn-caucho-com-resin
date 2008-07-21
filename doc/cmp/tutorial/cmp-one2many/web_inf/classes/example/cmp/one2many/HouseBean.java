package example.cmp.one2many;

import java.util.*;

/**
 * Implementation class for the House bean.
 *
 * Each instance of StudentBean maps to a table entry of "one2many_houses",
 * where student is defined.
 *
 * <p>HouseBean is abstract since it's taking advantage of container-managed
 * persistence.  Resin-CMP will create the implementation of the abstract
 * methods.
 *
 * <p>HouseBean also takes advantage of the AbstractEntityBean
 * implementation.  AbstractEntityBean is just a stub
 * EntityBean implementation with default methods to make life
 * a little more sane for simple beans.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <code><pre>
 *  CREATE TABLE one2many_houses (
 *     name VARCHAR(250) NOT NULL,
 *
 *     PRIMARY KEY(name)
 *   );
 * </pre></code>
 */
abstract public class HouseBean extends com.caucho.ejb.AbstractEntityBean {

  /**
   * returns the name of the house (CMP field). The name is the primary key as defined
   * in the deployment descriptor.
   */
  abstract public String getName();

  /**
   * returns a <code>Collection</code> of all <code>Students</code>s managed by
   * the container (CMR field).
   * <p>This method needs to exist
   * because the field <code>studentList</code> is defined as a CMR field.
   */
  abstract public Collection getStudentList();

  /**
   * Adds a student to the house.  If the student is already a member
   * of another house, he will be removed from that house automatically.
   */
  public void addStudent(Student student)
  {
    getStudentList().add(student);
  }

  /**
   * Removes a student from the house.
   */
  public void removeStudent(Student student)
  {
    getStudentList().remove(student);
  }

}
