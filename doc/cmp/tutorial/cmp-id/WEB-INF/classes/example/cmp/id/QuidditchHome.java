package example.cmp.id;

import java.rmi.*;
import javax.ejb.*;

import java.util.*;

/**
 * Home interface for the Quidditch statistics.  The
 * Quidditch entry uses an identifying relationship to the Student
 * as its primary.
 */
public interface QuidditchHome extends EJBLocalHome {
  /**
   * Returns the <code>Quidditch</code> entity with the <code>Student</code>
   * as its primary key.
   */
  public Quidditch findByPrimaryKey(Student student)
    throws FinderException;
  /**
   * Create a Quidditch entry for a student.
   */
  public Quidditch create(Student student, String position)
    throws CreateException;
}
