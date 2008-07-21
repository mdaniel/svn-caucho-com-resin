package example.cmp.many2many;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the Course bean.
 */
public interface CourseHome extends EJBLocalHome {

  /**
   * returns the <code>Course</code> that has <code>name</code>
   * as its primary key.
   *
   * @param name primary key of the Course we want to find.
   */
  Course findByPrimaryKey(String name)
    throws FinderException;

  /**
   * Returns all Courses that a student is enrolled in. Resin-CMP will implement
   * this method.
   *
   * @param name of the Student whose Courses we want to find.
   */
  Collection findByStudent(String name)
    throws FinderException;
}
