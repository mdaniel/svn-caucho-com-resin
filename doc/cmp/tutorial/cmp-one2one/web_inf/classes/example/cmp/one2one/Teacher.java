package example.cmp.one2one;

import java.rmi.*;
import java.util.*;
import javax.ejb.*;


/**
 * Local interface for the Teacher bean.
 */
public interface Teacher extends EJBLocalObject {

  /**
   * returns the the name of the Teacher (CMP field).
   */
  String getName();

  /**
   * returns the Course associated with the Teacher (CMR field).
   */
  Course getCourse();

//  void setCourse(Course course);

}
