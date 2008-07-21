package example.cmp.map;

import javax.ejb.*;

/**
 * Implementation class for the Grade bean.
 *
 * <p>The Grade has a compound key with two identifying fields: the
 * Student and the Course.
 *
 * <p>This CMP bean uses the following schema:
 *
 * <code><pre>
 *    CREATE TABLE map_grades (
 *     student VARCHAR(250) NOT NULL REFERENCES map_students(name),
 *     course VARCHAR(250) NOT NULL REFERENCES map_courses(name),
 *
 *     grade VARCHAR(3),
 *
 *     PRIMARY KEY(student, course)
 *   );
 * </pre></code>
 */
abstract public class GradeBean extends com.caucho.ejb.AbstractEntityBean {
  /**
   * Returns the Grade's student.  This is part of the primary key.
   */
  abstract public Student getStudent();
  /**
   * Sets the Grade's student.  Since this is part of the primary
   * key, it it only set in ejbCreate.
   */
  abstract public void setStudent(Student student);
  /**
   * Returns the Grade's course.  This is part of the primary key.
   */
  abstract public Course getCourse();
  /**
   * Sets the Grade's course.  Since this is part of the primary
   * key, it it only set in ejbCreate.
   */
  abstract public void setCourse(Course course);
  /**
   * Returns the grade.
   */
  abstract public String getGrade();
  /**
   * Sets the grade.
   */
  abstract public void setGrade(String grade);

  /**
   * Create a new grade object.
   */
  public GradeKey ejbCreate(Student student, Course course, String grade)
    throws CreateException
  {
    setStudent(student);
    setCourse(course);
    setGrade(grade);

    return null;
  }
  
  /**
   * Create a new grade object.
   */
  public void ejbPostCreate(Student student, Course course, String grade)
    throws CreateException
  {
    setStudent(student);
    setCourse(course);
    setGrade(grade);
  }
}

