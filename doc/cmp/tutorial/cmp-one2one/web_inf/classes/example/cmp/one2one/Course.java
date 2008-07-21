package example.cmp.one2one;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;

import java.util.*;


/**
 * Local interface for the Course bean.
 */
public interface Course extends EJBLocalObject {

  /**
   * Returns the name of the <code>Course</code> (CMP field).
   * This is also the primary key as defined in the deployment descriptor.
   */
  String getName();

  /**
   * Returns the Teacher teaching this <code>Course</course> (CMR field).
   */
  Teacher getTeacher();
}
