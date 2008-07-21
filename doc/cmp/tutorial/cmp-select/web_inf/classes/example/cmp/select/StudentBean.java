package example.cmp.select;

import java.util.Collection;

/**
 * Implementation class for the Student bean.
 *
 * <code><pre>
 * CREATE TABLE select_student (
 *   name VARCHAR(250) NOT NULL,
 *   gender VARCHAR(6) NOT NULL,
 *   house VARCHAR(250) NOT NULL,
 * 
 *   PRIMARY KEY(name)
 * );
 * </pre></code>
 *
 * </p>StudentBean is abstract since it's taking advantage of container-managed
 * persistence.  Resin-CMP will create the implementation of the abstract
 * methods.
 *
 * <p/>StudentBean also takes advantage of the AbstractEntityBean
 * implementation.  AbstractEntityBean is just a stub
 * EntityBean implementation with default methods to make life
 * a little more sane for simple beans.
 */
abstract public class StudentBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns the name of the student (CMP field). The name is also the primary
   * key as defined in the deployment descriptor.
   */
  abstract public String getName();

  /**
   * Returns the gender of the student (CMP field).
   */
  abstract public String getGender();
  /**
   * Sets the gender of the student (CMP field).
   */
  abstract public void setGender(String gender);

  /**
   * returns the <code>House</code> that this Student belongs to (CMR field).
   */
  abstract public House getHouse();

  /**
   * sets the <code>House</code> that this Student is to belong to (CMR field).
   *
   * @param house new House that this Student will belong to.
   */
  abstract public void setHouse(House house);
}
