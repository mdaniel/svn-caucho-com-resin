package example.cmp.transaction;

import javax.ejb.*;
import javax.naming.*;
import java.util.*;

import com.caucho.ejb.AbstractSessionBean;

/**
 * Implementation Class for the RegistrationSession bean.
 */
public class RegistrationSessionBean extends AbstractSessionBean
  implements SessionSynchronization {

  /**
   * Home Interface for the Course CMP bean.
   */
  private CourseHome courseHome = null;

  /**
   * Home Interface for the Student CMP bean.
   */
  private StudentHome studentHome = null;

  /**
   * The calling Principal.
   */
  private Student student;

  /**
   * True for the finalizing method.
   */
  private boolean isCompleting;
  /**
   * True when the registration has completed and mail has been sent.
   */
  private boolean isComplete;

  /**
   * Currently selected Courses. Note that these are in-memory only and will
   * not be committed to the database until <code>finalizeRegistration</code>
   * is called.
   */
  private Collection selectedCourses = new ArrayList(20);

  public static final int TRANSACTION_COMMITTED = 0;
  public static final int TRANSACTION_ROLLEDBACK = 1;

  /**
   * Standard Constructor.
   */
  public RegistrationSessionBean()
  {
  }

  /**
   * Tries to resolve the calling <code<Principal</code>.
   */
  public void ejbCreate()
    throws CreateException
  {
    try {
      // The JNDI context containing EJBs
      Context ejb = (Context) new InitialContext().lookup("java:comp/env/cmp");

      // get the bean stubs
      courseHome = (CourseHome) ejb.lookup("transaction_course");
      studentHome = (StudentHome) ejb.lookup("transaction_student");
    } catch (Exception e) {
      throw new CreateException("can't initialize home interfaces\n" + e);
    }

    student = (Student) getSessionContext().getCallerPrincipal();
    if (student == null)
      throw new CreateException("Cannot create " + getClass().getName() +
        ": caller is not authenticated. You need to authenticate yourself " +
        "through Resin's security framework." );
  }

  /**
   * Called by Resin-CMP after a new transaction has begun -- required by the
   * SessionSynchronization interface.
   */
  public void afterBegin()
    throws EJBException
  {
  }

  /**
   * Called by Resin-CMP just before a transaction is committed --
   * required by the SessionSynchronization interface.
   *
   * <p>This call might update the database or do some validation.
   * Because the transaction might still be rolled back, it should be
   * possible to roll back any operatin in the before completion.
   */
  public void beforeCompletion()
    throws EJBException
  {
  }

  /**
   * Called by Resin-CMP when a transaction has completed -- required by the
   * SessionSynchronization interface.
   *
   * <p>In order to keep the transaction Atomic, Consistent, Independent, and
   * Durable, we do nothing in this method unless the transaction has been
   * committed.
   *
   * <p>If the transaction was rolled back, we will treat it as if it never
   * happened.
   */
  public void afterCompletion(boolean committed)
    throws EJBException
  {
    if (committed && isCompleting) {
      // Normally, an application would do something like send a
      // confirmation email.  Here we'll just set a flag.
      isComplete = true;
    }

    isCompleting = false;
  }

  /**
   * Returns the name of the Student who is selecting Courses.
   */
  public String getStudentName()
  {
    if (student == null)
      return "Anonymous";
    else
      return student.getName();
  }

  /**
   * Returns a <code>Collection</code> of Courses that the now registering
   * Student is currently enrolled in.
   */
  public Collection getEnrolledCourses()
  {
    if (student == null)
      return new ArrayList();
    else
      return student.getCourseList();
  }

  /**
   * Adds a course to the set of selected courses for this session. Note that
   * this method does not commit data to the database.
   */
  public void addCourse(Course course)
    throws FinderException
  {
    if (! selectedCourses.contains(course))
      selectedCourses.add(course);
  }

  /**
   * Deletes a course from the set of selected courses for this session.
   */
  public void removeCourse(Course course)
    throws FinderException
  {
    if (selectedCourses.contains(course))
      selectedCourses.remove(course);
  }

  /**
   * Returns a <code>Collection</code> of all Courses offered
   * (including those that the student has already selected).
   */
  public Collection getAvailableCourses()
  {
    Collection availableCourses = null;
    try {
      availableCourses = courseHome.findAll();
    } catch(FinderException e) {
    }
    return availableCourses;
  }

  /**
   * Returns a <code>Collection</code> of all courses currently selected.
   */
  public Collection getSelectedCourses() {
    return this.selectedCourses;
  }

  /**
   * Returns true if the registration is complete.
   */
  public boolean isComplete()
  {
    return isComplete;
  }

  /**
   * Executes a transaction that will commit the selected courses to the
   * persistant store unless an error occurs.
   */
  public void finalizeRegistration()
    throws RegistrationDeniedException
  {
    student.getCourseList().clear();

    isCompleting = true;

    Iterator iter = selectedCourses.iterator();
    while (iter.hasNext()) {
      Course course = (Course)iter.next();
      if (course.isFull()) {
        getSessionContext().setRollbackOnly();
        throw new RegistrationDeniedException (
          course.getName() + " is full. Please drop this class and try again.");
      }
      else if (! student.getCourseList().contains(course))
        student.getCourseList().add(course);
    }
  }
}
