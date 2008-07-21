package example.cmp.id;

import javax.ejb.CreateException;

/**
 * Implementation class for the Quidditch bean.
 *
 * <p>Each instance of QuidditchBean maps to a table entry of "id_quidditch".
 *
 * <p>The Quidditch entry is tied to a Student, using the Student as
 * its primary key.
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
 * CREATE TABLE id_quidditch (
 *   student VARCHAR(250) NOT NULL REFERENCES id_student(name),
 *
 *   position VARCHAR(250),
 *   points INTEGER,
 *
 *   PRIMARY KEY(student)
 * );
 * </pre></code>
 */
abstract public class QuidditchBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns the owning student. The student is also the primary
   * key.
   */
  abstract public Student getStudent();
  /**
   * Sets the owning student. Since the student is the primary key,
   * this method is only called from the ejbCreate method.
   */
  abstract public void setStudent(Student student);
  /**
   * Returns the position the student plays on the team.
   */
  abstract public String getPosition();
  /**
   * Sets the position the student plays on the team.
   */
  abstract public void setPosition(String position);
  /**
   * Returns the number of points the student has earned.
   */
  abstract public int getPoints();
  /**
   * Sets the number of points the student has earned.
   */
  abstract public void setPoints(int points);
  /**
   * Creates the student's scores, setting primary keys and fields.
   */
  public Student ejbCreate(Student student, String position)
    throws CreateException
  {
    setStudent(student);
    setPosition(position);

    return student;
  }
  /**
   * Sets any relations.  This case has no relations.
   */
  public void ejbPostCreate(Student student, String position)
  {
  }
}
