package example.cmp.transaction;

import javax.ejb.*;
import java.util.Collection;

/**
 * Local Interface for the RegistrationSession bean.
 */
public interface RegistrationSession extends EJBLocalObject {

  /**
   * Adds a <code>Course</code> to the list of selected courses. Note that this
   * method does store the <code>Course</code> to the database.
   */
  public void addCourse(Course course)
    throws FinderException;
  /**
   * Removes a <code>Course</code> from the list of selected courses. Note that
   * this method does not delete any records from the database.
   */
  public void removeCourse(Course course)
    throws FinderException;
  /**
   * Returns a <code>Collection</code> of all available Courses.
   */
  public Collection getAvailableCourses();
  /**
   * Returns a <code>Collection</code> of all Courses that are currently
   * selected in this RegistrationSession Bean.
   */
  public Collection getSelectedCourses();
  /**
   * Returns a <code>Collection</code> of all Courses that the Student
   * currently enrolled in.
   */
  public Collection getEnrolledCourses();
  /**
   * Returns the name of the Student who is currently selecting Courses.
   */
  public String getStudentName();
  /**
   * Returns true if the registration is complete.
   */
  public boolean isComplete();
  /**
   * Consitutes a transaction that tries to commit every course in the
   * <code>RegistrationSessionBean</code>'s list of selected courses to the
   * database.
   */
  public void finalizeRegistration()
    throws RegistrationDeniedException;

}
