package example.cmp.transaction;

import java.util.Collection;
import javax.ejb.*;

/**
 * Implementation class for the Student bean.
 *
 * <p>StudentBean is abstract since it's taking advantage of container-managed
 * persistence.  Resin-CMP will create the implementation of the abstract
 * methods.
 *
 * <p>StudentBean also takes advantage of the AbstractEntityBean
 * implementation.  AbstractEntityBean is just a stub
 * EntityBean implementation with default methods to make life
 * a little more sane for simple beans.
 */
abstract public class StudentBean extends com.caucho.ejb.AbstractEntityBean {

  /**
   * Returns the student ID (which is also the primary key) of this student
   * (CMP field).
   */
  abstract public int getId();
  /**
   * Sets the student ID (which is also the primary key) of this student
   * (CMP field).
   */
  abstract public void setId(int id);
  /**
   * Returns the name of the student (CMP field). The name is also the primary
   * key as defined in the deployment descriptor.
   */
  abstract public String getName();
  /**
   * Sets the name of the student.
   */
  abstract public void setName(String name);
  /**
   * Returns the gender of the student (CMP field).
   */
  abstract public String getGender();
  /**
   * Returns the password associated with this student.
   */
  abstract public String getPassword();
  /**
   * Sets the gender of the student (CMP field).
   */
  abstract public void setGender(String gender);
  /**
   * Returns a <code>Collection</code> of all Courses that the student is
   * currently enrolled in (CMR field).
   */
  abstract public Collection getCourseList();
}
