package example.cmp.ejbql;

import java.rmi.*;
import javax.ejb.*;
import java.util.*;

/**
 * Home interface for the Teacher bean.
 */
public interface TeacherHome extends EJBLocalHome {
  /**
   * returns the <code>Teacher</code> entity that has <code>name</code>
   * as its primary key.
   */
  Teacher findByPrimaryKey(String name)
    throws FinderException;

  /**
   * Finds the teachers teaching any classes with the named student.
   *
   * <code><pre>
   * SELECT course.teacher
   * FROM ejbql_student student, IN(student.courseList) course
   * WHERE student.name=?1
   * </pre></code>
   *
   * @param studentName the student used as a key
   * @return a collection of Teachers teaching courses to the student.
   */
  Collection findByStudent(String studentName)
    throws FinderException;
}
