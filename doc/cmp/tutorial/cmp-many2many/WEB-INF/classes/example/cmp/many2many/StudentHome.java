package example.cmp.many2many;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the Student bean.
 */
public interface StudentHome extends EJBLocalHome {

  /**
   * returns the <code>Student</code> entity that has <code>name</code>
   * as its primary key.
   *
   * @param name Primary Key of the student we want to find
   */
  Student findByPrimaryKey(String name)
    throws FinderException;

  /**
   * finds all courses for a student
   *
   * @param name Name of the student for whom we want to find all Courses
   */
  public Collection findByCourse(String name)
    throws FinderException;

}
