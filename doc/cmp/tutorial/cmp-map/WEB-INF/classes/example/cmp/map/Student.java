package example.cmp.map;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

/**
 * Local interface for the Student bean.
 */
public interface Student extends EJBLocalObject {

  /**
   * Returns the student's name.
   */
  String getName();

  /**
   * Returns the student's grades.  The grades are indexed by the
   * courses.
   */
  Map getGrades();
}
