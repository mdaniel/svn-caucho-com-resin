package example.cmp.map;

/**
 * Interface for the Grade bean.
 *
 * <p>The Grade has a compound key with two identifying fields: the
 * Student and the Course.
 */
public interface Grade extends javax.ejb.EJBLocalObject {
  /**
   * Returns the Grade's student.  This is part of the primary key.
   */
  public Student getStudent();
  /**
   * Returns the Grade's course.  This is part of the primary key.
   */
  public Course getCourse();
  /**
   * Returns the grade.
   */
  public String getGrade();
}

