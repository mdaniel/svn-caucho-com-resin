package example.cmp.ejbql;

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
   * Returns the Course that this Teacher is teaching (CMR field).
   */
  public Course getCourse();

  //public void setCourse();


}
