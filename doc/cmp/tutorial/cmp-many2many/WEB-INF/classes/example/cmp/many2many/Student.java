package example.cmp.many2many;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

import java.util.*;

/**
 * Local interface for the Student bean.
 */
public interface Student extends EJBLocalObject {

  /**
   * Returns the student's name (CMP field).
   */
  String getName();

  /**
   * Returns a <code>Collection</code> of the student's courses (CMR field)
   */
  Collection getCourseList();

  /**
   * Enrolls the <code>Student</code> in a <code>Course</code>.
   *
   * @param course Course the student is to enroll in.
   */
  void addCourse(Course course);

  /**
   * Drops a <code>Course</code> for the <code>Student</code>.
   *
   * @param course Course the student is to drop
   */
  void removeCourse(Course course);

}
