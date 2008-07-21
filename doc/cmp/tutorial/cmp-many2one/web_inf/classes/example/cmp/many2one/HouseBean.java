package example.cmp.many2one;

import java.util.*;
/**
 * Implementation of the HouseBean.  Each instance of HouseBean
 * maps to a table entry of "student_house", where student_house is defined as
 *
 * <pre>
 * CREATE TABLE student_house (
 *   name VARCHAR(250),
 *   points INTEGER
 * )
 * </pre>
 *
 * <p/>HouseBean is abstract since it's taking advantage of container-managed
 * persistence.  Resin-EJB will create the implementation of the abstract
 * methods.
 *
 * <p/>HouseBean also takes advantage of the AbstractEntityBean implementation.
 * AbstractEntityBean is just a stub EntityBean implementation with default
 * methods to make life a little more sane for simple beans.
 */
abstract public class HouseBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns the house name.  The name is the primary key.
   */
  abstract public String getName();
  /**
   * Returns the number of points for the house.
   */
  abstract public int getPoints();
  /**
   * Returns a collection of the students.
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
  /**
   * Return a new ArrayList of the students since entity beans can't
   * directly return the persistent collection.
   */
  public Collection getStudents()
  {
    return new ArrayList(getStudentList());
  }
}
