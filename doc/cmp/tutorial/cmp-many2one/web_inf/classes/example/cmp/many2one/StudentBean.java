package example.cmp.many2one;

/**
 * Implementation of the StudentBean.  Each instance of StudentBean
 * maps to a table entry of "student", where student is defined as
 *
 * <pre>
 * CREATE TABLE student_house (
 *   name VARCHAR(250) NOT NULL,
 *   house VARCHAR(250),
 *
 *   PRIMARY KEY(name)
 * )
 * </pre>
 *
 * <p/>StudentBean is abstract since it's taking advantage of container-managed
 * persistence.  Resin-EJB will create the implementation of the abstract
 * methods.
 *
 * <p/>StudentBean also takes advantage of the AbstractEntityBean
 * implementation.  AbstractEntityBean is just a stub
 * EntityBean implementation with default methods to make life
 * a little more sane for simple beans.
 */
abstract public class StudentBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns the student name.  The name is the primary key.
   */
  abstract public String getName();
  /**
   * Returns the house the student belongs to.
   */
  abstract public House getHouse();
  /**
   * Sets the student's house.
   */
  abstract public void setHouse(House house);
}
