package example.cmp.many2many;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

import java.util.*;

/**
 * Local interface for Course entity
 */
public interface Course extends EJBLocalObject {

  /**
   * Returns the name of the <code>Course</code> (CMP field).
   * This is also the primary key
   * as defined in the deployment descriptor.
   */
  String getName();

  /**
   * Returns the name of the instructor teaching the <code>Course</course>
   * (CMP field).
   */
  String getInstructor();

  /**
   * Returns a <code>Collection</code> of all students enrolled in the
   * <code>Course</course> (CMR field).
   */
  Collection getStudentList();

  /**
   * Adds a <code>Student</code> to the <code>Course</course>. This will update
   * the table many2many_student_course_mapping as defined in the
   * deployment descriptor.
   */
  void addStudent(Student student);

  /**
   * Removes a <code>Student</code> from the <code>Course</course>. This will
   * update the table many2many_student_course_mapping as defined in the
   * deployment descriptor.
   */
  void removeStudent(Student student);

}
